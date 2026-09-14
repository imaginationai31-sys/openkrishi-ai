"""Speech-to-text providers for OpenKrishi AI.

The default concrete provider uses Groq's Whisper-compatible transcription API.
The provider interface remains separate so OpenKrishi can add or replace
providers later.
"""

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
    "flac",
    "mp3",
    "mp4",
    "mpeg",
    "mpga",
    "m4a",
    "ogg",
    "opus",
    "wav",
    "webm",
}


def _audio_filename(
    audio: bytes,
    filename: str | None = None,
    content_type: str | None = None,
) -> str:
    """Return a Groq-supported filename extension for the upload.

    Prefer the extension supplied by the multipart upload, then use file
    signatures. This handles WhatsApp recordings whose bytes do not expose a
    recognizable container signature at the start of the payload.
    """
    if filename:
        extension = Path(filename).suffix.lower().lstrip(".")
        if extension in SUPPORTED_AUDIO_EXTENSIONS:
            return f"farmer_audio.{extension}"

    if content_type:
        mime_to_extension = {
            "audio/ogg": "ogg",
            "audio/opus": "opus",
            "audio/wav": "wav",
            "audio/x-wav": "wav",
            "audio/mpeg": "mp3",
            "audio/mp3": "mp3",
            "audio/mp4": "mp4",
            "audio/x-m4a": "m4a",
            "audio/webm": "webm",
            "audio/flac": "flac",
        }
        mime = content_type.split(";", 1)[0].strip().lower()
        extension = mime_to_extension.get(mime)
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


class GroqSpeechToText:
    """Speech-to-text provider backed by Groq's Whisper API.

    The API key is read from GROQ_API_KEY and is never stored in the repository.
    The model can be overridden with GROQ_STT_MODEL.
    """

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("GROQ_STT_MODEL", "whisper-large-v3-turbo")

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

        api_key = os.getenv("GROQ_API_KEY")
        if not api_key:
            raise RuntimeError(
                "GROQ_API_KEY is not configured. Set it before processing audio."
            )

        from groq import Groq

        client = Groq(api_key=api_key)
        audio_file = io.BytesIO(audio)
        audio_file.name = _audio_filename(audio, filename, content_type)

        result = client.audio.transcriptions.create(
            model=self.model,
            file=audio_file,
            language=language,
        )

        text = (result.text or "").strip()
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")

        return Transcription(text=text, language=language, confidence=None)


class OpenAISpeechToText:
    """Backward-compatible OpenAI STT provider.

    Kept available for callers that still explicitly instantiate this class.
    The OpenKrishi voice API uses GroqSpeechToText by default.
    """

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("OPENAI_STT_MODEL", "gpt-4o-mini-transcribe")

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

        api_key = os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError(
                "OPENAI_API_KEY is not configured. Set it before processing audio."
            )

        from openai import OpenAI

        client = OpenAI(api_key=api_key)
        audio_file = io.BytesIO(audio)
        audio_file.name = _audio_filename(audio, filename, content_type)
        result = client.audio.transcriptions.create(
            model=self.model,
            file=audio_file,
            language=language,
        )
        text = (result.text or "").strip()
        if not text:
            raise RuntimeError("Speech-to-text returned an empty transcription.")
        return Transcription(text=text, language=language, confidence=None)


class NotConfiguredSpeechToText:
    """Safe placeholder for deployments that do not configure an STT provider."""

    def transcribe(self, audio: bytes, language: str) -> Transcription:
        raise RuntimeError(
            "Speech-to-text provider is not configured. "
            "Configure an STT provider before processing audio."
        )
