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
