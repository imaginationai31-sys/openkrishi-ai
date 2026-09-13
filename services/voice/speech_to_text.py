"""Speech-to-text providers for OpenKrishi AI.

The default concrete provider uses the OpenAI transcription API. The provider
interface remains separate so OpenKrishi can add or replace providers later.
"""

from dataclasses import dataclass
import io
import os
from typing import Protocol

from .languages import is_supported_language


@dataclass(frozen=True)
class Transcription:
    text: str
    language: str
    confidence: float | None = None


class SpeechToTextProvider(Protocol):
    def transcribe(self, audio: bytes, language: str) -> Transcription:
        """Convert audio bytes into normalized farmer text."""


def _audio_filename(audio: bytes) -> str:
    """Return a useful filename extension for the OpenAI audio upload.

    WhatsApp voice notes commonly arrive as OGG/Opus. The upload filename must
    match the actual container instead of incorrectly labelling OGG bytes as
    WAV, otherwise the transcription API may reject or misinterpret the file.
    """
    if audio.startswith(b"OggS"):
        return "farmer_audio.ogg"
    if audio.startswith(b"RIFF") and audio[8:12] == b"WAVE":
        return "farmer_audio.wav"
    if audio.startswith(b"ID3") or audio[:2] in (b"\xff\xfb", b"\xff\xf3", b"\xff\xf2"):
        return "farmer_audio.mp3"
    if audio.startswith(b"\x1a\x45\xdf\xa3"):
        return "farmer_audio.webm"
    return "farmer_audio.bin"


class OpenAISpeechToText:
    """Speech-to-text provider backed by the OpenAI transcription API.

    The API key is read from the OPENAI_API_KEY environment variable and is
    never stored in the repository. The model can be overridden with the
    OPENAI_STT_MODEL environment variable for deployment configuration.
    """

    def __init__(self, model: str | None = None) -> None:
        self.model = model or os.getenv("OPENAI_STT_MODEL", "gpt-4o-mini-transcribe")

    def transcribe(self, audio: bytes, language: str) -> Transcription:
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
        audio_file.name = _audio_filename(audio)

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
