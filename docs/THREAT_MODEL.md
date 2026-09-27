# OpenKrishi AI — Threat Model

## Scope
Threats across the PWA, API, AI orchestration, storage, database, third-party providers, and operations.

## Assets
- user identities
- crop images and audio
- advisory history
- API credentials
- AI provider credentials
- database contents
- infrastructure configuration
- audit/security telemetry

## Key Threats
### Unauthorized Access
Mitigate with authentication, authorization, least privilege, secure tokens, and server-side ownership checks.

### IDOR / Cross-User Data Access
Every resource lookup must verify ownership or explicit authorization.

### Prompt Injection
Treat all user-controlled text and media as untrusted. Provider output cannot override system safety or access controls.

### Malicious Uploads
Validate size, type, content, processing libraries, and storage permissions. Avoid executing uploaded content.

### API Abuse
Use rate limits, quotas, anomaly detection, bounded payloads, and provider-side spending controls.

### Credential Leakage
Keep secrets in managed secret stores/environment configuration and scan repositories and CI logs.

### Supply-Chain Attack
Pin and regularly review dependencies, use lockfiles, scan vulnerabilities, and restrict CI permissions.

### Data Leakage
Minimize logs, encrypt transport/storage, restrict service accounts, and audit access.

### AI Hallucination
Require structured validation, uncertainty handling, safety classification, and human escalation for high-risk recommendations.

### Denial of Service
Use edge protections, request limits, timeouts, queue backpressure, and horizontal scaling.

## Trust Boundaries
1. User device ↔ public API
2. API ↔ AI providers
3. API/workers ↔ database
4. API/workers ↔ object storage
5. Application ↔ authentication provider
6. CI/CD ↔ production infrastructure

## Security Review
Threats should be reviewed before major features, especially new media processing, payments, privileged admin tools, or new external integrations.
