"""Optional production error tracking integration."""

from __future__ import annotations

import logging
from typing import Any


logger = logging.getLogger("openkrishi.error_tracking")


def configure_error_tracking(dsn: str | None, environment: str) -> bool:
    """Initialize Sentry when a DSN is configured and the SDK is installed."""
    if not dsn:
        return False

    try:
        import sentry_sdk
    except ImportError:
        logger.warning("Sentry DSN configured but sentry-sdk is not installed")
        return False

    sentry_sdk.init(
        dsn=dsn,
        environment=environment,
        send_default_pii=False,
        traces_sample_rate=0.0,
    )
    return True


def capture_exception(exc: BaseException) -> None:
    """Report an exception when the optional Sentry SDK is available."""
    try:
        import sentry_sdk
    except ImportError:
        return

    sentry_sdk.capture_exception(exc)


def _dsn_value(dsn: Any) -> str | None:
    """Normalize a settings SecretStr or plain string for callers."""
    if dsn is None:
        return None
    if hasattr(dsn, "get_secret_value"):
        return dsn.get_secret_value()
    return str(dsn)
