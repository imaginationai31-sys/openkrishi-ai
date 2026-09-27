import logging

import pytest
from httpx import ASGITransport, AsyncClient

from services.api.server import app


@pytest.mark.anyio
async def test_request_trace_id_is_returned_and_reused():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/", headers={"X-Trace-ID": "test-trace-123"})

    assert response.status_code == 200
    assert response.headers["X-Trace-ID"] == "test-trace-123"


@pytest.mark.anyio
async def test_request_trace_id_is_generated():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/")

    trace_id = response.headers.get("X-Trace-ID")
    assert response.status_code == 200
    assert trace_id
    assert len(trace_id) == 16


def test_api_logger_is_configured():
    assert logging.getLogger("openkrishi.api").name == "openkrishi.api"


@pytest.mark.anyio
async def test_security_headers_are_present():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/")
    assert response.headers["X-Content-Type-Options"] == "nosniff"
    assert response.headers["X-Frame-Options"] == "DENY"
    assert response.headers["Referrer-Policy"] == "no-referrer"
    assert response.headers["Cache-Control"] == "no-store"


@pytest.mark.anyio
async def test_oversized_request_is_rejected_before_handler():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/", content=b"x", headers={"Content-Length": str(12 * 1024 * 1024 + 1)})
    assert response.status_code == 413
    assert response.json()["error"] == "request_too_large"
