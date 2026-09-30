"""Speech-to-text providers for OpenKrishi AI."""

from dataclasses import dataclass
import logging
from pathlib import Path
import tempfile
import time
import httpx
from typing import Protocol

from .languages import is_supported_language
from services.core.config import get_settings

logger = logging.getLogger(__name__)

@dataclass(frozen=True)
class Transcription:
    text: str
    language: str
    confidence: float | None = None

class SpeechToTextProvider(Protocol):
    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription: ...

SUPPORTED_AUDIO_EXTENSIONS = {"aac", "flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "opus", "wav", "webm"}
SARVAM_STT_URL = get_settings().sarvam_stt_url
LANGUAGE_CODES = {"en": "en-IN", "bn": "bn-IN", "hi": "hi-IN", "ta": "ta-IN", "pa": "pa-IN", "te": "te-IN"}
LANGUAGE_NAMES = {"bn": "Bengali", "hi": "Hindi", "ta": "Tamil", "pa": "Punjabi", "te": "Telugu"}


def _audio_filename(audio: bytes, filename: str | None = None, content_type: str | None = None) -> str:
    if filename:
        extension = Path(filename).suffix.lower().lstrip(".")
        if extension in SUPPORTED_AUDIO_EXTENSIONS:
            return f"farmer_audio.{extension}"
    if content_type:
        mime_to_extension = {"audio/aac":"aac","audio/ogg":"ogg","application/ogg":"ogg","audio/opus":"opus","audio/wav":"wav","audio/x-wav":"wav","audio/mpeg":"mp3","audio/mp3":"mp3","audio/mp4":"mp4","audio/x-m4a":"m4a","audio/m4a":"m4a","audio/webm":"webm","audio/flac":"flac"}
        extension = mime_to_extension.get(content_type.split(";", 1)[0].strip().lower())
        if extension:
            return f"farmer_audio.{extension}"
    if audio.startswith(b"OggS"): return "farmer_audio.ogg"
    if audio.startswith(b"RIFF") and audio[8:12] == b"WAVE": return "farmer_audio.wav"
    if audio.startswith(b"ID3") or audio[:2] in (b"\xff\xfb", b"\xff\xf3", b"\xff\xf2"): return "farmer_audio.mp3"
    if audio.startswith(b"\x1a\x45\xdf\xa3"): return "farmer_audio.webm"
    return "farmer_audio.ogg"


class SarvamSpeechToText:
    """Sarvam Saaras speech recognition for OpenKrishi AI."""
    def __init__(self, model: str | None = None) -> None:
        self.model = model or get_settings().sarvam_stt_model

    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        if not audio:
            raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language):
            raise ValueError(f"Unsupported voice language: {language}")
        api_key = get_settings().sarvam_api_key.get_secret_value()
        language_code = LANGUAGE_CODES.get(language)
        if not language_code:
            raise ValueError(f"Unsupported Sarvam voice language: {language}")
        upload_name = _audio_filename(audio, filename, content_type)
        mime_type = (content_type or "audio/ogg").split(";", 1)[0].strip().lower()
        headers = {"api-subscription-key": api_key}
        data = {"model": self.model, "language_code": language_code, "mode": "transcribe"}
        try:
            with httpx.Client(timeout=get_settings().outbound_timeout_seconds) as client:
                response = client.post(SARVAM_STT_URL, headers=headers, data=data, files={"file": (upload_name, audio, mime_type)})
                response.raise_for_status()
                payload = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            logger.warning("Sarvam STT request failed: %s", exc)
            raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc
        text = str(payload.get("transcript") or "").strip()
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)

class GeminiSpeechToText:
    """Compatibility wrapper; Sarvam is now the active STT provider."""
    def __init__(self, model: str | None = None) -> None:
        self._provider = SarvamSpeechToText()
    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        return self._provider.transcribe(audio, language, filename, content_type)

class GroqSpeechToText:
    def __init__(self, model: str | None = None) -> None: self.model = model or get_settings().groq_stt_model
    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        if not audio: raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language): raise ValueError(f"Unsupported voice language: {language}")
        api_key = get_settings().groq_api_key.get_secret_value() if get_settings().groq_api_key else None
        if not api_key: raise RuntimeError("GROQ_API_KEY is not configured. Set it before processing audio.")
        from groq import Groq
        client = Groq(api_key=api_key); upload_name = _audio_filename(audio, filename, content_type)
        try: result = client.audio.transcriptions.create(model=self.model, file=(upload_name, audio), language=language, response_format="json")
        except Exception as exc: raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc
        text = (result.text or "").strip()
        if not text: raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class OpenAISpeechToText:
    def __init__(self, model: str | None = None) -> None: self.model = model or get_settings().openai_stt_model
    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        if not audio: raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language): raise ValueError(f"Unsupported voice language: {language}")
        api_key = get_settings().openai_api_key.get_secret_value()
        from openai import OpenAI
        client = OpenAI(api_key=api_key); upload_name = _audio_filename(audio, filename, content_type)
        try: result = client.audio.transcriptions.create(model=self.model, file=(upload_name, audio), language=language)
        except Exception as exc: raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc
        text = (result.text or "").strip()
        if not text: raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class NotConfiguredSpeechToText:
    def transcribe(self, audio: bytes, language: str) -> Transcription:
        raise RuntimeError("Speech-to-text provider is not configured. Configure an STT provider before processing audio.")
