from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.voice.languages import is_supported_language
from services.voice.speech_to_text import OpenAISpeechToText

router = APIRouter()

MAX_AUDIO_BYTES = 10 * 1024 * 1024


@router.post("/voice/transcribe")
async def transcribe_voice(
    file: UploadFile = File(...),
    language: str = Form(...),
) -> dict[str, Any]:
    """Transcribe a farmer voice recording with the configured OpenAI STT provider."""
    if not is_supported_language(language):
        raise HTTPException(
            status_code=422,
            detail=f"Unsupported voice language: {language}",
        )

    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Audio input cannot be empty.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(
            status_code=413,
            detail="Audio file is too large. Maximum size is 10 MB.",
        )

    try:
        transcription = OpenAISpeechToText().transcribe(audio, language)
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
