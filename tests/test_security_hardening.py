import os

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

os.environ.setdefault("CORS_ALLOW_ORIGINS", "https://openkrishi-ai.hatchable.site")
os.environ.setdefault("GEMINI_API_KEY", "test-gemini-key")
os.environ.setdefault("OPENAI_API_KEY", "test-openai-key")
os.environ.setdefault("SARVAM_API_KEY", "test-sarvam-key")

from services.api.server import app
from services.core.config import Settings


client = TestClient(app)


def test_oversized_image_rejected():
    payload = b"x" * (10 * 1024 * 1024 + 1)
    response = client.post(
        "/api/v1/vision/assess",
        files={"file": ("crop.jpg", payload, "image/jpeg")},
        data={"language": "en"},
    )
    assert response.status_code == 413


def test_wrong_image_type_rejected():
    response = client.post(
        "/api/v1/vision/assess",
        files={"file": ("crop.txt", b"not an image", "text/plain")},
        data={"language": "en"},
    )
    assert response.status_code == 400


def test_rate_limit_returns_429(monkeypatch):
    monkeypatch.setattr(
        "services.api.routes.advisory.generate_advisory",
        lambda **kwargs: {
            "language": kwargs["language"],
            "answer": "ok",
            "confidence": "low",
            "safety": {"status": "caution"},
            "observations": [],
            "recommendations": [],
            "uncertainties": [],
            "source_references": [],
        },
    )
    responses = [
        client.post(
            "/api/v1/advisory",
            json={"query": "yellow leaves", "language": "en"},
        )
        for _ in range(11)
    ]
    assert responses[-1].status_code == 429


def test_missing_config_fails_fast(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    with pytest.raises(ValidationError):
        Settings(_env_file=None)


def test_cors_blocks_unknown_origin():
    response = client.options(
        "/api/v1/advisory",
        headers={
            "Origin": "https://attacker.example",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "content-type",
        },
    )
    assert response.status_code == 400
    assert "access-control-allow-origin" not in response.headers
