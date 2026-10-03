import json
import logging
import sys

from services.core.logging import JsonFormatter


def test_json_formatter_includes_structured_request_fields():
    record = logging.LogRecord(
        name="openkrishi.api",
        level=logging.INFO,
        pathname=__file__,
        lineno=1,
        msg="request_complete",
        args=(),
        exc_info=None,
    )
    record.trace_id = "trace-1234"
    record.method = "GET"
    record.path = "/api/v1/health"
    record.status_code = 200
    record.duration_ms = 12.3

    payload = json.loads(JsonFormatter().format(record))

    assert payload["message"] == "request_complete"
    assert payload["trace_id"] == "trace-1234"
    assert payload["method"] == "GET"
    assert payload["path"] == "/api/v1/health"
    assert payload["status_code"] == 200
    assert payload["duration_ms"] == 12.3


def test_json_formatter_includes_exception_and_error_type():
    try:
        raise ValueError("bad input")
    except ValueError:
        record = logging.LogRecord(
            name="openkrishi.api",
            level=logging.ERROR,
            pathname=__file__,
            lineno=1,
            msg="unhandled_request_error",
            args=(),
            exc_info=sys.exc_info(),
        )
        record.trace_id = "trace-5678"
        record.error_type = "ValueError"

    payload = json.loads(JsonFormatter().format(record))

    assert payload["trace_id"] == "trace-5678"
    assert payload["error_type"] == "ValueError"
    assert "ValueError: bad input" in payload["exception"]
