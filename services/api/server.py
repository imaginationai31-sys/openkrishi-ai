from fastapi import FastAPI

from services.api.routes import advisory, health, voice

app = FastAPI(
    title="OpenKrishi AI API",
    description="Open-source, multilingual agricultural intelligence API.",
    version="0.1.0",
)

app.include_router(health.router, prefix="/api/v1")
app.include_router(advisory.router, prefix="/api/v1")
app.include_router(voice.router, prefix="/api/v1")


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "openkrishi-api", "version": "0.1.0", "status": "ok"}
