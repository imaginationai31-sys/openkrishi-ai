import os

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware\nfrom fastapi.responses import JSONResponse\nfrom slowapi.errors import RateLimitExceeded\nfrom slowapi import _rate_limit_exceeded_handler

from services.api.middleware import RequestTraceMiddleware
from services.api.routes import advisory, farm, health, vision, voice, weather

app = FastAPI(
    title="OpenKrishi AI API",
    description="Open-source, multilingual agricultural intelligence API.",
    version="0.1.0",
)

# The farmer-facing frontend is hosted separately from the Render API.
# Allow cross-origin browser requests while keeping credentials disabled.
allowed_origins = [
    origin.strip()
    for origin in os.getenv("CORS_ALLOW_ORIGINS", "*").split(",")
    if origin.strip()
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"]
)

app.add_middleware(RequestTraceMiddleware)

app.include_router(health.router, prefix="/api/v1")
app.include_router(advisory.router, prefix="/api/v1")
app.include_router(voice.router, prefix="/api/v1")
app.include_router(vision.router, prefix="/api/v1")
app.include_router(weather.router, prefix="/api/v1")
app.include_router(farm.router, prefix="/api/v1")\n\n\n@app.exception_handler(Exception)\nasync def unhandled_exception(request: Request, exc: Exception):\n    import logging\n    logging.getLogger("openkrishi.api").error("unhandled_request_error trace_id=%s error_type=%s", getattr(request.state, "trace_id", "unknown"), type(exc).__name__)\n    return JSONResponse(status_code=500, content={"detail": "An internal error occurred. Please try again."})


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "openkrishi-api", "version": "0.1.0", "status": "ok"}
