import asyncio

import httpx
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


def test_weather_retries_after_rate_limit():
    rate_limited = httpx.Response(429, headers={"retry-after": "0.1"}, request=httpx.Request("GET", "https://example.test"))
    success = httpx.Response(
        200,
        json={
            "latitude": 22.57,
            "longitude": 88.36,
            "timezone": "Asia/Kolkata",
            "elevation": 10,
            "current": {"temperature_2m": 28, "relative_humidity_2m": 70, "precipitation": 0, "rain": 0, "wind_speed_10m": 10, "weather_code": 0},
            "hourly": {"precipitation_probability": [0], "rain": [0], "showers": [0], "relative_humidity_2m": [70], "temperature_2m": [28], "wind_speed_10m": [10]},
            "daily": {"time": ["2026-10-01"], "temperature_2m_max": [30], "temperature_2m_min": [24], "precipitation_probability_max": [0], "precipitation_sum": [0], "wind_speed_10m_max": [10], "et0_fao_evapotranspiration": [4]},
        },
        request=httpx.Request("GET", "https://example.test"),
    )
    with patch("services.weather.engine.httpx.AsyncClient") as client_class, patch("services.weather.engine.asyncio.sleep", new=AsyncMock()):
        client = client_class.return_value.__aenter__.return_value
        client.get = AsyncMock(side_effect=[rate_limited, success])
        from services.weather.engine import get_weather
        result = asyncio.run(get_weather(22.57, 88.36, "en", 1))
    assert result["source"] == "Open-Meteo"
    assert client.get.await_count == 2


def test_weather_cache_avoids_repeat_provider_call():
    with patch("services.weather.engine.httpx.AsyncClient") as client_class:
        client = client_class.return_value.__aenter__.return_value
        response = httpx.Response(
            200,
            json={
                "latitude": 22.57, "longitude": 88.36, "timezone": "Asia/Kolkata", "elevation": 10,
                "current": {"temperature_2m": 28, "relative_humidity_2m": 70, "precipitation": 0, "rain": 0, "wind_speed_10m": 10, "weather_code": 0},
                "hourly": {"precipitation_probability": [0], "rain": [0], "showers": [0], "relative_humidity_2m": [70], "temperature_2m": [28], "wind_speed_10m": [10]},
                "daily": {"time": ["2026-10-01"], "temperature_2m_max": [30], "temperature_2m_min": [24], "precipitation_probability_max": [0], "precipitation_sum": [0], "wind_speed_10m_max": [10], "et0_fao_evapotranspiration": [4]},
            },
            request=httpx.Request("GET", "https://example.test"),
        )
        client.get = AsyncMock(return_value=response)
        from services.weather.engine import get_weather
        import services.weather.engine as engine
        engine._WEATHER_CACHE.clear()
        asyncio.run(get_weather(22.571, 88.361, "en", 1))
        asyncio.run(get_weather(22.571, 88.361, "en", 1))
    assert client.get.await_count == 1
