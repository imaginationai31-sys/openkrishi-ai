import os
from unittest.mock import Mock

import httpx
import openai
import pytest


os.environ.setdefault("CORS_ALLOW_ORIGINS", "https://openkrishi-ai.hatchable.site")
os.environ.setdefault("GEMINI_API_KEY", "test-gemini-key")
os.environ.setdefault("OPENAI_API_KEY", "test-openai-key")
os.environ.setdefault("SARVAM_API_KEY", "test-sarvam-key")


def _fail_network(*args, **kwargs):
    raise AssertionError(
        "Unexpected external network call in tests. "
        "Mock the provider boundary explicitly."
    )


@pytest.fixture(autouse=True)
def block_external_providers(monkeypatch):
    """Prevent real HTTP and OpenAI calls during every test."""
    monkeypatch.setattr(httpx.Client, "request", _fail_network)
    monkeypatch.setattr(httpx.AsyncClient, "request", _fail_network)
    monkeypatch.setattr(
        openai,
        "OpenAI",
        Mock(
            side_effect=AssertionError(
                "Unexpected OpenAI client construction in tests. Mock openai.OpenAI explicitly."
            )
        ),
    )
