from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel, Field

from services.advisory.engine import generate_advisory

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
def advisory(request: AdvisoryRequest) -> dict[str, Any]:
    return generate_advisory(
        query=request.query,
        language=request.language,
        crop_category=request.crop_category,
        growth_stage=request.growth_stage,
    )
