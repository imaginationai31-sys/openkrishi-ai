# OpenKrishi AI — Deployment

## Environments
Maintain separate:
- development
- staging
- production

Credentials and data must never be casually shared between environments.

## Current Direction
Render remains suitable for the current FastAPI deployment. The application should remain stateless so additional instances can be added later.

## Deployment Pipeline
1. Pull request checks
2. Build/test
3. Security checks
4. Staging deployment
5. Smoke tests
6. Production deployment
7. Health verification
8. Monitoring/rollback

## Configuration
All environment-specific configuration belongs in managed environment variables/secrets. Never commit API keys.

## Health
Provide:
- liveness endpoint
- readiness endpoint
- dependency health signals where appropriate

## Database
Use migrations as part of controlled releases. Never rely on manual production schema edits.

## Rollback
Keep the previous known-good application version deployable. Database changes should use backward-compatible migration sequencing where possible.

## Infrastructure as Code
As infrastructure grows, use version-controlled Render configuration/Blueprints or equivalent declarative configuration.

## Disaster Recovery
Maintain backups and periodically test restoration. Initial target: RPO <=24 hours and RTO <=4 hours, then tighten targets as the platform matures.
