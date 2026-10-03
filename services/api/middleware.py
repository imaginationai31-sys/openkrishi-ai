import logging
import re
import time
import uuid
from collections.abc import Awaitable, Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

from services.core.metrics import metrics


logger = logging.getLogger("openkrishi.api")
TRACE_ID_PATTERN = re.compile(r"^[A-Za-z0-9._-]{8,64}$")


class RequestTraceMiddleware(BaseHTTPMiddleware):
    """Attach a safe trace ID to every request and response."""

    async def dispatch(
        self,
        request: Request,
        call_next: Callable[[Request], Awaitable[Response]],
    ) -> Response:
        candidate = request.headers.get("X-Trace-ID", "")
        trace_id = candidate if TRACE_ID_PATTERN.fullmatch(candidate) else uuid.uuid4().hex[:16]
        request.state.trace_id = trace_id
        started = time.perf_counter()

        try:
            response = await call_next(request)
        except Exception:
            elapsed_ms = (time.perf_counter() - started) * 1000
            logger.exception(
                "request_failed",
                extra={
                    "trace_id": trace_id,
                    "method": request.method,
                    "path": request.url.path,
                    "duration_ms": round(elapsed_ms, 1),
                },
            )
            metrics.record(elapsed_ms / 1000, 500)
            raise

        elapsed_ms = (time.perf_counter() - started) * 1000
        response.headers["X-Trace-ID"] = trace_id
        metrics.record(elapsed_ms / 1000, response.status_code)
        logger.info(
            "request_complete",
            extra={
                "trace_id": trace_id,
                "method": request.method,
                "path": request.url.path,
                "status_code": response.status_code,
                "duration_ms": round(elapsed_ms, 1),
            },
        )
        return response
