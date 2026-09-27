# OpenKrishi AI — Security Architecture

**Status:** Security Baseline  
**Version:** 1.0

## 1. Security objectives

OpenKrishi must protect:
- user accounts
- farmer-provided images
- voice data
- location data
- agricultural history
- API credentials
- infrastructure
- AI provider credentials
- operational logs

Security must protect confidentiality, integrity, availability and user control.

## 2. Trust boundaries

```
Untrusted Client
      |
      v
Edge / WAF
      |
      v
API Boundary
      |
      +--> Authentication / Authorization
      |
      v
Application Services
      |
      +--> AI Providers
      +--> Database
      +--> Cache
      +--> Object Storage
      +--> Queue
```

Anything supplied by a client is untrusted until validated.

## 3. Authentication

Use a managed identity provider such as Firebase Authentication initially.

Backend requirements:
- Verify tokens server-side.
- Never trust a client-supplied user ID.
- Derive identity from verified credentials.
- Reject expired/invalid tokens.
- Separate anonymous and authenticated capabilities.
- Support account deletion.

## 4. Authorization

Every protected resource must be authorized against the authenticated principal.

Rules:
- User A cannot read User B's diagnosis history.
- User A cannot access User B's images.
- Administrative endpoints require explicit elevated roles.
- Object-storage access must not rely only on hidden URLs.
- Server-side authorization is mandatory even when client UI hides controls.

## 5. API security

Required controls:
- HTTPS only.
- Strict CORS allowlist in production.
- Request body size limits.
- Image/audio size limits.
- MIME/content validation.
- Schema validation.
- Rate limiting.
- Request timeouts.
- Structured error responses.
- Security headers.
- API versioning.
- Abuse monitoring.

The current permissive CORS default must not be used as the production security posture.

## 6. Secrets management

AI provider keys, database passwords, signing keys and service credentials must:
- live in managed environment secrets;
- never be committed to Git;
- never be embedded in the PWA;
- never be embedded in Android source;
- never be logged;
- be rotated periodically;
- use separate development/staging/production values.

## 7. AI security

AI requests pass through a controlled orchestration layer.

```
Validated user input
      |
      v
Prompt/context builder
      |
      v
Model provider
      |
      v
Structured output validator
      |
      v
Safety policy validator
      |
      v
Localized response
```

User input must never be treated as trusted system instructions.

## 8. Prompt injection defenses

Controls:
- separate system instructions from user content;
- constrain model output with schemas;
- validate tool calls;
- never expose privileged prompts;
- never allow user text to directly select privileged tools;
- reject unexpected structured output;
- test adversarial inputs continuously.

## 9. AI safety

The model is an advisory component, not the final authority.

Responses should expose:
- confidence where meaningful;
- uncertainty;
- observation versus inference;
- recommendation risk;
- escalation/confirmation requirements.

High-risk agricultural actions must be constrained by application policy.

## 10. File upload security

For every uploaded image/audio file:
1. Enforce maximum size.
2. Validate declared MIME type.
3. Validate actual file signature/content.
4. Generate server-side object names.
5. Store outside executable application paths.
6. Restrict access.
7. Scan/process safely where appropriate.
8. Apply retention rules.
9. Delete when retention expires or the user requests deletion.

## 11. Database security

- TLS for remote database connections.
- Least-privilege database accounts.
- Parameterized queries/ORM.
- Migration control.
- Backups.
- Restore testing.
- No credentials in source code.
- Audit sensitive administrative operations.

## 12. Logging

Never log:
- API keys
- passwords
- authentication tokens
- full private images
- raw voice recordings
- unnecessary personal data

Logs should include safe operational identifiers such as request/trace IDs and error categories.

## 13. Rate limiting

Rate limits should be applied per:
- IP where appropriate;
- authenticated user;
- endpoint;
- expensive AI capability.

AI/vision/voice endpoints should have stricter limits than health endpoints.

Rate limiting must return a clear retry response and must not expose internal infrastructure details.

## 14. Abuse prevention

Monitor:
- repeated failed authentication;
- unusually high request rates;
- oversized uploads;
- repeated AI requests;
- automated scraping;
- suspicious account creation;
- prompt-injection attempts;
- anomalous provider usage.

## 15. Dependency and supply-chain security

CI should include:
- dependency vulnerability scanning;
- secret scanning;
- static analysis;
- lockfile/dependency review;
- automated test execution.

Dependencies must be reviewed before major upgrades.

## 16. Mobile/PWA security

Client applications must assume their code is observable.

Therefore:
- no provider API secrets in client code;
- no privileged business logic only enforced client-side;
- secure token storage;
- HTTPS;
- safe file handling;
- restrictive content security policy where applicable.

## 17. Incident response

Security incidents should follow:

```
Detect
  -> Contain
  -> Investigate
  -> Rotate/Revoke
  -> Recover
  -> Notify where required
  -> Post-incident review
```

Maintain an incident record with timeline, affected systems, evidence, remediation and follow-up actions.

## 18. Backup and recovery

Critical data must have automated backups with:
- defined retention;
- encryption;
- access controls;
- restore testing;
- documented recovery procedure.

Initial recovery targets:
- RPO: <= 24 hours
- RTO: <= 4 hours

Targets may be tightened as the platform matures.

## 19. Security review gates

Before production:
- authentication tested;
- authorization tested;
- CORS restricted;
- rate limiting active;
- secrets verified outside Git;
- file validation active;
- AI output validation active;
- dependency scanning active;
- backups verified;
- monitoring active;
- incident procedure documented.

## 20. Security principle

OpenKrishi should follow defense in depth. No single model, client control, API gateway, database rule or provider should be treated as the only security boundary.
