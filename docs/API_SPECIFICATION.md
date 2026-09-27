# OpenKrishi AI — API Specification

## Contract
The public API remains versioned under /api/v1.

## Core Endpoints
- POST /api/v1/advisory
- POST /api/v1/vision
- POST /api/v1/voice/*
- GET /api/v1/weather/*
- health/readiness endpoints

Exact endpoint schemas are defined by the application's OpenAPI contract.

## Request Standards
Requests should include validated:
- language
- crop/category
- question or task input
- optional growth stage
- optional media references

Reject unsupported enum values and malformed payloads with consistent 4xx responses.

## Response Standards
Responses should include:
- stable machine-readable fields
- user-facing answer
- confidence where applicable
- safety status where applicable
- request ID for support/debugging

## Error Contract
Use consistent error envelopes with:
- error code
- human-readable message
- request ID
- optional field-level validation details

Do not expose stack traces, provider secrets, internal prompts, or infrastructure details.

## Authentication
Protected endpoints require a valid user identity token. Authorization must be enforced server-side.

## Rate Limits
Apply per-user and per-IP controls with endpoint-specific limits. Expensive AI endpoints should have stricter quotas than health checks.

## Idempotency
Support idempotency keys for expensive or asynchronous operations where duplicate submissions could create cost or duplicate jobs.

## File Uploads
Prefer signed object-storage upload flows for large files. Validate size, MIME type, extension, content, and ownership.

## Compatibility
Do not break existing clients without a versioning/deprecation plan. Additive changes are preferred.

## OpenAPI
The generated FastAPI OpenAPI document is the canonical machine-readable API contract and should be validated in CI.
