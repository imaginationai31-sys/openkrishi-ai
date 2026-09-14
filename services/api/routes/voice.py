from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.advisory.engine import generate_advisory
from services.voice.languages import is_supported_language
from services.voice.speech_to_text import GroqSpeechToText

router = APIRouter()

MAX_AUDIO_BYTES = 10 * 1024 * 1024


@router.post("/voice/transcribe")
async def transcribe_voice(
    file: UploadFile = File(...),
    language: str = Form(...),
) -> dict[str, Any]:
    """Transcribe a farmer voice recording with Groq Whisper."""
    if not is_supported_language(language):
        raise HTTPException(status_code=422, detail=f"Unsupported voice language: {language}")

    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large. Maximum size is 10 MB.")

    try:
        transcription = GroqSpeechToText().transcribe(
            audio,
            language,
            filename=file.filename,
            content_type=file.content_type,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return {
        "text": transcription.text,
        "language": transcription.language,
        "confidence": transcription.confidence,
        "filename": file.filename,
        "content_type": file.content_type,
    }


@router.post("/voice/advisory")
async def voice_advisory(
    file: UploadFile = File(...),
    language: str = Form(...),
    crop_category: str | None = Form(default=None),
    growth_stage: str | None = Form(default=None),
    location: str | None = Form(default=None),
) -> dict[str, Any]:
    """Transcribe a farmer voice recording and pass the text to the advisory engine."""
    if not is_supported_language(language):
        raise HTTPException(status_code=422, detail=f"Unsupported voice language: {language}")

    if crop_category is not None and crop_category not in {"rice", "peanut", "vegetables", "flowers"}:
        raise HTTPException(status_code=422, detail=f"Unsupported crop category: {crop_category}")

    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio file is too large. Maximum size is 10 MB.")

    try:
        transcription = GroqSpeechToText().transcribe(
            audio,
            language,
            filename=file.filename,
            content_type=file.content_type,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    advisory = generate_advisory(
        query=transcription.text,
        language=transcription.language,
        crop_category=crop_category,
        growth_stage=growth_stage,
        location=location,
    )

    return {
        "transcription": {
            "text": transcription.text,
            "language": transcription.language,
            "confidence": transcription.confidence,
        },
        "advisory": advisory,
    }
