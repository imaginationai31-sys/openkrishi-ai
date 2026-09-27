import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from services.api.middleware import RequestTraceMiddleware, SecurityHeadersMiddleware, RequestSizeLimitMiddleware
from services.api.routes import advisory, health, vision, voice, weather


def _allowed_origins() -> list[str]:
    raw = os.getenv("CORS_ALLOW_ORIGINS", "")
    origins = [origin.strip().rstrip("/") for origin in raw.split(",") if origin.strip()]
    if not origins:
        # Fail closed in production. Local development can explicitly set
        # CORS_ALLOW_ORIGINS=http://localhost:5173.
        return []
    if "*" in origins:
        raise RuntimeError("CORS_ALLOW_ORIGINS must contain explicit origins; wildcard '*' is not allowed.")
    return origins


app = FastAPI(
    title="OpenKrishi AI API",
    description="Open-source, multilingual agricultural intelligence API.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_allowed_origins(),
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Trace-ID", "X-Request-ID"],
    expose_headers=["X-Trace-ID"],
)

# Keep request limits and security headers at the edge of the application.
app.add_middleware(RequestSizeLimitMiddleware)
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RequestTraceMiddleware)

app.include_router(health.router, prefix="/api/v1")
app.include_router(advisory.router, prefix="/api/v1")
app.include_router(voice.router, prefix="/api/v1")
app.include_router(vision.router, prefix="/api/v1")
app.include_router(weather.router, prefix="/api/v1")


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "openkrishi-api", "version": "0.1.0", "status": "ok"}
