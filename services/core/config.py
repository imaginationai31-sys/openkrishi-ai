"""Centralized application configuration and secret loading."""
from __future__ import annotations

from functools import lru_cache

from pydantic import Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=True,
    )

    environment: str = Field(default="production", alias="ENVIRONMENT")
    cors_allow_origins: str = Field(alias="CORS_ALLOW_ORIGINS")

    openai_advisory_model: str = Field(default="gpt-5-mini", alias="OPENAI_ADVISORY_MODEL")
    openai_api_key: SecretStr = Field(alias="OPENAI_API_KEY")
    openai_vision_model: str = Field(default="gpt-5-mini", alias="OPENAI_VISION_MODEL")

    sarvam_api_key: SecretStr = Field(alias="SARVAM_API_KEY")
    sarvam_stt_model: str = Field(default="saaras:v4", alias="SARVAM_STT_MODEL")
    sarvam_tts_model: str = Field(default="bulbul:v3", alias="SARVAM_TTS_MODEL")

    open_meteo_url: str = Field(default="https://api.open-meteo.com/v1/forecast", alias="OPEN_METEO_URL")
    sarvam_stt_url: str = Field(default="https://api.sarvam.ai/speech-to-text", alias="SARVAM_STT_URL")
    sarvam_tts_url: str = Field(default="https://api.sarvam.ai/text-to-speech", alias="SARVAM_TTS_URL")

    max_image_bytes: int = Field(default=10 * 1024 * 1024, alias="MAX_IMAGE_BYTES", ge=1)
    max_audio_bytes: int = Field(default=10 * 1024 * 1024, alias="MAX_AUDIO_BYTES", ge=1)
    outbound_timeout_seconds: float = Field(default=45.0, alias="OUTBOUND_TIMEOUT_SECONDS", gt=0)
    weather_timeout_seconds: float = Field(default=15.0, alias="WEATHER_TIMEOUT_SECONDS", gt=0)
    ai_rate_limit: str = Field(default="10/minute", alias="AI_RATE_LIMIT")
    voice_rate_limit: str = Field(default="6/minute", alias="VOICE_RATE_LIMIT")
    vision_rate_limit: str = Field(default="6/minute", alias="VISION_RATE_LIMIT")

    @field_validator("cors_allow_origins")
    @classmethod
    def validate_cors_origins(cls, value: str) -> str:
        origins = [item.strip() for item in value.split(",") if item.strip()]
        if not origins or "*" in origins:
            raise ValueError("CORS_ALLOW_ORIGINS must contain explicit origins; wildcard '*' is not allowed.")
        return ",".join(origins)

    @property
    def allowed_origins(self) -> list[str]:
        return [item.strip() for item in self.cors_allow_origins.split(",") if item.strip()]


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
