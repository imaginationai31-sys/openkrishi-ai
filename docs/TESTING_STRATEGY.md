# OpenKrishi AI — Testing Strategy

## Test Pyramid
- unit tests for business logic and validators
- API tests for contracts and authorization
- integration tests for database/storage/provider adapters
- end-to-end tests for critical user journeys
- load tests for capacity
- security tests for abuse and isolation
- AI evaluation tests for quality and safety

## Critical Advisory Tests
Maintain coverage for:
- rice + yellow leaves
- rice + yellow leaves + tillering
- wilting
- leaf spots
- missing growth stage

## API Contract Tests
Validate request/response schemas, supported languages, crop categories, error envelopes, authentication, authorization, rate limits, and OpenAPI generation.

## AI Evaluation
Maintain deterministic fixtures where possible and scored evaluation sets for:
- factuality
- uncertainty handling
- language consistency
- safety
- hallucination rate
- prompt-injection resistance

## Security Tests
Test:
- IDOR/authz failures
- malformed uploads
- oversized payloads
- malicious filenames/content
- secret leakage
- CORS behavior
- rate-limit bypasses
- prompt injection
- dependency vulnerabilities

## Load Testing
Test normal and peak traffic separately. Measure p50/p95/p99 latency, error rate, queue depth, database saturation, and provider throttling.

## CI Gates
Pull requests should run formatting/linting, unit/API tests, dependency/security checks, OpenAPI validation, and relevant AI safety fixtures.

## Release Strategy
Use staging validation and progressive rollout where infrastructure supports it. Keep rollback procedures documented and tested.
