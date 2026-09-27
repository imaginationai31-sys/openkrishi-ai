# OpenKrishi AI — Observability

## Goals
Detect failures quickly, explain incidents, and measure user-visible reliability without collecting unnecessary personal data.

## Signals
### Logs
Structured JSON logs with timestamp, level, request ID, endpoint, status, latency, provider, model, and error category.

### Metrics
Track:
- request rate
- error rate
- p50/p95/p99 latency
- AI provider latency
- token/cost metadata where available
- queue depth
- database pool usage
- cache hit rate
- upload failures
- safety-state distribution

### Traces
Propagate a request/correlation ID across API, worker, database, storage, and AI provider calls.

## Privacy
Never log passwords, access tokens, provider keys, raw uploaded images, full private prompts, or unnecessary personal identifiers.

## Alerts
Alert on sustained:
- elevated 5xx
- latency degradation
- provider failure
- queue backlog
- database saturation
- authentication anomalies
- unusual abuse patterns

## SLO Direction
Start with measurable availability and latency targets, then refine them using real production traffic rather than arbitrary promises.

## Incident Response
Every production incident should have an owner, timeline, impact assessment, mitigation, root-cause analysis, and follow-up actions.
