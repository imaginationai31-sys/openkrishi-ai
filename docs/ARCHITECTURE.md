# OpenKrishi AI — Production Architecture

**Status:** Architecture Baseline  
**Version:** 1.0

## 1. Architectural principles

1. API-first.
2. Stateless application services.
3. Mobile/PWA clients consume the same versioned backend.
4. AI providers are isolated behind server-side adapters.
5. Long-running work is asynchronous.
6. Shared state lives in managed stores.
7. Security is enforced at multiple layers.
8. Observability is built into every service.
9. Provider failures must degrade gracefully.
10. Architecture must scale horizontally.

## 2. Logical architecture

```
Users
  |
  +--> PWA
  |
  +--> Android
          |
          v
     CDN / WAF / Edge
          |
          v
     API Gateway
          |
          +--> AuthN/AuthZ
          +--> Rate Limiting
          +--> Request Validation
          |
          v
   Stateless API Services
      |       |       |       |
      v       v       v       v
 Advisory  Vision   Voice  Weather
      \       |       |       /
       \      v       v      /
        +-- AI Orchestrator --+
                  |
          +-------+-------+
          |       |       |
          v       v       v
        LLM    Vision    STT/TTS
          |
          v
      Safety Layer
          |
   +------+-------+--------+
   |              |        |
   v              v        v
PostgreSQL      Redis    Object Storage
   |
   v
Analytics / Audit
```

## 3. Current-to-target migration

The current FastAPI application is retained as the initial application boundary. Existing API routes under `/api/v1` remain stable while internal implementation is refactored.

Current routes:
- `/api/v1/advisory`
- `/api/v1/vision`
- `/api/v1/voice`
- `/api/v1/weather`
- `/api/v1/health`

Target service boundaries may remain within one deployable application initially. They should become independently scalable only when traffic or operational needs justify it. This avoids premature microservices complexity.

## 4. API layer

FastAPI remains the API framework.

Responsibilities:
- Request validation.
- Authentication token verification.
- Authorization.
- Rate limiting.
- Structured errors.
- Request IDs.
- CORS policy.
- Security headers.
- API versioning.
- OpenAPI documentation.

## 5. AI orchestration

The application must not couple business logic directly to a single model provider.

Recommended abstraction:

```
AIProvider
  +-- GeminiProvider
  +-- OpenAIProvider
  +-- FutureProvider
```

The orchestrator decides which capability is required and applies policy before and after model execution.

## 6. Data layer

### PostgreSQL
Primary source for structured transactional data:
- users
- profiles
- subscriptions
- usage
- diagnosis records
- advisory records
- feedback
- configuration
- audit metadata

### Redis/Valkey
For:
- rate limits
- caching
- short-lived state
- idempotency
- queue support where appropriate

### Object storage
For:
- crop images
- audio assets
- generated media
- other large binary objects

### Firebase
Existing Firebase capabilities may remain where useful for client authentication/realtime UX, but critical backend data ownership must be explicit and not duplicated without a defined source of truth.

## 7. Asynchronous processing

Use background workers for:
- image processing
- vision inference
- speech transcription
- text-to-speech
- alert generation
- notifications
- large media processing

Pattern:

```
Client -> API -> Job Queue -> Worker -> Database/Storage
                         |
                         +-> status/result
```

## 8. Caching

Cache stable or expensive data such as:
- weather responses
- crop metadata
- language configuration
- selected knowledge context

Cache entries must have explicit TTLs and invalidation rules.

## 9. Storage security

Uploaded images/audio must:
- be size-limited;
- have validated MIME/content;
- receive generated object names;
- avoid user-controlled executable paths;
- be protected by access rules;
- use signed/private access where applicable;
- have defined retention/deletion policies.

## 10. Observability

Every request should carry a trace/request ID.

Metrics:
- request count
- latency
- error rate
- provider latency
- provider failures
- worker queue depth
- cache hit rate
- database latency
- storage errors

Logs must avoid unnecessary sensitive user content.

## 11. Scaling model

The first production deployment can remain a modular monolith:

```
Load Balancer
     |
API instance x N
     |
Managed PostgreSQL
Managed Redis/Valkey
Object Storage
Worker x N
```

This provides horizontal scaling without requiring microservices on day one.

## 12. Reliability

Every external dependency should have:
- timeout
- bounded retry
- exponential backoff where appropriate
- circuit-breaking/failure isolation where appropriate
- fallback behavior
- observable failure state

Never retry unsafe or non-idempotent operations blindly.

## 13. Deployment environments

Maintain separate:
- development
- staging
- production

Production secrets must never be committed to Git.

## 14. Architecture evolution

Scale vertically first where economical, then horizontally. Split services only when independent scaling, isolation, ownership or deployment frequency provides a measurable benefit.
