"""Text-to-speech providers for OpenKrishi AI."""

from dataclasses import dataclass
import base64
import os
from typing import Protocol


@dataclass(frozen=True)
class SpeechAudio:
    audio: bytes
    language: str
    mime_type: str = "audio/wav"


class TextToSpeechProvider(Protocol):
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        """Convert an advisory response into spoken audio."""


SARVAM_LANGUAGE_CODES = {
    "bn": "bn-IN",
    "hi": "hi-IN",
    "ta": "ta-IN",
    "pa": "pa-IN",
    "te": "te-IN",
}


class SarvamTextToSpeech:
    """Regional-language TTS backed by Sarvam Bulbul v3."""

    def __init__(self, model: str | None = None, speaker: str | None = None) -> None:
        self.model = model or os.getenv("SARVAM_TTS_MODEL", "bulbul:v3")
        self.speaker = speaker or os.getenv("SARVAM_TTS_SPEAKER", "Shubh")

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        if not text.strip():
            raise ValueError("Text input cannot be empty.")

        language_code = SARVAM_LANGUAGE_CODES.get(language)
        if not language_code:
            raise ValueError(f"Unsupported TTS language: {language}")

        api_key = os.getenv("SARVAM_API_KEY")
        if not api_key:
            raise RuntimeError(
                "SARVAM_API_KEY is not configured. Set it before generating audio."
            )

        from sarvamai import SarvamAI

        client = SarvamAI(api_subscription_key=api_key)
        response = client.text_to_speech.convert(
            text=text,
            target_language_code=language_code,
            model=self.model,
            speaker=self.speaker,
        )

        if not response.audios:
            raise RuntimeError("Sarvam TTS returned no audio.")

        audio = base64.b64decode(response.audios[0])
        if not audio:
            raise RuntimeError("Sarvam TTS returned empty audio.")

        return SpeechAudio(audio=audio, language=language, mime_type="audio/wav")


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError(
            "Text-to-speech provider is not configured. "
            "Configure a TTS provider before generating audio."
        )
