from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.advisory.engine import generate_advisory
from services.vision.engine import assess_crop_image

router = APIRouter()

SUPPORTED_LANGUAGES = {"en", "bn", "hi", "ta", "pa", "te"}


@router.post("/vision/assess")
async def vision_assess(
    file: UploadFile = File(...),
    crop_category: str | None = Form(default=None),
    growth_stage: str | None = Form(default=None),
    language: str = Form(default="en"),
    location: str | None = Form(default=None),
) -> dict[str, Any]:
    """Assess a crop photo and turn visible findings into safe agronomy guidance."""
    if language not in SUPPORTED_LANGUAGES:
        raise HTTPException(status_code=400, detail="Unsupported language.")

    try:
        image_bytes = await file.read()
        visual = assess_crop_image(
            image_bytes=image_bytes,
            content_type=file.content_type or "",
            crop_category=crop_category,
            growth_stage=growth_stage,
        )

        observations = visual.get("observations", [])
        possible_causes = visual.get("possible_causes", [])
        visual_query = "; ".join([*observations, *possible_causes]).strip()

        if visual.get("status") == "assessed" and visual_query:
            advisory_query = visual_query
        else:
            advisory_query = "crop photo assessment is unclear; no reliable visual symptom identified"

        advisory = generate_advisory(
            query=advisory_query,
            language=language,
            crop_category=crop_category,
            growth_stage=growth_stage,
            location=location,
        )

        return {
            "status": visual["status"],
            "image": visual["image"],
            "crop_category": crop_category,
            "growth_stage": growth_stage,
            "vision": {
                "observations": observations,
                "possible_causes": possible_causes,
                "confidence": visual["confidence"],
                "safety": visual["safety"],
                "uncertainties": visual["uncertainties"],
            },
            "advisory": advisory,
        }
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
