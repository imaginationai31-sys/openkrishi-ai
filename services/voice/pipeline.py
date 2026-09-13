"""Voice-to-advisory-to-voice orchestration."""

from typing import Any

from services.advisory.engine import generate_advisory

from .languages import is_supported_language
from .speech_to_text import SpeechToTextProvider
from .text_to_speech import TextToSpeechProvider


def process_voice_query(
    audio: bytes,
    language: str,
    stt: SpeechToTextProvider,
    tts: TextToSpeechProvider,
    *,
    crop_category: str | None = None,
) -> dict[str, Any]:
    """Run STT -> advisory -> TTS for one farmer voice interaction."""
    if not is_supported_language(language):
        raise ValueError(f"Unsupported voice language: {language}")

    transcription = stt.transcribe(audio, language)
    advisory = generate_advisory(
        transcription.text,
        transcription.language,
        crop_category=crop_category,
    )
    spoken = tts.synthesize(advisory["answer"], language)

    return {
        "transcription": {
            "text": transcription.text,
            "language": transcription.language,
            "confidence": transcription.confidence,
        },
        "advisory": advisory,
        "audio": spoken,
    }
