from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.vision.engine import assess_crop_image

router = APIRouter()


@router.post("/vision/assess")
async def vision_assess(
    file: UploadFile = File(...),
    crop_category: str | None = Form(default=None),
    growth_stage: str | None = Form(default=None),
) -> dict[str, Any]:
    """Validate a farmer crop photo and prepare it for visual assessment."""
    try:
        image_bytes = await file.read()
        return assess_crop_image(
            image_bytes=image_bytes,
            content_type=file.content_type or "",
            crop_category=crop_category,
            growth_stage=growth_stage,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
