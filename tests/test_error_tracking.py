import sys
from types import SimpleNamespace

from services.core.error_tracking import capture_exception, configure_error_tracking


def test_error_tracking_is_disabled_without_dsn():
    assert configure_error_tracking(None, "test") is False


def test_error_tracking_initializes_sentry(monkeypatch):
    calls = []
    fake = SimpleNamespace(init=lambda **kwargs: calls.append(kwargs))
    monkeypatch.setitem(sys.modules, "sentry_sdk", fake)

    assert configure_error_tracking("https://example@sentry.invalid/1", "test") is True
    assert calls == [
        {
            "dsn": "https://example@sentry.invalid/1",
            "environment": "test",
            "send_default_pii": False,
            "traces_sample_rate": 0.0,
        }
    ]


def test_error_tracking_captures_exception(monkeypatch):
    captured = []
    fake = SimpleNamespace(capture_exception=captured.append)
    monkeypatch.setitem(sys.modules, "sentry_sdk", fake)

    error = ValueError("test")
    capture_exception(error)

    assert captured == [error]
