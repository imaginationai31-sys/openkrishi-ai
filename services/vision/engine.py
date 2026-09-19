"""Crop image assessment with a conservative Gemini vision provider."""

from __future__ import annotations

import json
from typing import Any

from services.gemini.client import get_gemini_client, get_model, output_text

SUPPORTED_CROPS = {"rice", "peanut", "vegetables", "flowers"}
SUPPORTED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_IMAGE_BYTES = 10 * 1024 * 1024

LANGUAGE_NAMES = {
    "en": "English",
    "bn": "Bengali",
    "hi": "Hindi",
    "ta": "Tamil",
    "pa": "Punjabi",
    "te": "Telugu",
}

VISION_RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "observations": {"type": "array", "items": {"type": "string"}, "description": "Short observations directly supported by the image."},
        "possible_causes": {"type": "array", "items": {"type": "string"}, "description": "Cautious possible causes; never definitive diagnoses."},
        "confidence": {"type": "string", "enum": ["low", "medium", "high"]},
        "uncertainties": {"type": "array", "items": {"type": "string"}},
        "recommendations": {"type": "array", "items": {"type": "string"}, "description": "Safe next-step checks only."},
    },
    "required": ["observations", "possible_causes", "confidence", "uncertainties", "recommendations"],
    "additionalProperties": False,
}


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


def _parse_assessment(raw: str) -> dict[str, Any]:
    text = (raw or "").strip()
    if text.startswith("```"):
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()
    try:
        payload = json.loads(text)
    except json.JSONDecodeError as exc:
        raise RuntimeError("Vision provider returned an invalid assessment format.") from exc
    if not isinstance(payload, dict):
        raise RuntimeError("Vision provider returned an invalid assessment format.")
    return payload


def assess_crop_image(
    image_bytes: bytes,
    content_type: str,
    crop_category: str | None = None,
    growth_stage: str | None = None,
    language: str = "en",
) -> dict[str, Any]:
    """Assess a crop image and generate all natural-language findings in the requested language."""
    normalized_type = _validate_image(image_bytes, content_type, crop_category)
    if language not in LANGUAGE_NAMES:
        raise ValueError("Unsupported language.")

    client = get_gemini_client()
    language_name = LANGUAGE_NAMES[language]
    context = f"Crop category: {crop_category or 'unknown'}; specific crop/variety: {crop_name or 'not specified'}; growth stage: {growth_stage or 'unknown'}."
    prompt = f"""You are a conservative agricultural image-assessment assistant.
Assess only what is visibly supported by the crop photo.
Do not claim a definitive disease, pest, nutrient deficiency, or treatment.
Do not recommend pesticide products, rates, or large fertilizer doses.
Return only a JSON object matching the supplied response schema.
IMPORTANT LANGUAGE RULE: Every natural-language value in observations, possible_causes, uncertainties, and recommendations MUST be written fully and naturally in {language_name}. Do not use English in those fields unless it is an unavoidable proper name or technical term. The confidence value must remain exactly one of low, medium, high.
If the image is unclear, explain that clearly in {language_name} and use low confidence.

Context:
{context}

Analyze the attached crop image. Return only the JSON object matching the response schema."""

    try:
        # Use generateContent for image input. The Gemini Python SDK supports raw
        # image bytes through Part.from_bytes; this avoids the invalid Interactions
        # API image-data shape that caused the generic provider-unavailable error.
        from google.genai import types

        response = client.models.generate_content(
            model=get_model(),
            contents=[
                types.Part.from_bytes(data=image_bytes, mime_type=normalized_type),
                prompt,
            ],
            config={
                "response_format": {
                    "text": {
                        "mime_type": "application/json",
                        "schema": VISION_RESPONSE_SCHEMA,
                    }
                }
            },
        )
    except Exception as exc:
        raise RuntimeError("Vision provider is temporarily unavailable.") from exc

    raw_text = getattr(response, "text", None) or output_text(response)
    payload = _parse_assessment(raw_text)
    result = _safe_result(payload)
    result["image"] = {"content_type": normalized_type, "size_bytes": len(image_bytes)}
    result["crop_category"] = crop_category
    result["growth_stage"] = growth_stage
    result["language"] = language
    result["status"] = "assessed"
    return result
