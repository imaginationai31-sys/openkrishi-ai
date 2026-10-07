from fastapi import APIRouter

from services.core.config import get_settings


router = APIRouter()


@router.get("/health")
def health() -> dict[str, object]:
    settings = get_settings()
    return {
        "status": "ok",
        "service": "openkrishi-api",
        "checks": {
            "configuration": "ok",
            "openai": "configured" if settings.openai_api_key.get_secret_value() else "missing",
            "sarvam": "configured" if settings.sarvam_api_key.get_secret_value() else "missing",
        },
    }
