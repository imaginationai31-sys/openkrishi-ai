from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from services.api.server import app


client = TestClient(app)


def test_weather_rejects_unsupported_language():
    response = client.get(
        "/api/v1/weather",
        params={"latitude": 22.57, "longitude": 88.36, "language": "fr"},
    )
    assert response.status_code == 422
    assert "unsupported language" in response.json()["detail"]["message"].lower()


def test_weather_rejects_invalid_coordinates():
    response = client.get(
        "/api/v1/weather",
        params={"latitude": 100, "longitude": 88.36, "language": "bn"},
    )
    assert response.status_code == 422


def test_weather_provider_failure_returns_503():
    with patch(
        "services.api.routes.weather.get_weather",
        new=AsyncMock(side_effect=RuntimeError("Weather provider is temporarily unavailable.")),
    ):
        response = client.get(
            "/api/v1/weather",
            params={"latitude": 22.57, "longitude": 88.36, "language": "bn"},
        )

    assert response.status_code == 503
    assert "weather provider" in response.json()["detail"].lower()
