# Score Readiness

Updated during the repository quality pass.

| Category | State |
|---|---|
| Architecture | Partial: PWA/API boundary documented; large legacy files remain |
| Tests | Partial: backend endpoint tests exist; 80% backend coverage is not yet enforced |
| Security | Strong baseline: centralized settings, limits, CORS, request IDs, Gitleaks, CodeQL, Firebase rules |
| Docs | Strong: README, architecture, API, deployment, test guide, changelog, contribution guide, roadmap |
| Cleanliness | Partial: Ruff tooling and provider cleanup added; large files remain |
| Dependencies | Strong baseline: uv.lock, package-lock, Dependabot, dependency audits |
| CI/CD | Partial until the latest queued workflows are green |
| History | Partial: Conventional Commit guidance added; PR review and v0.1.0 release remain |

## Completed

- Python dependency pinning and uv.lock.
- Makefile development commands.
- Multi-stage non-root backend Docker image.
- Non-root PWA Docker image and docker-compose stack.
- Devcontainer configuration.
- Frontend npm package, package-lock, Vitest, ESLint, Prettier, and TypeScript tooling.
- Integrated PWA API/language modules with real unit tests.
- Frontend coverage threshold configuration.
- Backend coverage reporting.
- Dependency audits, Dependabot, secret scanning, CodeQL, Docker builds, and fresh-clone verification.
- Structured JSON logging and safe request IDs.
- Removal of obsolete Gemini/Groq/OpenAI-STT configuration and legacy provider wrappers.
- Updated Render configuration for OpenAI and Sarvam.
- Expanded onboarding documentation and repository templates.

## Remaining gates

1. Let the latest CI runs complete and fix failures.
2. Measure backend coverage and enforce 80% after the endpoint matrix reaches it.
3. Complete timeout, upload-limit, and upstream-failure tests for all public routes.
4. Finish frontend quality verification.
5. Split remaining files over 500 lines and large functions.
6. Add Firebase Emulator rules tests.
7. Complete dependency license reporting.
8. Review and close or merge the current open pull requests.
9. Tag v0.1.0 and publish the release.
10. Complete manual GitHub, Render, Firebase, and production smoke-test steps.

## Verification limitation

A direct local fresh-clone execution could not be performed because outbound Git clone access is unavailable in this environment. The Fresh Clone Verification workflow is the reproducible CI equivalent. No local test execution is claimed until CI passes.
