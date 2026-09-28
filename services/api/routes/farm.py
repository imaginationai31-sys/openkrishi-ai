from typing import Any
from fastapi import APIRouter, HTTPException, Query

from services.farm.intelligence import (
    build_crop_knowledge,
    build_crop_recommendation,
    build_farm_plan,
    build_fertilizer,
    build_irrigation,
    build_pest_alerts,
)
from services.weather.engine import get_weather, SUPPORTED_LANGUAGES

router = APIRouter(prefix="/farm", tags=["farm-intelligence"])

def _err(exc: Exception) -> None:
    raise HTTPException(status_code=422, detail=str(exc)) from exc

@router.get("/plan")
async def farm_plan(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    crop_category: str = Query(...),
    growth_stage: str | None = Query(None),
    language: str = Query("en"),
) -> dict[str, Any]:
    if language not in SUPPORTED_LANGUAGES:
        raise HTTPException(status_code=422, detail="Unsupported language")
    try:
        weather = await get_weather(latitude, longitude, language, 3)
        return build_farm_plan(crop_category, growth_stage, language, weather)
    except ValueError as exc:
        _err(exc)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

@router.get("/irrigation")
async def irrigation(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    crop_category: str = Query(...),
    language: str = Query("en"),
) -> dict[str, Any]:
    try:
        weather = await get_weather(latitude, longitude, language, 1)
        current = weather.get("current", {})
        return build_irrigation(crop_category, language, current.get("temperature_2m"), current.get("precipitation"), current.get("et0_fao_evapotranspiration"))
    except ValueError as exc:
        _err(exc)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

@router.get("/fertilizer")
def fertilizer(crop_category: str = Query(...), growth_stage: str | None = Query(None), language: str = Query("en")) -> dict[str, Any]:
    try:
        return build_fertilizer(crop_category, growth_stage, language)
    except ValueError as exc:
        _err(exc)

@router.get("/pest-alerts")
async def pest_alerts(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    crop_category: str = Query(...),
    language: str = Query("en"),
) -> dict[str, Any]:
    try:
        weather = await get_weather(latitude, longitude, language, 1)
        current = weather.get("current", {})
        return build_pest_alerts(crop_category, language, current.get("precipitation_probability"), current.get("relative_humidity_2m"))
    except ValueError as exc:
        _err(exc)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

@router.get("/recommendation")
def recommendation(
    soil_type: str | None = Query(None),
    water_availability: str | None = Query(None),
    season: str | None = Query(None),
    language: str = Query("en"),
) -> dict[str, Any]:
    return build_crop_recommendation(soil_type, water_availability, season, language)

@router.get("/knowledge")
def knowledge(crop_category: str = Query(...), language: str = Query("en")) -> dict[str, Any]:
    try:
        return build_crop_knowledge(crop_category, language)
    except ValueError as exc:
        _err(exc)

@router.get("/market")
def market(crop_category: str = Query(...), language: str = Query("en")) -> dict[str, Any]:
    names = {"rice": "Paddy / Rice", "peanut": "Peanut / Groundnut", "vegetables": "Vegetables", "flowers": "Flowers"}
    if crop_category not in names:
        raise HTTPException(status_code=422, detail="Unsupported crop category")
    return {
        "crop_category": crop_category,
        "commodity_hint": names[crop_category],
        "status": "official_source",
        "message": "OpenKrishi links farmers to official market-price sources. Live mandi data integration requires an approved data API credential.",
        "sources": [
            {"name": "e-NAM", "url": "https://enam.gov.in/"},
            {"name": "data.gov.in AGMARKNET daily mandi prices", "url": "https://www.data.gov.in/resource/current-daily-price-various-commodities-various-markets-mandi"},
        ],
        "safety": "Prices change by market, commodity, variety and date. Do not treat a single price as a guaranteed selling price.",
    }
