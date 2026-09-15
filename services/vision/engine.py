"""Conservative vision engine boundary for crop photos.

The first production-safe layer validates images and returns structured visual
assessment fields. It never invents a disease or treatment from an image.
A model provider can populate the same schema later without changing the API.
"""

from __future__ import annotations

from typing import Any

SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}
SUPPORTED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_IMAGE_BYTES = 10 * 1024 * 1024


def assess_crop_image(
    image_bytes: bytes,
    content_type: str,
    crop_category: str | None = None,
    growth_stage: str | None = None,
) -> dict[str, Any]:
    """Validate and structure a crop image for downstream visual analysis."""
    normalized_type = (content_type or "").lower().split(";")[0].strip()

    if not image_bytes:
        raise ValueError("Image is empty.")
    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise ValueError("Image exceeds the 10 MB limit.")
    if normalized_type not in SUPPORTED_IMAGE_TYPES:
        raise ValueError("Unsupported image type. Use JPEG, PNG, or WebP.")
    if crop_category and crop_category not in SUPPORTED_CROPS:
        raise ValueError("Unsupported crop category.")

    return {
        "status": "ready_for_visual_model",
        "image": {
            "content_type": normalized_type,
            "size_bytes": len(image_bytes),
        },
        "crop_category": crop_category,
        "growth_stage": growth_stage,
        "observations": [],
        "possible_causes": [],
        "confidence": "low",
        "safety": "caution",
        "uncertainties": [
            "No visual diagnosis is made until a vision model assesses the image.",
            "A photo alone may not distinguish disease, pest, nutrient, water, or environmental stress reliably.",
        ],
        "recommendations": [
            "Use a clear close-up photo of affected leaves or plant parts in good natural light.",
            "Include more than one affected area when possible and avoid blurry or heavily shadowed images.",
            "Do not apply pesticide or a large fertilizer dose based on an image alone.",
        ],
    }
