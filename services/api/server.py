from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from services.api.middleware import RequestTraceMiddleware
from services.api.rate_limit import limiter
from services.api.routes import advisory, farm, health, vision, voice, weather
from services.core.config import get_settings
from services.core.logging import configure_logging
from services.core.metrics import metrics

configure_logging()
settings = get_settings()

app = FastAPI(
    title="OpenKrishi AI API",
    description="Open-source, multilingual agricultural intelligence API.",
    version="0.1.0",
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Content-Type", "Accept", "X-Trace-ID", "X-Firebase-AppCheck"],
)

app.add_middleware(RequestTraceMiddleware)

app.include_router(health.router, prefix="/api/v1")
app.include_router(advisory.router, prefix="/api/v1")
app.include_router(voice.router, prefix="/api/v1")
app.include_router(vision.router, prefix="/api/v1")
app.include_router(weather.router, prefix="/api/v1")
app.include_router(farm.router, prefix="/api/v1")


@app.exception_handler(Exception)
async def unhandled_exception(request: Request, exc: Exception):
    import logging

    logging.getLogger("openkrishi.api").error(
        "unhandled_request_error",
        extra={
            "trace_id": getattr(request.state, "trace_id", "unknown"),
            "method": request.method,
            "path": request.url.path,
            "status_code": 500,
            "error_type": type(exc).__name__,
        },
    )
    return JSONResponse(
        status_code=500,
        content={"detail": "An internal error occurred. Please try again."},
    )


@app.get("/metrics", include_in_schema=False)
def metrics_endpoint() -> Response:
    return Response(content=metrics.prometheus(), media_type="text/plain; version=0.0.4")


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "openkrishi-api", "version": "0.1.0", "status": "ok"}
