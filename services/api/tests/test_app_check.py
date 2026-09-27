import asyncio

import pytest
from fastapi import HTTPException

from services.api.app_check import require_app_check


class DummyState:
    pass


class DummyHeaders(dict):
    def get(self, key, default=None):
        return super().get(key, default)


class DummyUrl:
    def __init__(self, path: str):
        self.path = path


class DummyRequest:
    def __init__(self, path: str, method: str = "GET", headers=None):
        self.url = DummyUrl(path)
        self.method = method
        self.headers = DummyHeaders(headers or {})
        self.state = DummyState()


def test_app_check_is_disabled_by_default(monkeypatch):
    monkeypatch.delenv("FIREBASE_APPCHECK_ENFORCE", raising=False)
    asyncio.run(require_app_check(DummyRequest("/api/v1/advisory")))


def test_health_endpoint_is_excluded_when_enforced(monkeypatch):
    monkeypatch.setenv("FIREBASE_APPCHECK_ENFORCE", "true")
    asyncio.run(require_app_check(DummyRequest("/api/v1/health")))


def test_missing_token_is_rejected_when_enforced(monkeypatch):
    monkeypatch.setenv("FIREBASE_APPCHECK_ENFORCE", "true")
    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(require_app_check(DummyRequest("/api/v1/advisory")))
    assert exc_info.value.status_code == 401


def test_valid_token_claims_are_saved(monkeypatch):
    monkeypatch.setenv("FIREBASE_APPCHECK_ENFORCE", "true")
    monkeypatch.setattr(
        "services.api.app_check.verify_app_check_token",
        lambda token: {"sub": "1:837358973413:web:49f86614fd155f29ce3f0c"},
    )
    request = DummyRequest(
        "/api/v1/advisory",
        headers={"X-Firebase-AppCheck": "test-token"},
    )
    asyncio.run(require_app_check(request))
    assert request.state.firebase_app_check["sub"].startswith("1:837358973413:web:")
