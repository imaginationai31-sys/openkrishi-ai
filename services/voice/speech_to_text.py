"""Speech-to-text providers for OpenKrishi AI."""

from dataclasses import dataclass
import io
import os
from pathlib import Path
from typing import Protocol

from .languages import is_supported_language


@dataclass(frozen=True)
class Transcription:
    text: str
    language: str
    confidence: float | None = None


class SpeechToTextProvider(Protocol):
    def transcribe(
        self,
        audio: bytes,
        language: str,
        filename: str | None = None,
        content_type: str | None = None,
    ) -> Transcription:
        """Convert audio bytes into normalized farmer text."""


SUPPORTED_AUDIO_EXTENSIONS = {
    "flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "opus", "wav", "webm"
}

LANGUAGE_CODES = {
    "bn": "bn-IN",
    "hi": "hi-IN",
    "ta": "ta-IN",
    "pa": "pa-IN",
    "te": "te-IN",
}

AGRICULTURAL_VOCABULARY = [
    "rice", "paddy", "ধান", "dhan", "peanut", "groundnut", "বাদাম",
    "yellow leaves", "wilting", "leaf spots", "পোকা", "রোগ", "পাতা",
]


def _audio_filename(audio: bytes, filename: str | None = None, content_type: str | None = None) -> str:
    """Return a supported filename extension for the Gemini upload."""
    if filename:
        extension = Path(filename).suffix.lower().lstrip(".")
        if extension in SUPPORTED_AUDIO_EXTENSIONS:
            return f"farmer_audio.{extension}"

    if content_type:
        mime_to_extension = {
            "audio/ogg": "ogg", "application/ogg": "ogg", "audio/opus": "opus",
            "audio/wav": "wav", "audio/x-wav": "wav", "audio/mpeg": "mp3",
            "audio/mp3": "mp3", "audio/mp4": "mp4", "audio/x-m4a": "m4a",
            "audio/webm": "webm", "audio/flac": "flac",
        }
        extension = mime_to_extension.get(content_type.split(";", 1)[0].strip().lower())
        if extension:
            return f"farmer_audio.{extension}"

    if audio.startswith(b"OggS"):
        return "farmer_audio.ogg"
    if audio.startswith(b"RIFF") and audio[8:12] == b"WAVE":
        return "farmer_audio.wav"
    if audio.startswith(b"ID3") or audio[:2] in (b"\xff\xfb", b"\xff\xf3", b"\xff\xf2"):
        return "farmer_audio.mp3"
    if audio.startswith(b"\x1a\x45\xdf\xa3"):
        return "farmer_audio.webm"
    return "farmer_audio.ogg"


class GeminiSpeechToText:
    """Speech-to-text provider backed by Gemini 3.5 Transcribe."""

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("GEMINI_TRANSCRIBE_MODEL", "gemini-3.5-transcribe")

    def transcribe(
        self,
        audio: bytes,
        language: str,
        filename: str | None = None,
        content_type: str | None = None,
    ) -> Transcription:
        if not audio:
            raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language):
            raise ValueError(f"Unsupported voice language: {language}")
        if language not in LANGUAGE_CODES:
            raise ValueError(f"Unsupported Gemini voice language: {language}")

        from services.gemini.client import get_gemini_client, output_text

        client = get_gemini_client()
        upload_name = _audio_filename(audio, filename, content_type)
        mime_type = content_type or "audio/ogg"
        mime_type = mime_type.split(";", 1)[0].strip().lower()
        if mime_type == "audio/x-wav":
            mime_type = "audio/wav"

        try:
            audio_file = client.files.upload(
                file=io.BytesIO(audio),
                config={"display_name": upload_name, "mime_type": mime_type},
            )
            interaction = client.interactions.create(
                model=self.model,
                input=[
                    {
                        "type": "audio",
                        "uri": audio_file.uri,
                        "mime_type": audio_file.mime_type or mime_type,
                    }
                ],
                generation_config={
                    "transcription_config": {
                        "language_codes": [LANGUAGE_CODES[language]],
                        "custom_vocabulary": AGRICULTURAL_VOCABULARY,
                        "mode": "smart",
                    }
                },
            )
        except Exception as exc:
            raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc

        text = output_text(interaction)
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class GroqSpeechToText:
    """Backward-compatible Groq Whisper provider."""

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("GROQ_STT_MODEL", "whisper-large-v3-turbo")

    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        if not audio:
            raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language):
            raise ValueError(f"Unsupported voice language: {language}")
        api_key = os.getenv("GROQ_API_KEY")
        if not api_key:
            raise RuntimeError("GROQ_API_KEY is not configured. Set it before processing audio.")
        from groq import Groq
        client = Groq(api_key=api_key)
        upload_name = _audio_filename(audio, filename, content_type)
        try:
            result = client.audio.transcriptions.create(
                model=self.model, file=(upload_name, audio), language=language, response_format="json"
            )
        except Exception as exc:
            raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc
        text = (result.text or "").strip()
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class OpenAISpeechToText:
    """Backward-compatible OpenAI STT provider."""

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("OPENAI_STT_MODEL", "gpt-4o-mini-transcribe")

    def transcribe(self, audio: bytes, language: str, filename: str | None = None, content_type: str | None = None) -> Transcription:
        if not audio:
            raise ValueError("Audio input cannot be empty.")
        if not is_supported_language(language):
            raise ValueError(f"Unsupported voice language: {language}")
        api_key = os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError("OPENAI_API_KEY is not configured. Set it before processing audio.")
        from openai import OpenAI
        client = OpenAI(api_key=api_key)
        upload_name = _audio_filename(audio, filename, content_type)
        try:
            result = client.audio.transcriptions.create(model=self.model, file=(upload_name, audio), language=language)
        except Exception as exc:
            raise RuntimeError("Speech-to-text provider is temporarily unavailable.") from exc
        text = (result.text or "").strip()
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class NotConfiguredSpeechToText:
    """Safe placeholder for deployments that do not configure an STT provider."""

    def transcribe(self, audio: bytes, language: str) -> Transcription:
        raise RuntimeError("Speech-to-text provider is not configured. Configure an STT provider before processing audio.")
