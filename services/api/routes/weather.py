from fastapi import APIRouter, HTTPException, Query

from services.weather.engine import SUPPORTED_LANGUAGES, get_weather

router = APIRouter(prefix="/weather", tags=["weather"])


@router.get("")
async def weather(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    language: str = Query("en"),
    forecast_days: int = Query(7, ge=1, le=7),
) -> dict:
    if language not in SUPPORTED_LANGUAGES:
        raise HTTPException(
            status_code=422,
            detail={
                "message": "Unsupported language",
                "supported_languages": sorted(SUPPORTED_LANGUAGES),
            },
        )
    try:
        return await get_weather(
            latitude=latitude,
            longitude=longitude,
            language=language,
            forecast_days=forecast_days,
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
