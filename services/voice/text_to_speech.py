"""Text-to-speech providers for OpenKrishi AI."""

from dataclasses import dataclass
import base64
import os
import httpx
from typing import Protocol


@dataclass(frozen=True)
class SpeechAudio:
    audio: bytes
    language: str
    mime_type: str = "audio/wav"


class TextToSpeechProvider(Protocol):
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        """Convert an advisory response into spoken audio."""


SARVAM_TTS_URL = "https://api.sarvam.ai/text-to-speech"
SARVAM_LANGUAGE_CODES = {"en": "en-IN", "bn": "bn-IN", "hi": "hi-IN", "ta": "ta-IN", "pa": "pa-IN", "te": "te-IN"}


def _pcm_to_wav(pcm: bytes, sample_rate: int = 24000, channels: int = 1, sample_width: int = 2) -> bytes:
    """Wrap Gemini PCM output in a WAV container."""
    import io
    import wave

    output = io.BytesIO()
    with wave.open(output, "wb") as wav:
        wav.setnchannels(channels)
        wav.setsampwidth(sample_width)
        wav.setframerate(sample_rate)
        wav.writeframes(pcm)
    return output.getvalue()


class SarvamTextToSpeech:
    """Sarvam Bulbul v3 TTS for OpenKrishi AI."""
    def __init__(self, model: str | None = None, voice: str | None = None) -> None:
        self.model = model or os.getenv("SARVAM_TTS_MODEL", "bulbul:v3")
        self.voice = voice

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        if not text.strip():
            raise ValueError("Text input cannot be empty.")
        api_key = os.getenv("SARVAM_API_KEY")
        if not api_key:
            raise RuntimeError("SARVAM_API_KEY is not configured. Set it before generating audio.")
        language_code = SARVAM_LANGUAGE_CODES.get(language)
        if not language_code:
            raise ValueError(f"Unsupported TTS language: {language}")
        payload = {"text": text[:2500], "target_language_code": language_code, "model": self.model, "speaker": self.voice or "shubh"}
        headers = {"api-subscription-key": api_key, "Content-Type": "application/json"}
        try:
            with httpx.Client(timeout=45.0) as client:
                response = client.post(SARVAM_TTS_URL, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            raise RuntimeError("Text-to-speech provider is temporarily unavailable.") from exc
        audios = data.get("audios") or []
        if not audios:
            raise RuntimeError("Sarvam TTS returned empty audio.")
        try:
            audio = base64.b64decode(audios[0])
        except Exception as exc:
            raise RuntimeError("Sarvam TTS returned invalid audio data.") from exc
        if not audio:
            raise RuntimeError("Sarvam TTS returned empty audio.")
        return SpeechAudio(audio=audio, language=language, mime_type="audio/wav")

class GeminiTextToSpeech:
    """Compatibility wrapper; Sarvam is now the active TTS provider."""
    def __init__(self, model: str | None = None, voice: str | None = None) -> None:
        self._provider = SarvamTextToSpeech()
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        return self._provider.synthesize(text, language)

class TTSFreeTextToSpeech:
    """Backward-compatible TTSFree provider."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError("TTSFree provider is no longer the primary provider. Use GeminiTextToSpeech.")


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError("Text-to-speech provider is not configured. Configure a TTS provider before generating audio.")
