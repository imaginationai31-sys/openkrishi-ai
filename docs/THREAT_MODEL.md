# OpenKrishi AI Threat Model

## Scope

This threat model covers the current OpenKrishi AI PWA + FastAPI/Render backend, Firebase data/storage controls, farmer uploads, location data, and third-party AI/weather/speech providers.

## System overview

~~~mermaid
flowchart LR
    Farmer["Indian Farmer"] --> PWA["Mobile-first PWA"]
    PWA --> API["FastAPI API on Render"]
    API --> AI["Third-party AI providers"]
    API --> Weather["Weather API"]
    API --> Speech["Speech-to-text / Text-to-speech"]
    PWA --> Firebase["Firebase Auth / Firestore / Storage"]
    AI --> API
    Weather --> API
    Speech --> API
~~~

## Assets

- Farmer-provided crop photos.
- Farmer voice recordings and transcriptions.
- Location coordinates or location text.
- Selected language, crop and growth-stage information.
- Diagnosis/advisory history stored by the client/Firebase where enabled.
- API credentials for Gemini, OpenAI and Sarvam.
- Firebase authentication/data/storage.
- API availability and third-party provider availability.
- Advisory integrity and farmer safety.

## Trust boundaries and entry points

| Entry point / boundary | Data crossing it | Main concern |
|---|---|---|
| PWA -> API | Text, location, image, audio | Untrusted input, abuse, privacy |
| PWA -> Firebase | Authenticated user data and images | Authorization and storage rules |
| API -> AI providers | Farmer prompts and crop images | Privacy, prompt injection, provider exposure |
| API -> weather provider | Coordinates | Location privacy and outbound failures |
| API -> speech providers | Audio and language | Privacy, upload abuse, provider exposure |
| PWA local storage | UI/history/cache data | Device privacy |
| Render environment | API secrets | Secret exposure and compromise |
| Git repository/history | Source and configuration | Credential leakage |

## STRIDE threat table

| Threat | Affected component | Likelihood | Impact | Existing mitigation | Remaining gap |
|---|---|---:|---:|---|---|
| Leaked API keys | Render/API, Git | Medium | High | Central Pydantic settings, .env ignored, Render sync false, Gitleaks CI | Enable GitHub secret scanning/push protection; rotate any historical real secret |
| Abuse / AI cost exhaustion | AI, voice, vision endpoints | High | High | SlowAPI per-IP limits, upload limits | Add authenticated quotas, provider budgets and abuse analytics |
| Malicious/oversized uploads | Vision/voice API | High | Medium/High | MIME + extension checks, 10 MB limits, bounded reads | Add content-signature validation and malware scanning if stored |
| Prompt injection via text/image | AI providers | Medium | High | Conservative system prompts, structured output, safety layer | Add explicit prompt-injection handling and adversarial tests |
| Unsafe agronomy advice | Advisory/vision | Medium | High | Conservative confidence, no pesticide rates, uncertainty and safety fields | Human agronomist review, source-backed recommendations and stronger evaluation set |
| Location privacy | Weather/advisory | Medium | High | Location is optional; outbound weather call uses only required coordinates | Minimize retention, disclose provider sharing and add deletion controls |
| Voice privacy | Speech providers | Medium | High | Audio is processed through backend; no intentional API logging of content | Document provider retention terms and define application retention/deletion policy |
| Open Firebase rules | Firestore/Storage | Low/Medium | High | Deny-by-default, UID-scoped access, upload type/size checks | Add automated Firebase rules tests and least-privilege field validation |
| CORS misconfiguration | API | Medium | Medium/High | Explicit configured origins; wildcard rejected | Keep production origin list minimal and test deployment configuration |
| Dependency vulnerabilities / supply chain | Python/npm/GitHub Actions | Medium | High | Dependency files and CI checks | Add Dependabot/Renovate, lockfiles and dependency/SBOM scanning |
| Render free-tier denial of service | API | High | Medium/High | Rate limits, bounded uploads, outbound timeouts | CDN/WAF, authenticated quotas, autoscaling or paid capacity |
| Error/log leakage | API/logs | Medium | Medium/High | Generic 500 response, request IDs, no secret logging | Central log redaction and privacy review |
| Credential abuse | Authentication | Medium | Medium | Firebase authentication controls | Add abuse monitoring and stronger account protections as account login expands |

## Implemented mitigations

- Centralized secret/config loading with pydantic-settings.
- Secrets represented as SecretStr and never intentionally logged.
- Render secret variables use sync: false.
- Explicit CORS origins; wildcard production configuration is rejected.
- Rate limiting on advisory, vision and voice endpoints.
- Image/audio upload size, MIME type and extension validation.
- Bounded upload reads rather than unbounded request-body reads.
- Outbound HTTP timeouts.
- Generic internal-error responses.
- Request trace IDs without farmer content.
- Firebase Firestore deny-by-default fallback.
- Firebase Storage UID scoping and image MIME/size restrictions.
- Gitleaks push/PR scanning with full Git history checkout.

## Data handling

### Collected

Depending on the feature used:

- Crop images.
- Voice recordings and resulting transcription.
- Farmer questions.
- Crop/language/growth-stage selections.
- Approximate or precise device location when the farmer grants it.
- Diagnosis/advisory history when the client saves it.

### Why

- Crop analysis requires the image.
- Voice features require audio.
- Weather requires coordinates.
- Agricultural recommendations use crop and context.
- History requires storing previous results.

### Retention

The current API processes image/audio data in memory for provider calls and does not intentionally persist uploads itself. The PWA/Firebase layer may persist diagnosis history and images when that feature is enabled. Third-party providers may have their own retention policies.

A formal application-wide retention period is not yet enforced.

### Deletion

Firebase user-scoped data can be deleted subject to the application's UI and Firebase controls. The API currently has no general user-data deletion endpoint.

Priority gap: implement an explicit data-export/deletion flow and a documented retention period before broad public launch.

## Incident response: leaked secret

1. Identify which provider and credential were exposed.
2. Immediately rotate/revoke the credential at the provider.
3. Replace the Render environment variable with the new secret.
4. Search current code and Git history for the old credential.
5. Purge the credential from Git history when appropriate.
6. Audit provider and Render logs for suspicious use.
7. Check billing and usage for unexpected activity.
8. Review GitHub Actions and repository access.
9. Notify affected stakeholders when required.
10. Document the incident and corrective action.

Deleting a secret from the latest source file is not sufficient if the real credential existed in Git history.

## Prioritized remaining work

### P0
- Rotate any credential that Gitleaks or GitHub secret scanning identifies in history.
- Enable GitHub secret scanning and push protection.
- Set all required Render environment variables.
- Establish provider budgets and usage alerts.
- Define application retention/deletion policy.

### P1
- Add Firebase rules emulator tests.
- Add authenticated user quotas in addition to IP-based rate limits.
- Add adversarial prompt-injection and unsafe-agronomy evaluation tests.
- Add dependency vulnerability/SBOM scanning.
- Add provider privacy/retention disclosures.

### P2
- Add malware/content-signature scanning for persisted uploads.
- Add WAF/CDN protection and stronger production observability.
- Add formal agronomist review and periodic model evaluation.
