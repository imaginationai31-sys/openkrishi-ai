"""Safety helpers for agricultural advisory responses."""

from typing import Any


UNSAFE_ACTION_TERMS = (
    "pesticide",
    "insecticide",
    "fungicide",
    "herbicide",
    "chemical spray",
    "spray dose",
    "large fertilizer dose",
    "double dose",
)

CAUTION_TERMS = (
    "disease",
    "pest",
    "deficiency",
    "fungus",
    "virus",
    "bacteria",
)


def build_safety(status: str = "caution", reason: str = "") -> dict[str, Any]:
    return {"status": status, "reason": reason}


def low_confidence_safety() -> dict[str, Any]:
    return build_safety(
        "caution",
        "This MVP uses limited rule-based guidance. Confirm important farm decisions with a qualified local agronomist.",
    )


def enforce_advisory_safety(
    recommendations: list[str],
    uncertainties: list[str],
    confidence: str = "low",
) -> tuple[list[str], list[str], dict[str, Any]]:
    """Remove unsafe treatment instructions and downgrade uncertain outputs."""
    safe_recommendations: list[str] = []
    blocked = False

    for recommendation in recommendations:
        text = str(recommendation).strip()
        lowered = text.lower()
        if any(term in lowered for term in UNSAFE_ACTION_TERMS):
            blocked = True
            continue
        safe_recommendations.append(text)

    safe_uncertainties = [str(item) for item in uncertainties]
    if blocked:
        safe_uncertainties.append(
            "A potentially unsafe treatment instruction was removed; do not apply chemical or large-dose treatment based on this advisory alone."
        )

    normalized_confidence = confidence if confidence in {"low", "medium", "high"} else "low"
    if normalized_confidence != "low" and any(
        term in " ".join(safe_recommendations).lower() for term in CAUTION_TERMS
    ):
        normalized_confidence = "low"
        safe_uncertainties.append(
            "Potential disease, pest, or deficiency wording requires confirmation before action."
        )

    status = "caution" if normalized_confidence == "low" or blocked else "caution"
    reason = (
        "Recommendations are limited to low-risk checks. Confirm diagnosis and treatment with a qualified local agronomist."
    )
    return safe_recommendations, safe_uncertainties, build_safety(status, reason)
