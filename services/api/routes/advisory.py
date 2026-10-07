from typing import Any

from fastapi import APIRouter, Request, Response
from pydantic import BaseModel, Field

from services.advisory.engine import generate_advisory
from services.advisory.localization import localize_advisory
from services.api.rate_limit import AI_LIMIT, limiter


router = APIRouter()


class AdvisoryRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    language: str = Field(pattern="^(en|bn|hi|ta|pa|te)$")
    crop_category: str | None = Field(default=None, pattern="^(rice|peanut|vegetables|flowers)$")
    crop_name: str | None = Field(default=None, max_length=100)
    growth_stage: str | None = Field(default=None, max_length=100)
    location: str | None = Field(default=None, max_length=200)
    input_mode: str = Field(default="text", pattern="^(text)$")


@router.post("/advisory")
@limiter.limit(AI_LIMIT)
def advisory(request: Request, response: Response, body: AdvisoryRequest) -> dict[str, Any]:
    result = generate_advisory(
        query=body.query,
        language=body.language,
        crop_category=body.crop_category,
        crop_name=body.crop_name,
        growth_stage=body.growth_stage,
        location=body.location,
    )
    return localize_advisory(result, body.language)
