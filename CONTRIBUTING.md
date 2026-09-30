# Contributing to OpenKrishi AI

Thank you for contributing.

## Development setup

1. Clone the repository.
2. Install Python 3.11 or 3.12 and uv.
3. Run uv sync --dev.
4. Copy .env.example to .env.
5. Set only provider keys required for the feature being tested.
6. Install frontend dependencies with cd frontend && npm ci.

## Before changing code

Read:

- README.md
- docs/architecture.md
- docs/THREAT_MODEL.md
- SECURITY.md

## Tests and quality

Backend:

~~~bash
make test
make lint
make typecheck
~~~

Frontend:

~~~bash
cd frontend
npm test
npm run lint
npm run typecheck
npm run format:check
npm run build
~~~

## Commit conventions

Use Conventional Commits:

- feat: new functionality
- fix: bug fix
- refactor: behavior-preserving restructuring
- test: tests
- docs: documentation
- build: dependencies/build tooling
- ci: CI/CD
- security: security controls
- chore: maintenance

Keep commits small and focused.

## Pull request checklist

- [ ] Tests added or updated.
- [ ] Backend tests pass.
- [ ] Frontend checks pass when applicable.
- [ ] No real secrets or private farmer data were added.
- [ ] API behavior changes are documented.
- [ ] Security implications were reviewed.
- [ ] Commit messages follow the project convention.

## Agricultural safety

Do not add unsupported diagnoses, treatment claims, pesticide rates, or fertilizer instructions without reliable evidence and safety review.
