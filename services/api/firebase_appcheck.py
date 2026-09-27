import json
import os
from typing import Any

import firebase_admin
from firebase_admin import app_check, credentials


class AppCheckConfigurationError(RuntimeError):
    """Raised when App Check enforcement is enabled but Firebase Admin is unavailable."""


def is_enforced() -> bool:
    return os.getenv("FIREBASE_APPCHECK_ENFORCE", "false").strip().lower() in {"1", "true", "yes", "on"}


def _initialize_firebase_admin():
    try:
        return firebase_admin.get_app()
    except ValueError:
        pass

    raw_credentials = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
    try:
        if raw_credentials:
            service_account = json.loads(raw_credentials)
            return firebase_admin.initialize_app(credentials.Certificate(service_account))
        return firebase_admin.initialize_app()
    except Exception as exc:
        raise AppCheckConfigurationError(
            "Firebase Admin credentials are required when App Check enforcement is enabled."
        ) from exc


def verify_app_check_token(token: str) -> dict[str, Any]:
    if not token:
        raise ValueError("Missing Firebase App Check token.")

    firebase_app = _initialize_firebase_admin()
    return app_check.verify_token(token, app=firebase_app)
