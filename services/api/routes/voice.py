import base64
from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.advisory.engine import generate_advisory
from services.advisory.normalizer import normalize_agricultural_terms
from services.advisory.voice_understanding import build_voice_understanding
from services.voice.languages import is_supported_language
from services.voice.speech_to_text import GeminiSpeechToText
from services.voice.text_to_speech import GeminiTextToSpeech
from services.vision.engine import assess_crop_image

router = APIRouter()

MAX_AUDIO_BYTES = 10 * 1024 * 1024
SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}


def _transcribe(audio: bytes, language: str, filename: str | None, content_type: str | None):
    return GeminiSpeechToText().transcribe(
        audio, language, filename=filename, content_type=content_type
    )


def _spoken_advisory(advisory: dict[str, Any], visual: dict[str, Any] | None = None) -> str:
    """Build the complete farmer-friendly text that Gemini TTS should speak."""
    parts: list[str] = []
    answer = str(advisory.get("answer") or "").strip()
    if answer:
        parts.append(answer)

    if visual:
        observations = [str(x).strip() for x in visual.get("observations", []) if str(x).strip()]
        causes = [str(x).strip() for x in visual.get("possible_causes", []) if str(x).strip()]
        if observations:
            parts.append("Photo observations: " + " ".join(observations))
        if causes:
            parts.append("Possible causes: " + "; ".join(causes))
    else:
        observations = [str(x).strip() for x in advisory.get("observations", []) if str(x).strip()]
        if observations:
            parts.append("Possible causes: " + " ".join(observations))

    recommendations = [str(x).strip() for x in advisory.get("recommendations", []) if str(x).strip()]
    if recommendations:
        parts.append("Safe solutions and next steps: " + " ".join(recommendations))

    uncertainties = [str(x).strip() for x in advisory.get("uncertainties", []) if str(x).strip()]
    if uncertainties:
        parts.append("Important: " + " ".join(uncertainties[:2]))

    return " ".join(parts).strip() or "No advisory information is available yet."


def _synthesize(text: str, language: str):
    return GeminiTextToSpeech().synthesize(text, language)


@router.post("/voice/transcribe")
async def transcribe_voice(file: UploadFile = File(...), language: str = Form(...)) -> dict[str, Any]:
    """Transcribe a farmer voice recording with Gemini 3.5 Transcribe."""
    if not is_supported_language(language):
        raise HTTPException(status_code=422, detail=f"Unsupported voice language: {language}")
    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large. Maximum size is 10 MB.")
    try:
        transcription = _transcribe(audio, language, file.filename, file.content_type)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {"text": transcription.text, "language": transcription.language, "confidence": transcription.confidence, "filename": file.filename, "content_type": file.content_type}


@router.post("/voice/advisory")
async def voice_advisory(file: UploadFile = File(...), language: str = Form(...), crop_category: str | None = Form(default=None), growth_stage: str | None = Form(default=None), location: str | None = Form(default=None)) -> dict[str, Any]:
    """Run farmer voice -> STT -> understanding -> advisory -> TTS, including causes and solutions in speech."""
    if not is_supported_language(language):
        raise HTTPException(status_code=422, detail=f"Unsupported voice language: {language}")
    if crop_category is not None and crop_category not in SUPPORTED_CROPS:
        raise HTTPException(status_code=422, detail=f"Unsupported crop category: {crop_category}")
    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large. Maximum size is 10 MB.")
    try:
        transcription = _transcribe(audio, language, file.filename, file.content_type)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    normalized_text, matched_terms = normalize_agricultural_terms(transcription.text, transcription.language)
    understanding = build_voice_understanding(transcription.text, normalized_text, matched_terms, transcription.language, crop_category)
    advisory = generate_advisory(query=normalized_text, language=transcription.language, crop_category=crop_category, growth_stage=growth_stage, location=location)
    if understanding["needs_clarification"]:
        advisory["confidence"] = "low"
        advisory["uncertainties"].insert(0, "The farmer's wording could not be mapped confidently to a known agricultural symptom.")
        advisory["recommendations"].extend(understanding["follow_up_questions"])
    try:
        spoken = _synthesize(_spoken_advisory(advisory), transcription.language)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {
        "transcription": {"text": transcription.text, "normalized_text": normalized_text, "matched_terms": matched_terms, "language": transcription.language, "confidence": transcription.confidence},
        "understanding": understanding,
        "advisory": advisory,
        "audio": {"mime_type": spoken.mime_type, "language": spoken.language, "base64": base64.b64encode(spoken.audio).decode("ascii")},
    }


@router.post("/voice/vision-advisory")
async def voice_vision_advisory(file: UploadFile = File(...), image: UploadFile = File(...), language: str = Form(...), crop_category: str | None = Form(default=None), growth_stage: str | None = Form(default=None), location: str | None = Form(default=None)) -> dict[str, Any]:
    """Run farmer voice + crop photo -> STT -> Vision -> advisory -> TTS, including visual causes and safe solutions in speech."""
    if not is_supported_language(language):
        raise HTTPException(status_code=422, detail=f"Unsupported voice language: {language}")
    if crop_category is not None and crop_category not in SUPPORTED_CROPS:
        raise HTTPException(status_code=422, detail=f"Unsupported crop category: {crop_category}")
    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large. Maximum size is 10 MB.")
    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="Image input cannot be empty.")

    try:
        transcription = _transcribe(audio, language, file.filename, file.content_type)
        normalized_text, matched_terms = normalize_agricultural_terms(transcription.text, transcription.language)
        understanding = build_voice_understanding(transcription.text, normalized_text, matched_terms, transcription.language, crop_category)
        visual = assess_crop_image(image_bytes=image_bytes, content_type=image.content_type or "", crop_category=crop_category, growth_stage=growth_stage)
        visual_query = "; ".join([*visual.get("observations", []), *visual.get("possible_causes", [])]).strip()
        combined_query = "; ".join(part for part in (normalized_text, visual_query) if part).strip()
        if not combined_query:
            combined_query = "farmer crop concern is unclear; crop photo assessment is unclear"
        advisory = generate_advisory(query=combined_query, language=transcription.language, crop_category=crop_category, growth_stage=growth_stage, location=location)
        if understanding["needs_clarification"]:
            advisory["confidence"] = "low"
            advisory["uncertainties"].insert(0, "The farmer's wording could not be mapped confidently to a known agricultural symptom.")
            advisory["recommendations"].extend(understanding["follow_up_questions"])
        if visual.get("confidence") == "low":
            advisory["confidence"] = "low"
            advisory["uncertainties"].insert(0, "The crop photo did not provide enough reliable visual evidence for a confident conclusion.")
        spoken = _synthesize(_spoken_advisory(advisory, visual), transcription.language)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return {
        "transcription": {"text": transcription.text, "normalized_text": normalized_text, "matched_terms": matched_terms, "language": transcription.language, "confidence": transcription.confidence},
        "understanding": understanding,
        "vision": {"status": visual["status"], "observations": visual.get("observations", []), "possible_causes": visual.get("possible_causes", []), "confidence": visual["confidence"], "safety": visual["safety"], "uncertainties": visual["uncertainties"]},
        "advisory": advisory,
        "audio": {"mime_type": spoken.mime_type, "language": spoken.language, "base64": base64.b64encode(spoken.audio).decode("ascii")},
    }
