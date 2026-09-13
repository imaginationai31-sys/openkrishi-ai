"""Text-to-speech provider interface."""

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class SpeechAudio:
    audio: bytes
    language: str
    mime_type: str = "audio/wav"


class TextToSpeechProvider(Protocol):
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        """Convert an advisory response into spoken audio."""


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError(
            "Text-to-speech provider is not configured. "
            "Configure a TTS provider before generating audio."
        )
