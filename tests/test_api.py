from fastapi.testclient import TestClient

from services.api.server import app

client = TestClient(app)


def test_health_endpoint() -> None:
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "openkrishi-api"}


def test_advisory_bengali_rice() -> None:
    response = client.post(
        "/api/v1/advisory",
        json={
            "query": "ধানের জন্য কী তথ্য দরকার?",
            "language": "bn",
            "crop_category": "rice",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["language"] == "bn"
    assert body["confidence"] == "low"
    assert body["safety"]["status"] == "caution"
    assert body["answer"]


def test_invalid_language_is_rejected() -> None:
    response = client.post(
        "/api/v1/advisory",
        json={"query": "test", "language": "xx"},
    )
    assert response.status_code == 422
