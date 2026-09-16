from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from services.advisory.engine import generate_advisory
from services.advisory.localization import localize_advisory, localize_visual
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
    """Assess a crop photo and return all farmer-facing text in the selected language."""
    if language not in SUPPORTED_LANGUAGES:
        raise HTTPException(status_code=400, detail="Unsupported language.")

    try:
        image_bytes = await file.read()
        visual = assess_crop_image(
            image_bytes=image_bytes,
            content_type=file.content_type or "",
            crop_category=crop_category,
            growth_stage=growth_stage,
            language=language,
        )
        visual = localize_visual(visual, language)

        observations = visual.get("observations", [])
        possible_causes = visual.get("possible_causes", [])
        visual_query = "; ".join([*observations, *possible_causes]).strip()
        advisory_query = visual_query if visual.get("status") == "assessed" and visual_query else "crop photo assessment is unclear; no reliable visual symptom identified"

        advisory = generate_advisory(
            query=advisory_query,
            language=language,
            crop_category=crop_category,
            growth_stage=growth_stage,
            location=location,
        )
        advisory = localize_advisory(advisory, language)

        return {
            "status": visual["status"],
            "image": visual["image"],
            "crop_category": crop_category,
            "growth_stage": growth_stage,
            "vision": {
                "language": language,
                "observations": observations,
                "possible_causes": possible_causes,
                "confidence": visual["confidence"],
                "safety": visual["safety"],
                "uncertainties": visual["uncertainties"],
                "recommendations": visual.get("recommendations", []),
            },
            "advisory": advisory,
        }
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
