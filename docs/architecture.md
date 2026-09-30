# Architecture

## Runtime flow

~~~mermaid
flowchart LR
  Browser["Mobile PWA"] --> Auth["Firebase Auth / App Check"]
  Browser --> API["FastAPI / Render"]
  API --> Advisory["Advisory service"]
  API --> Vision["Vision service"]
  API --> Voice["Voice service"]
  API --> Weather["Weather service"]
  Advisory --> OpenAI["OpenAI"]
  Vision --> OpenAI
  Voice --> Sarvam["Sarvam AI"]
  Weather --> Provider["Weather provider"]
~~~

## Backend boundaries

- services/api: HTTP routing, request validation, middleware, and error translation.
- services/core: configuration, upload controls, and cross-cutting security primitives.
- services/advisory: advisory generation, localization, normalization, and safety.
- services/vision: image validation and structured crop assessment.
- services/voice: speech-to-text, text-to-speech, and multimodal voice orchestration.
- services/weather: weather-provider access and response normalization.
- services/farm: deterministic farm-intelligence helpers.

Third-party credentials never belong in the browser.

## Request lifecycle

1. FastAPI receives a request.
2. Structured fields are validated.
3. Rate limiting and upload bounds are applied.
4. A request trace ID is attached.
5. The service calls the minimum required third-party provider.
6. Provider failures are translated into safe HTTP responses.
7. The response is localized/normalized where required.
8. Logs contain metadata and trace IDs, not farmer content or secrets.

## Data boundaries

Farmer images, audio, text, and location are treated as untrusted and potentially sensitive. The API processes image/audio inputs in memory for provider calls and should not log their contents.

Firebase access is user-scoped. Firestore and Storage rules deny access unless the authenticated user owns the target path.

## PWA boundary

The current PWA contains a legacy single-page UI plus extracted shared modules under frontend/src. The extracted modules are the preferred place for new browser logic. Future work should progressively move the remaining inline application logic into focused modules without changing user-visible behavior.

## Production deployment

- FastAPI backend: Render.
- PWA: static hosting.
- Firebase: authentication and optional user data/storage.
- Provider APIs: OpenAI, Sarvam, and weather provider.

## Design principles

- Secure defaults.
- Explicit validation.
- Small, testable provider adapters.
- Provider failures must not expose raw exception details.
- Agricultural uncertainty must remain visible to users.
- External services are dependencies, not security controls.
