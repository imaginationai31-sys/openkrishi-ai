from services.voice.languages import is_supported_language
from services.voice.pipeline import process_voice_query
from services.voice.speech_to_text import Transcription
from services.voice.text_to_speech import SpeechAudio


class FakeSTT:
    def transcribe(self, audio: bytes, language: str) -> Transcription:
        return Transcription(text="আমার ধান সম্পর্কে প্রশ্ন আছে", language=language, confidence=0.95)


class FakeTTS:
    def synthesize(self, text: str, language: str) -> SpeechAudio:
        return SpeechAudio(audio=b"fake-audio", language=language)


def test_supported_voice_languages():
    for language in ("bn", "hi", "ta", "pa", "te"):
        assert is_supported_language(language)


def test_voice_pipeline_reaches_advisory_engine():
    result = process_voice_query(
        b"fake-input",
        "bn",
        FakeSTT(),
        FakeTTS(),
        crop_category="rice",
    )

    assert result["transcription"]["language"] == "bn"
    assert result["advisory"]["language"] == "bn"
    assert result["audio"].audio == b"fake-audio"


def test_unsupported_voice_language_is_rejected():
    try:
        process_voice_query(b"fake-input", "en", FakeSTT(), FakeTTS())
    except ValueError as exc:
        assert "Unsupported voice language" in str(exc)
    else:
        raise AssertionError("Expected unsupported language to be rejected")
