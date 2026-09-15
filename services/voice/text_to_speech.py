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


GEMINI_TTS_MODEL = "gemini-3.1-flash-tts-preview"
GEMINI_TTS_LANGUAGES = {"bn": "bn-IN", "hi": "hi-IN", "ta": "ta-IN", "pa": "pa-IN", "te": "te-IN"}
GEMINI_TTS_VOICES = {"bn": "Kore", "hi": "Kore", "ta": "Kore", "pa": "Kore", "te": "Kore"}


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


class GeminiTextToSpeech:
    """Indian-language TTS backed by Gemini 3.1 Flash TTS."""

    def __init__(self, model: str | None = None, voice: str | None = None) -> None:
        self.model = model or os.getenv("GEMINI_TTS_MODEL", GEMINI_TTS_MODEL)
        self.voice = voice

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        if not text.strip():
            raise ValueError("Text input cannot be empty.")

        language_code = GEMINI_TTS_LANGUAGES.get(language)
        if not language_code:
            raise ValueError(f"Unsupported TTS language: {language}")

        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY is not configured. Set it before generating audio.")

        from google import genai

        client = genai.Client(api_key=api_key)
        voice = self.voice or GEMINI_TTS_VOICES[language]
        prompt = (
            f"Synthesize the following OpenKrishi farmer advisory in {language_code}. "
            "Speak naturally, clearly, warmly, and at a moderate pace. "
            "Do not add or remove information. Spoken text:\n" + text
        )
        try:
            interaction = client.interactions.create(
                model=self.model,
                input=prompt,
                response_format={"type": "audio"},
                generation_config={
                    "speech_config": [{"voice": voice, "language": language_code}]
                },
            )
        except Exception as exc:
            raise RuntimeError("Text-to-speech provider is temporarily unavailable.") from exc

        output_audio = getattr(interaction, "output_audio", None)
        data = getattr(output_audio, "data", None) if output_audio is not None else None
        if not data:
            raise RuntimeError("Gemini TTS returned empty audio.")

        try:
            pcm = base64.b64decode(data)
        except Exception as exc:
            raise RuntimeError("Gemini TTS returned invalid audio data.") from exc

        if not pcm:
            raise RuntimeError("Gemini TTS returned empty audio.")
        return SpeechAudio(audio=_pcm_to_wav(pcm), language=language, mime_type="audio/wav")


class TTSFreeTextToSpeech:
    """Backward-compatible TTSFree provider."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError("TTSFree provider is no longer the primary provider. Use GeminiTextToSpeech.")


class NotConfiguredTextToSpeech:
    """Safe placeholder until a concrete TTS provider is configured."""

    def synthesize(self, text: str, language: str) -> SpeechAudio:
        raise RuntimeError("Text-to-speech provider is not configured. Configure a TTS provider before generating audio.")
