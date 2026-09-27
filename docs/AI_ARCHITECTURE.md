# OpenKrishi AI — AI Architecture

## Purpose
Define a provider-independent AI orchestration layer for advisory, vision, voice, and future agronomy capabilities.

## Principles
- AI providers are implementation details behind stable interfaces.
- The API never exposes provider credentials to clients.
- AI output is treated as untrusted until validated.
- Safety checks run before actionable recommendations reach users.
- Structured outputs are preferred over free-form parsing.
- Every AI request is traceable with a request ID and model/provider metadata.
- Provider failures must degrade gracefully.

## Logical Flow
Client → API → Auth/Rate Limit → AI Orchestrator → Context Builder → Provider Adapter → Output Validator → Safety Engine → Response.

## Provider Abstraction
Define interfaces such as:
- generate_advisory()
- analyze_crop_image()
- transcribe_audio()
- synthesize_speech()

Adapters may include Gemini, OpenAI, or future providers without changing API contracts.

## Context Builder
Build a bounded context from:
- user language
- crop/category
- growth stage
- farmer question
- image observations
- weather context
- relevant conversation history

Do not send unnecessary personal data or unrestricted historical conversations.

## Structured AI Contract
Internal AI responses should use validated schemas containing:
- observation
- possible_causes
- recommendations
- confidence
- safety_status
- follow_up_questions
- limitations

Reject malformed output rather than silently guessing.

## Model Routing
Use a policy-driven router based on task, latency, cost, availability, and required capability. Do not hard-code one provider into business logic.

## Reliability
Apply bounded timeouts, limited retries, circuit breaking where appropriate, and provider fallback only when the fallback preserves safety and output requirements.

## Caching
Cache only safe, non-user-specific results. Never cache sensitive prompts, private images, or personalized recommendations in a shared cache without explicit isolation.

## Observability
Record provider, model, latency, token/cost metadata where available, validation result, safety status, and error category. Never log raw secrets or unnecessary personal content.

## Future Extensions
The architecture can later support retrieval-augmented agronomy knowledge, specialist review, multimodal models, local/edge models, and domain-specific evaluation without changing client contracts.
