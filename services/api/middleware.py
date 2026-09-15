import logging
import time
import uuid

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger("openkrishi.api")


class RequestTraceMiddleware(BaseHTTPMiddleware):
    """Attach a short trace ID to every request and response."""

    async def dispatch(self, request: Request, call_next):
        trace_id = request.headers.get("X-Trace-ID") or uuid.uuid4().hex[:16]
        request.state.trace_id = trace_id
        started = time.perf_counter()

        try:
            response = await call_next(request)
        except Exception:
            elapsed_ms = (time.perf_counter() - started) * 1000
            logger.exception(
                "request_failed trace_id=%s method=%s path=%s duration_ms=%.1f",
                trace_id,
                request.method,
                request.url.path,
                elapsed_ms,
            )
            raise

        elapsed_ms = (time.perf_counter() - started) * 1000
        response.headers["X-Trace-ID"] = trace_id
        logger.info(
            "request_complete trace_id=%s method=%s path=%s status=%s duration_ms=%.1f",
            trace_id,
            request.method,
            request.url.path,
            response.status_code,
            elapsed_ms,
        )
        return response
