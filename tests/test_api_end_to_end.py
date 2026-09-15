from unittest.mock import patch

from fastapi.testclient import TestClient

from services.api.server import app
from services.voice.speech_to_text import Transcription
from services.voice.text_to_speech import SpeechAudio


client = TestClient(app)


def make_audio_upload(filename="farmer.ogg", content_type="audio/ogg", content=b"fake-audio"):
    return (filename, content, content_type)


def make_image_upload(filename="rice.jpg", content_type="image/jpeg", content=b"fake-image"):
    return (filename, content, content_type)


def test_root_health():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.headers.get("x-trace-id")


def test_advisory_http_success():
    response = client.post(
        "/api/v1/advisory",
        json={
            "query": "My rice plants have yellow leaves.",
            "language": "en",
            "crop_category": "rice",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["language"] == "en"
    assert body["confidence"] == "low"
    assert body["safety"]["status"] == "caution"
    assert body["recommendations"]


def test_advisory_rejects_unsupported_crop():
    response = client.post(
        "/api/v1/advisory",
        json={
            "query": "My tomato plants have yellow leaves.",
            "language": "en",
            "crop_category": "tomato",
        },
    )
    assert response.status_code == 422


def test_voice_transcribe_empty_audio_returns_400():
    response = client.post(
        "/api/v1/voice/transcribe",
        files={"file": make_audio_upload(content=b"")},
        data={"language": "bn"},
    )
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_voice_transcribe_provider_failure_returns_503():
    with patch(
        "services.api.routes.voice.GroqSpeechToText.transcribe",
        side_effect=RuntimeError("Speech-to-text provider is temporarily unavailable."),
    ):
        response = client.post(
            "/api/v1/voice/transcribe",
            files={"file": make_audio_upload()},
            data={"language": "bn"},
        )
    assert response.status_code == 503
    assert "speech-to-text provider" in response.json()["detail"].lower()


def test_voice_advisory_end_to_end_with_provider_mocks():
    transcription = Transcription(text="ধানের পাতায় হলুদ দাগ আছে", language="bn", confidence=None)
    spoken = SpeechAudio(audio=b"RIFF-fake-wav", mime_type="audio/wav", language="bn")

    with (
        patch("services.api.routes.voice.GroqSpeechToText.transcribe", return_value=transcription),
        patch("services.api.routes.voice.TTSFreeTextToSpeech.synthesize", return_value=spoken),
    ):
        response = client.post(
            "/api/v1/voice/advisory",
            files={"file": make_audio_upload()},
            data={"language": "bn", "crop_category": "rice"},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["transcription"]["text"] == transcription.text
    assert body["transcription"]["language"] == "bn"
    assert body["advisory"]["confidence"] == "low"
    assert body["audio"]["mime_type"] == "audio/wav"
    assert body["audio"]["base64"]


def test_vision_assess_invalid_image_returns_400():
    with patch(
        "services.api.routes.vision.assess_crop_image",
        side_effect=ValueError("Unsupported image type."),
    ):
        response = client.post(
            "/api/v1/vision/assess",
            files={"file": make_image_upload()},
            data={"language": "en", "crop_category": "rice"},
        )
    assert response.status_code == 400
    assert "unsupported image" in response.json()["detail"].lower()


def test_vision_assess_provider_failure_returns_502():
    with patch(
        "services.api.routes.vision.assess_crop_image",
        side_effect=RuntimeError("Vision provider is temporarily unavailable."),
    ):
        response = client.post(
            "/api/v1/vision/assess",
            files={"file": make_image_upload()},
            data={"language": "en", "crop_category": "rice"},
        )
    assert response.status_code == 502
    assert "vision provider" in response.json()["detail"].lower()


def test_voice_vision_advisory_end_to_end_with_provider_mocks():
    transcription = Transcription(text="ধানের পাতা হলুদ হয়ে যাচ্ছে", language="bn", confidence=None)
    spoken = SpeechAudio(audio=b"RIFF-fake-wav", mime_type="audio/wav", language="bn")
    visual = {
        "status": "assessed",
        "image": {"content_type": "image/jpeg", "size_bytes": 10},
        "observations": ["yellowing leaves"],
        "possible_causes": ["water stress"],
        "confidence": "medium",
        "safety": {"status": "caution", "reason": "Conservative visual assessment."},
        "uncertainties": ["Photo evidence is limited."],
    }

    with (
        patch("services.api.routes.voice.GroqSpeechToText.transcribe", return_value=transcription),
        patch("services.api.routes.voice.assess_crop_image", return_value=visual),
        patch("services.api.routes.voice.TTSFreeTextToSpeech.synthesize", return_value=spoken),
    ):
        response = client.post(
            "/api/v1/voice/vision-advisory",
            files={"file": make_audio_upload(), "image": make_image_upload()},
            data={"language": "bn", "crop_category": "rice"},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["transcription"]["text"] == transcription.text
    assert body["transcription"]["language"] == "bn"
    assert body["vision"]["status"] == "assessed"
    assert body["vision"]["observations"] == ["yellowing leaves"]
    assert body["advisory"]["confidence"] == "low"
    assert body["audio"]["base64"]


def test_voice_vision_advisory_rejects_unsupported_language():
    response = client.post(
        "/api/v1/voice/vision-advisory",
        files={"file": make_audio_upload(), "image": make_image_upload()},
        data={"language": "fr", "crop_category": "rice"},
    )
    assert response.status_code == 422
    assert "unsupported voice language" in response.json()["detail"].lower()
