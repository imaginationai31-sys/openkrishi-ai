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


class OpenAISpeechToText:
    """Speech-to-text provider backed by OpenAI's transcription API.

    The API key is read from the OPENAI_API_KEY environment variable and is
    never stored in the repository.
    """

    def __init__(self, model: str = "gpt-4o-mini-transcribe") -> None:
        self.model = model

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
        audio_file.name = "farmer_audio.wav"

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
