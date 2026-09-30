"""Compatibility helpers for farmer-facing advisory localization.

OpenKrishi AI providers already return the requested language, so these helpers
intentionally avoid a second translation provider and preserve the provider output.
"""

from typing import Any

LANGUAGE_NAMES = {
    "en": "English",
    "bn": "Bengali",
    "hi": "Hindi",
    "ta": "Tamil",
    "pa": "Punjabi",
    "te": "Telugu",
}


def localize_advisory(advisory: dict[str, Any], language: str) -> dict[str, Any]:
    """Return advisory output unchanged when it is already in the requested language."""
    return advisory


def localize_visual(visual: dict[str, Any], language: str) -> dict[str, Any]:
    """Return vision output unchanged when it is already in the requested language."""
    return visual
