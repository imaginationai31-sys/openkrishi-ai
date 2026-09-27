import logging

from fastapi import HTTPException, Request

from services.api.firebase_appcheck import AppCheckConfigurationError, is_enforced, verify_app_check_token

logger = logging.getLogger("openkrishi.api.app_check")

EXCLUDED_PATHS = {"/api/v1/health"}


async def require_app_check(request: Request) -> None:
    """Require a valid Firebase App Check token when production enforcement is enabled."""
    if request.method == "OPTIONS" or request.url.path in EXCLUDED_PATHS:
        return
    if not request.url.path.startswith("/api/v1/"):
        return
    if not is_enforced():
        return

    token = request.headers.get("X-Firebase-AppCheck", "").strip()
    try:
        claims = verify_app_check_token(token)
        request.state.firebase_app_check = claims
    except AppCheckConfigurationError as exc:
        logger.error("Firebase App Check is enforced but not configured: %s", exc)
        raise HTTPException(status_code=503, detail="API protection is temporarily unavailable.") from exc
    except Exception as exc:
        logger.warning(
            "Rejected request with invalid Firebase App Check token path=%s error=%s",
            request.url.path,
            type(exc).__name__,
        )
        raise HTTPException(status_code=401, detail="Valid Firebase App Check token required.") from exc
