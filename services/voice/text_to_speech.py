"""Text-to-speech providers for OpenKrishi AI.

The default concrete provider uses TTSFree's free REST API backed by
AI4Bharat's Indic Parler-TTS voices.
"""

from dataclasses import dataclass
import json
import os
from typing import Protocol
from urllib import error, request


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

TTSFREE_DEFAULT_SPEAKERS = {
    "bn": "Aditi",
    "hi": "Divya",
    "ta": "Kavitha",
    "pa": "Divjot",
    "te": "Priya_tel",
}


class TTSFreeTextToSpeech:
    """Regional-language TTS backed by TTSFree's free REST API."""

    def __init__(self, speaker: str | None = None) -> None:
        self.speaker = speaker
        self.endpoint = os.getenv("TTSFREE_TTS_URL", "https://ttsfree.in/api/tts")

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

        speaker = self.speaker or os.getenv(
            "TTSFREE_TTS_SPEAKER", TTSFREE_DEFAULT_SPEAKERS[language]
        )
        payload = json.dumps(
            {
                "text": text,
                "language": language_name,
                "speaker": speaker,
                "emotion": os.getenv("TTSFREE_TTS_EMOTION", "Neutral"),
                "pitch": 1.0,
                "rate": 1.0,
            }
        ).encode("utf-8")

        http_request = request.Request(
            self.endpoint,
            data=payload,
            method="POST",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
        )

        try:
            with request.urlopen(http_request, timeout=30) as response:
                audio = response.read()
                mime_type = response.headers.get_content_type() or "audio/wav"
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"TTSFree TTS request failed ({exc.code}): {detail}") from exc
        except error.URLError as exc:
            raise RuntimeError(f"TTSFree TTS request failed: {exc.reason}") from exc

        if not audio:
            raise RuntimeError("TTSFree TTS returned empty audio.")

        return SpeechAudio(audio=audio, language=language, mime_type=mime_type)


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError(
            "Text-to-speech provider is not configured. "
            "Configure a TTS provider before generating audio."
        )
