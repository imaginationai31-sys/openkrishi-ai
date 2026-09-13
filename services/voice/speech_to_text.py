"""Speech-to-text provider interface.

Providers should implement this interface without coupling the advisory
engine to a specific commercial or open-source speech service.
"""

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class Transcription:
    text: str
    language: str
    confidence: float | None = None


class SpeechToTextProvider(Protocol):
    def transcribe(self, audio: bytes, language: str) -> Transcription:
        """Convert audio bytes into normalized farmer text."""


class NotConfiguredSpeechToText:
    """Safe placeholder until a concrete STT provider is configured."""

    def transcribe(self, audio: bytes, language: str) -> Transcription:
        raise RuntimeError(
            "Speech-to-text provider is not configured. "
            "Configure an STT provider before processing audio."
        )
