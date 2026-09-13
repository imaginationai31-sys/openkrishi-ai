"""Supported voice-language metadata for OpenKrishi AI."""

from dataclasses import dataclass


@dataclass(frozen=True)
class VoiceLanguage:
    code: str
    name: str


SUPPORTED_VOICE_LANGUAGES = (
    VoiceLanguage("bn", "Bengali"),
    VoiceLanguage("hi", "Hindi"),
    VoiceLanguage("ta", "Tamil"),
    VoiceLanguage("pa", "Punjabi"),
    VoiceLanguage("te", "Telugu"),
)

SUPPORTED_LANGUAGE_CODES = frozenset(
    language.code for language in SUPPORTED_VOICE_LANGUAGES
)


def is_supported_language(language: str) -> bool:
    """Return True when the language code is supported by the voice MVP."""
    return language in SUPPORTED_LANGUAGE_CODES
