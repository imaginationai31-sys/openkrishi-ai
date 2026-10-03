import threading


class RequestMetrics:
    """Thread-safe in-process request metrics for operational monitoring."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._requests_total = 0
        self._errors_total = 0
        self._duration_seconds_total = 0.0

    def record(self, duration_seconds: float, status_code: int) -> None:
        with self._lock:
            self._requests_total += 1
            self._duration_seconds_total += duration_seconds
            if status_code >= 500:
                self._errors_total += 1

    def snapshot(self) -> tuple[int, int, float]:
        with self._lock:
            return (
                self._requests_total,
                self._errors_total,
                self._duration_seconds_total,
            )

    def prometheus(self) -> str:
        requests_total, errors_total, duration_seconds_total = self.snapshot()
        return (
            "# HELP openkrishi_http_requests_total Total HTTP requests handled.\n"
            "# TYPE openkrishi_http_requests_total counter\n"
            f"openkrishi_http_requests_total {requests_total}\n"
            "# HELP openkrishi_http_errors_total Total HTTP 5xx responses.\n"
            "# TYPE openkrishi_http_errors_total counter\n"
            f"openkrishi_http_errors_total {errors_total}\n"
            "# HELP openkrishi_http_request_duration_seconds_total Total request duration.\n"
            "# TYPE openkrishi_http_request_duration_seconds_total counter\n"
            f"openkrishi_http_request_duration_seconds_total {duration_seconds_total:.6f}\n"
        )


metrics = RequestMetrics()
