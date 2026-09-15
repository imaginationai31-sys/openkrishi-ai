"""Text-to-speech providers for OpenKrishi AI."""

from dataclasses import dataclass
import os
from typing import Protocol

import httpx


@dataclass(frozen=True)
class SpeechAudio:
    audio: bytes
    language: str
    mime_type: str = "audio/wav"


class TextToSpeechProvider(Protocol):
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        """Convert an advisory response into spoken audio."""


TTSFREE_LANGUAGE_CODES = {
    "bn": "Bengali",
    "hi": "Hindi",
    "ta": "Tamil",
    "pa": "Punjabi",
    "te": "Telugu",
}

TTSFREE_SPEAKERS = {
    "bn": "Ananya",
    "hi": "Divya",
    "ta": "Kavitha",
    "pa": "Divjot",
    "te": "Priya_tel",
}


class TTSFreeTextToSpeech:
    """Indian-language TTS backed by the TTSFree developer API."""

    def __init__(self, speaker: str | None = None) -> None:
        self.base_url = os.getenv("TTSFREE_BASE_URL", "https://ttsfree.in")
        self.speaker = speaker

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        if not text.strip():
            raise ValueError("Text input cannot be empty.")

        language_name = TTSFREE_LANGUAGE_CODES.get(language)
        if not language_name:
            raise ValueError(f"Unsupported TTS language: {language}")

        api_key = os.getenv("TTSFREE_API_KEY")
        if not api_key:
            raise RuntimeError(
                "TTSFREE_API_KEY is not configured. Set it before generating audio."
            )

        speaker = self.speaker or TTSFREE_SPEAKERS[language]
        response = httpx.post(
            f"{self.base_url.rstrip('/')}/api/tts",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json={
                "text": text,
                "language": language_name,
                "speaker": speaker,
                "emotion": "Neutral",
            },
            timeout=60.0,
        )
        response.raise_for_status()

        audio = response.content
        if not audio:
            raise RuntimeError("TTSFree returned empty audio.")

        return SpeechAudio(audio=audio, language=language, mime_type="audio/wav")


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError(
            "Text-to-speech provider is not configured. "
            "Configure a TTS provider before generating audio."
        )
