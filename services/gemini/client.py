"""Shared Gemini API client helpers.

This layer is intentionally small: API routes and domain services keep their
existing contracts while Gemini becomes the common multimodal provider.
"""

from __future__ import annotations

import os
from typing import Any


DEFAULT_MODEL = "gemini-3.8-flash"
TRANSCRIBE_MODEL = "gemini-3.5-transcribe"


def get_gemini_client() -> Any:
    """Return a configured Google GenAI client or fail clearly."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not configured. Set it before using Gemini.")

    from google import genai

    return genai.Client(api_key=api_key)


def get_model(default: str = DEFAULT_MODEL) -> str:
    return os.getenv("GEMINI_MODEL", default)


def get_transcribe_model() -> str:
    return os.getenv("GEMINI_TRANSCRIBE_MODEL", TRANSCRIBE_MODEL)


def output_text(interaction: Any) -> str:
    """Extract Gemini's final text while tolerating SDK response variations."""
    text = getattr(interaction, "output_text", None)
    if text is None:
        text = getattr(interaction, "outputText", None)
    return str(text or "").strip()
