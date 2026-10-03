from services.core.metrics import RequestMetrics


def test_metrics_record_requests_and_errors():
    metrics = RequestMetrics()

    metrics.record(0.25, 200)
    metrics.record(0.5, 503)

    assert metrics.snapshot() == (2, 1, 0.75)


def test_metrics_prometheus_output():
    metrics = RequestMetrics()
    metrics.record(0.125, 200)

    output = metrics.prometheus()

    assert "openkrishi_http_requests_total 1" in output
    assert "openkrishi_http_errors_total 0" in output
    assert "openkrishi_http_request_duration_seconds_total 0.125000" in output
