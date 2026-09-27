import logging
import time
import uuid

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

logger = logging.getLogger("openkrishi.api")

MAX_REQUEST_BYTES = 12 * 1024 * 1024


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


class RequestSizeLimitMiddleware(BaseHTTPMiddleware):
    """Reject obviously oversized requests before application processing."""

    async def dispatch(self, request: Request, call_next):
        content_length = request.headers.get("content-length")
        if content_length:
            try:
                size = int(content_length)
            except ValueError:
                return JSONResponse(status_code=400, content={"error": "invalid_content_length"})
            if size > MAX_REQUEST_BYTES:
                return JSONResponse(
                    status_code=413,
                    content={"error": "request_too_large", "message": "Request exceeds the 12 MB limit."},
                )
        return await call_next(request)


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """Apply baseline browser/API security headers."""

    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("X-Frame-Options", "DENY")
        response.headers.setdefault("Referrer-Policy", "no-referrer")
        response.headers.setdefault("Permissions-Policy", "camera=(self), microphone=(self), geolocation=(self)")
        response.headers.setdefault("Cache-Control", "no-store")
        if request.url.scheme == "https":
            response.headers.setdefault("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        return response
