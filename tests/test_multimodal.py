from io import BytesIO
from types import SimpleNamespace

import pytest
from fastapi import HTTPException
from starlette.datastructures import Headers, UploadFile

import services.api.routes.voice as voice_route


class FakeSpeechToText:
    def transcribe(self, audio, language, filename=None, content_type=None):
        return SimpleNamespace(
            text="My rice leaves are yellow",
            normalized_text=None,
            language=language,
            confidence="high",
        )


class FakeTTS:
    def synthesize(self, text, language):
        return SimpleNamespace(audio=b"wav-audio", mime_type="audio/wav", language=language)


def make_upload(filename, content_type, content):
    return UploadFile(
        file=BytesIO(content),
        filename=filename,
        headers=Headers({"content-type": content_type}),
    )


@pytest.mark.anyio
async def test_voice_vision_advisory_combines_voice_and_photo(monkeypatch):
    monkeypatch.setattr(voice_route, "GroqSpeechToText", FakeSpeechToText)
    monkeypatch.setattr(voice_route, "TTSFreeTextToSpeech", FakeTTS)
    monkeypatch.setattr(
        voice_route,
        "normalize_agricultural_terms",
        lambda text, language: ("yellow leaf", ["yellow leaf"]),
    )
    monkeypatch.setattr(
        voice_route,
        "build_voice_understanding",
        lambda *args: {"needs_clarification": False, "follow_up_questions": []},
    )
    monkeypatch.setattr(
        voice_route,
        "assess_crop_image",
        lambda **kwargs: {
            "status": "assessed",
            "observations": ["leaf yellowing"],
            "possible_causes": ["nutrient or water stress"],
            "confidence": "medium",
            "safety": "caution",
            "uncertainties": [],
        },
    )
    captured = {}

    def fake_advisory(**kwargs):
        captured.update(kwargs)
        return {
            "answer": "Check soil moisture before applying inputs.",
            "confidence": "medium",
            "safety": "caution",
            "observations": [],
            "recommendations": [],
            "uncertainties": [],
        }

    monkeypatch.setattr(voice_route, "generate_advisory", fake_advisory)

    audio = make_upload("farmer.ogg", "audio/ogg", b"audio")
    image = make_upload("rice.jpg", "image/jpeg", b"image")

    result = await voice_route.voice_vision_advisory(
        file=audio,
        image=image,
        language="bn",
        crop_category="rice",
        growth_stage="tillering",
        location="West Bengal",
    )

    assert result["transcription"]["normalized_text"] == "yellow leaf"
    assert result["vision"]["status"] == "assessed"
    assert "yellow leaf" in captured["query"]
    assert "leaf yellowing" in captured["query"]
    assert result["audio"]["mime_type"] == "audio/wav"


@pytest.mark.anyio
async def test_voice_vision_advisory_rejects_unsupported_crop(monkeypatch):
    audio = make_upload("farmer.ogg", "audio/ogg", b"audio")
    image = make_upload("rice.jpg", "image/jpeg", b"image")

    with pytest.raises(HTTPException) as exc:
        await voice_route.voice_vision_advisory(
            file=audio,
            image=image,
            language="bn",
            crop_category="tomato",
        )
    assert exc.value.status_code == 422
