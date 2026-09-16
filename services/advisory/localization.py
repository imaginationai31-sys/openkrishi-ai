"""Gemini-powered final localization for farmer-facing advisory text."""

from __future__ import annotations

import json
from typing import Any

from services.gemini.client import get_gemini_client, get_model, output_text

LANGUAGE_NAMES = {
    "en": "English",
    "bn": "Bengali",
    "hi": "Hindi",
    "ta": "Tamil",
    "pa": "Punjabi",
    "te": "Telugu",
}

LOCALIZATION_SCHEMA = {
    "type": "object",
    "properties": {
        "answer": {"type": "string"},
        "observations": {"type": "array", "items": {"type": "string"}},
        "recommendations": {"type": "array", "items": {"type": "string"}},
        "uncertainties": {"type": "array", "items": {"type": "string"}},
    },
    "required": ["answer", "observations", "recommendations", "uncertainties"],
    "additionalProperties": False,
}


def localize_advisory(advisory: dict[str, Any], language: str) -> dict[str, Any]:
    """Translate all farmer-facing advisory fields without changing meaning or safety."""
    if language == "en" or language not in LANGUAGE_NAMES:
        return advisory

    payload = {
        "answer": str(advisory.get("answer") or ""),
        "observations": [str(x) for x in advisory.get("observations", [])],
        "recommendations": [str(x) for x in advisory.get("recommendations", [])],
        "uncertainties": [str(x) for x in advisory.get("uncertainties", [])],
    }
    prompt = f"""Translate the following agricultural advisory into {LANGUAGE_NAMES[language]}.

STRICT RULES:
- Translate every string completely into the requested language.
- Do not leave English phrases untranslated.
- Preserve the exact meaning, ordering, and number of list items.
- Do not add, remove, or invent agronomic advice.
- Do not turn a possible cause into a diagnosis.
- Do not add pesticides, chemical names, fertilizer doses, or treatment instructions.
- Keep crop names and scientific terms natural for farmers in the requested language.
- Return only JSON matching the supplied schema.

Input JSON:
{json.dumps(payload, ensure_ascii=False)}"""

    try:
        client = get_gemini_client()
        interaction = client.interactions.create(
            model=get_model(),
            input=prompt,
            response_format={
                "type": "text",
                "mime_type": "application/json",
                "schema": LOCALIZATION_SCHEMA,
            },
        )
        translated = json.loads(output_text(interaction))
        if not isinstance(translated, dict):
            return advisory
        result = dict(advisory)
        result["answer"] = str(translated.get("answer", payload["answer"]))
        result["observations"] = [str(x) for x in translated.get("observations", payload["observations"])]
        result["recommendations"] = [str(x) for x in translated.get("recommendations", payload["recommendations"])]
        result["uncertainties"] = [str(x) for x in translated.get("uncertainties", payload["uncertainties"])]
        return result
    except Exception:
        # Keep the existing deterministic localization if Gemini localization is unavailable.
        return advisory


def localize_visual(visual: dict[str, Any], language: str) -> dict[str, Any]:
    """Translate Gemini Vision findings into the farmer's selected language."""
    if language == "en" or language not in LANGUAGE_NAMES:
        return visual

    payload = {
        "observations": [str(x) for x in visual.get("observations", [])],
        "possible_causes": [str(x) for x in visual.get("possible_causes", [])],
        "uncertainties": [str(x) for x in visual.get("uncertainties", [])],
        "recommendations": [str(x) for x in visual.get("recommendations", [])],
    }
    prompt = f"""Translate these crop-photo findings into {LANGUAGE_NAMES[language]}.
Translate every string completely. Preserve meaning, uncertainty, ordering, and item count.
Do not add diagnoses or treatments. Return only JSON matching the schema.

Input JSON:
{json.dumps(payload, ensure_ascii=False)}"""
    schema = {
        "type": "object",
        "properties": {
            "observations": {"type": "array", "items": {"type": "string"}},
            "possible_causes": {"type": "array", "items": {"type": "string"}},
            "uncertainties": {"type": "array", "items": {"type": "string"}},
            "recommendations": {"type": "array", "items": {"type": "string"}},
        },
        "required": ["observations", "possible_causes", "uncertainties", "recommendations"],
        "additionalProperties": False,
    }
    try:
        client = get_gemini_client()
        interaction = client.interactions.create(
            model=get_model(),
            input=prompt,
            response_format={"type": "text", "mime_type": "application/json", "schema": schema},
        )
        translated = json.loads(output_text(interaction))
        if not isinstance(translated, dict):
            return visual
        result = dict(visual)
        for key in payload:
            result[key] = [str(x) for x in translated.get(key, payload[key])]
        return result
    except Exception:
        return visual
