"""Shared request-rate limiting configuration."""

from slowapi import Limiter
from slowapi.util import get_remote_address

from services.core.config import get_settings


limiter = Limiter(key_func=get_remote_address, headers_enabled=True)

AI_LIMIT = get_settings().ai_rate_limit
VOICE_LIMIT = get_settings().voice_rate_limit
VISION_LIMIT = get_settings().vision_rate_limit
