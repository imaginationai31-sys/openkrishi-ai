"""Crop image assessment with a conservative OpenAI vision provider."""

from __future__ import annotations

import base64
import json
import os
from typing import Any

SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}
SUPPORTED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_IMAGE_BYTES = 10 * 1024 * 1024


VISION_SYSTEM_PROMPT = """You are a conservative agricultural image-assessment assistant.
Assess only what is visibly supported by the crop photo.
Do not claim a definitive disease, pest, nutrient deficiency, or treatment.
Do not recommend pesticide products, rates, or large fertilizer doses.
Return JSON with exactly these keys:
observations (array of short strings), possible_causes (array of cautious possibilities),
confidence (one of low, medium, high), uncertainties (array of strings),
recommendations (array of safe next-step checks).
If the image is unclear, say so and use low confidence.
"""


def _validate_image(image_bytes: bytes, content_type: str, crop_category: str | None) -> str:
    normalized_type = (content_type or "").lower().split(";", 1)[0].strip()
    if not image_bytes:
        raise ValueError("Image is empty.")
    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise ValueError("Image exceeds the 10 MB limit.")
    if normalized_type not in SUPPORTED_IMAGE_TYPES:
        raise ValueError("Unsupported image type. Use JPEG, PNG, or WebP.")
    if crop_category and crop_category not in SUPPORTED_CROPS:
        raise ValueError("Unsupported crop category.")
    return normalized_type


def _safe_result(payload: dict[str, Any]) -> dict[str, Any]:
    confidence = payload.get("confidence")
    if confidence not in {"low", "medium", "high"}:
        confidence = "low"
    return {
        "observations": [str(x) for x in payload.get("observations", [])][:8],
        "possible_causes": [str(x) for x in payload.get("possible_causes", [])][:8],
        "confidence": confidence,
        "safety": "caution",
        "uncertainties": [str(x) for x in payload.get("uncertainties", [])][:8],
        "recommendations": [str(x) for x in payload.get("recommendations", [])][:8],
    }


def assess_crop_image(image_bytes: bytes, content_type: str, crop_category: str | None = None, growth_stage: str | None = None) -> dict[str, Any]:
    """Assess a crop image using OpenAI when configured, otherwise validate it."""
    normalized_type = _validate_image(image_bytes, content_type, crop_category)
    api_key = os.getenv("OPENAI_API_KEY")

    if not api_key:
        return {
            "status": "ready_for_visual_model",
            "image": {"content_type": normalized_type, "size_bytes": len(image_bytes)},
            "crop_category": crop_category,
            "growth_stage": growth_stage,
            "observations": [],
            "possible_causes": [],
            "confidence": "low",
            "safety": "caution",
            "uncertainties": ["OPENAI_API_KEY is not configured; no visual model was called."],
            "recommendations": [
                "Use a clear close-up photo of affected plant parts in good natural light.",
                "Do not apply pesticide or a large fertilizer dose based on an image alone.",
            ],
        }

    from openai import OpenAI

    client = OpenAI(api_key=api_key)
    image_url = f"data:{normalized_type};base64,{base64.b64encode(image_bytes).decode('ascii')}"
    context = f"Crop category: {crop_category or 'unknown'}; growth stage: {growth_stage or 'unknown'}."

    try:
        response = client.responses.create(
            model=os.getenv("OPENAI_VISION_MODEL", "gpt-5.6-luna"),
            input=[
                {"role": "system", "content": [{"type": "input_text", "text": VISION_SYSTEM_PROMPT}]},
                {"role": "user", "content": [
                    {"type": "input_text", "text": context},
                    {"type": "input_image", "image_url": image_url},
                ]},
            ],
        )
    except Exception as exc:
        raise RuntimeError("Vision provider is temporarily unavailable.") from exc

    raw = response.output_text.strip()
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("Vision provider returned an invalid assessment format.") from exc

    result = _safe_result(payload)
    result["image"] = {"content_type": normalized_type, "size_bytes": len(image_bytes)}
    result["crop_category"] = crop_category
    result["growth_stage"] = growth_stage
    result["status"] = "assessed"
    return result
