import base64
from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.advisory.engine import generate_advisory
from services.advisory.localization import localize_advisory, localize_visual
from services.advisory.normalizer import normalize_agricultural_terms
from services.advisory.voice_understanding import build_voice_understanding
from services.voice.languages import is_supported_language
from services.voice.speech_to_text import OpenAISpeechToText
from services.voice.text_to_speech import GeminiTextToSpeech
from services.vision.engine import assess_crop_image

router = APIRouter()
MAX_AUDIO_BYTES = 10 * 1024 * 1024
SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}


def _transcribe(audio: bytes, language: str, filename: str | None, content_type: str | None):
    return OpenAISpeechToText().transcribe(audio, language, filename=filename, content_type=content_type)


def _spoken_advisory(advisory: dict[str, Any], visual: dict[str, Any] | None = None) -> str:
    labels = {
        "en": ("Photo observations", "Possible causes", "Safe solutions and next steps", "Important", "No advisory information is available yet."),
        "bn": ("ছবিতে দেখা লক্ষণ", "সম্ভাব্য কারণ", "নিরাপদ সমাধান ও পরবর্তী পদক্ষেপ", "গুরুত্বপূর্ণ", "এখনও কোনো পরামর্শের তথ্য পাওয়া যায়নি।"),
        "hi": ("फोटो में दिखाई देने वाले लक्षण", "संभावित कारण", "सुरक्षित समाधान और अगले कदम", "महत्वपूर्ण", "अभी कोई सलाह उपलब्ध नहीं है।"),
        "ta": ("படத்தில் காணப்படும் அறிகுறிகள்", "சாத்தியமான காரணங்கள்", "பாதுகாப்பான தீர்வுகள் மற்றும் அடுத்தடுத்த நடவடிக்கைகள்", "முக்கியம்", "இப்போது எந்த ஆலோசனைத் தகவலும் இல்லை."),
        "pa": ("ਤਸਵੀਰ ਵਿੱਚ ਦਿਖਾਈ ਦੇਣ ਵਾਲੇ ਲੱਛਣ", "ਸੰਭਾਵੀ ਕਾਰਨ", "ਸੁਰੱਖਿਅਤ ਹੱਲ ਅਤੇ ਅਗਲੇ ਕਦਮ", "ਮਹੱਤਵਪੂਰਨ", "ਹਾਲੇ ਕੋਈ ਸਲਾਹ ਉਪਲਬਧ ਨਹੀਂ ਹੈ।"),
        "te": ("చిత్రంలో కనిపించే లక్షణాలు", "సంభావ్య కారణాలు", "సురక్షిత పరిష్కారాలు మరియు తదుపరి చర్యలు", "ముఖ్యమైన విషయం", "ప్రస్తుతం ఎలాంటి సలహా సమాచారం అందుబాటులో లేదు."),
    }
    language = str(advisory.get("language") or "en")
    photo_label, causes_label, solution_label, important_label, empty_text = labels.get(language, labels["en"])
    parts: list[str] = []
    answer = str(advisory.get("answer") or "").strip()
    if answer:
        parts.append(answer)
    if visual:
        observations = [str(x).strip() for x in visual.get("observations", []) if str(x).strip()]
        causes = [str(x).strip() for x in visual.get("possible_causes", []) if str(x).strip()]
        if observations:
            parts.append(photo_label + ": " + " ".join(observations))
        if causes:
            parts.append(causes_label + ": " + "; ".join(causes))
    else:
        observations = [str(x).strip() for x in advisory.get("observations", []) if str(x).strip()]
        if observations:
            parts.append(causes_label + ": " + " ".join(observations))
    recommendations = [str(x).strip() for x in advisory.get("recommendations", []) if str(x).strip()]
    if recommendations:
        parts.append(solution_label + ": " + " ".join(recommendations))
    uncertainties = [str(x).strip() for x in advisory.get("uncertainties", []) if str(x).strip()]
    if uncertainties:
        parts.append(important_label + ": " + " ".join(uncertainties[:2]))
    return " ".join(parts).strip() or empty_text


def _synthesize(text: str, language: str):
    return GeminiTextToSpeech().synthesize(text, language)


@router.post("/voice/transcribe")
async def transcribe_voice(file: UploadFile = File(...), language: str = Form(...)) -> dict[str, Any]:
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
        normalized_text, matched_terms = normalize_agricultural_terms(transcription.text, transcription.language)
        understanding = build_voice_understanding(transcription.text, normalized_text, matched_terms, transcription.language, crop_category)
        advisory = generate_advisory(query=normalized_text, language=transcription.language, crop_category=crop_category, growth_stage=growth_stage, location=location)
        if understanding["needs_clarification"]:
            advisory["confidence"] = "low"
            advisory["uncertainties"].insert(0, "The farmer's wording could not be mapped confidently to a known agricultural symptom.")
            advisory["recommendations"].extend(understanding["follow_up_questions"])
        advisory = localize_advisory(advisory, transcription.language)
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
        visual = assess_crop_image(image_bytes=image_bytes, content_type=image.content_type or "", crop_category=crop_category, growth_stage=growth_stage, language=transcription.language)
        visual = localize_visual(visual, transcription.language)
        visual_query = "; ".join([*visual.get("observations", []), *visual.get("possible_causes", [])]).strip()
        combined_query = "; ".join(part for part in (normalized_text, visual_query) if part).strip() or "farmer crop concern is unclear; crop photo assessment is unclear"
        advisory = generate_advisory(query=combined_query, language=transcription.language, crop_category=crop_category, growth_stage=growth_stage, location=location)
        if understanding["needs_clarification"]:
            advisory["confidence"] = "low"
            advisory["uncertainties"].insert(0, "The farmer's wording could not be mapped confidently to a known agricultural symptom.")
            advisory["recommendations"].extend(understanding["follow_up_questions"])
        if visual.get("confidence") == "low":
            advisory["confidence"] = "low"
            advisory["uncertainties"].insert(0, "The crop photo did not provide enough reliable visual evidence for a confident conclusion.")
        advisory = localize_advisory(advisory, transcription.language)
        spoken = _synthesize(_spoken_advisory(advisory, visual), transcription.language)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {
        "transcription": {"text": transcription.text, "normalized_text": normalized_text, "matched_terms": matched_terms, "language": transcription.language, "confidence": transcription.confidence},
        "understanding": understanding,
        "vision": {"language": transcription.language, "status": visual["status"], "observations": visual.get("observations", []), "possible_causes": visual.get("possible_causes", []), "confidence": visual["confidence"], "safety": visual["safety"], "uncertainties": visual["uncertainties"], "recommendations": visual.get("recommendations", [])},
        "advisory": advisory,
        "audio": {"mime_type": spoken.mime_type, "language": spoken.language, "base64": base64.b64encode(spoken.audio).decode("ascii")},
    }
