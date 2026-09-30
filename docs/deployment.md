# Deployment

## Render backend

The repository includes render.yaml for the FastAPI service.

Required production configuration:

- CORS_ALLOW_ORIGINS
- OPENAI_API_KEY
- OPENAI_VISION_MODEL
- OPENAI_ADVISORY_MODEL
- SARVAM_API_KEY
- SARVAM_STT_MODEL
- SARVAM_TTS_MODEL

Secret entries must remain sync: false in render.yaml.

After deployment:

~~~bash
curl https://openkrishi-ai-api.onrender.com/api/v1/health
~~~

Then open /docs and test each provider-backed route with valid configuration.

## PWA hosting

The frontend is static.

~~~bash
cd frontend
npm ci
npm run build
~~~

Publish frontend/dist/ to the chosen static host.

The PWA must not contain provider API keys.

## Firebase

Configure Firebase Auth, Firestore, Storage, and App Check separately. Deploy rules from the repository after reviewing the target Firebase project.

## Docker

~~~bash
docker compose up --build
~~~

For production container hosting, inject secrets through the platform's secret manager instead of baking them into the image.

## Release verification

1. Run backend tests with coverage.
2. Run Ruff and mypy.
3. Run frontend lint, typecheck, unit tests, and build.
4. Run dependency and secret scans.
5. Build both Docker images.
6. Verify the health endpoint.
7. Test advisory, vision, voice, and weather with real provider configuration.
8. Confirm CORS and Firebase rules.
