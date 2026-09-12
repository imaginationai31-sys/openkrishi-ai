"""Safety helpers for agricultural advisory responses."""

from typing import Any


def build_safety(status: str = "caution", reason: str = "") -> dict[str, Any]:
    return {"status": status, "reason": reason}


def low_confidence_safety() -> dict[str, Any]:
    return build_safety(
        "caution",
        "This MVP uses limited rule-based guidance. Confirm important farm decisions with a qualified local agronomist.",
    )
