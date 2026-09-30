# OpenKrishi AI

[![Backend Tests](https://github.com/imaginationai31-sys/openkrishi-ai/actions/workflows/backend-tests.yml/badge.svg)](https://github.com/imaginationai31-sys/openkrishi-ai/actions/workflows/backend-tests.yml) [![Coverage target](https://img.shields.io/badge/coverage-80%25%2B%20target-blue)](tests/README.md) [![License](https://img.shields.io/github/license/imaginationai31-sys/openkrishi-ai)](LICENSE)

**Free agricultural intelligence for every farmer.**

OpenKrishi AI is an open-source, multilingual agriculture platform for Indian farmers. It combines a mobile-first Progressive Web App (PWA) with a FastAPI backend for agricultural advisory, crop-image assessment, voice interaction, weather information, and farm-intelligence utilities.

The current product direction is **PWA-first**. Android Studio is not required to develop or run the primary client.

## Demo and production services

- PWA: https://openkrishi-ai.hatchable.site
- API: https://openkrishi-ai-api.onrender.com/
- Interactive API docs: https://openkrishi-ai-api.onrender.com/docs
- OpenAPI schema: https://openkrishi-ai-api.onrender.com/openapi.json
- Repository: https://github.com/imaginationai31-sys/openkrishi-ai

## Features

- Multilingual agricultural advisory.
- Crop problem assessment from farmer-described symptoms.
- Crop image assessment from camera or gallery.
- Conservative observations, possible causes, confidence, uncertainty, and safe next steps.
- Voice transcription using Sarvam Saaras.
- Voice advisory and speech output using Sarvam Bulbul.
- Weather lookup with location permission.
- Farm intelligence utilities for planning, irrigation, fertilizer guidance, pest alerts, crop recommendations, crop knowledge, and market information.
- Firebase-compatible authentication, Firestore history, Storage rules, and App Check integration.
- Mobile-first interface with large touch targets and simple workflows.

## Supported languages

- English ("en")
- Bengali ("bn")
- Hindi ("hi")
- Tamil ("ta")
- Punjabi ("pa")
- Telugu ("te")

The UI and backend are designed to preserve the selected/requested language instead of unnecessarily mixing languages.

## Crop categories

The product currently uses four main categories:

1. **Rice**
2. **Peanut**
3. **Vegetables**
   - Tomato
   - Chilli
   - Other supported vegetables
4. **Flowers**

Tomato and chilli belong to the **Vegetables** category; they are not separate top-level categories.

## Safety principles

OpenKrishi AI is an informational agricultural assistant. It does not replace agricultural officers, agronomists, plant clinics, or local authorities.

The advisory system should:

- distinguish image observations from possible causes;
- avoid claiming a disease is confirmed from a photo or symptom description alone;
- expose uncertainty and confidence;
- avoid unsafe or unsupported pesticide/fertilizer dosage instructions;
- recommend appropriate local expert confirmation for serious or uncertain cases;
- avoid unnecessary collection of farmer personal data;
- treat farmer uploads and location as sensitive inputs;
- fail safely when an external AI, speech, or weather provider is unavailable.

See SECURITY.md and docs/THREAT_MODEL.md.

## Architecture

~~~mermaid
flowchart LR
    Farmer["Farmer"] --> PWA["frontend/ mobile PWA"]
    PWA --> Firebase["Firebase Auth / Firestore / Storage"]
    PWA --> API["services/api FastAPI"]
    API --> Advisory["services/advisory"]
    API --> Vision["services/vision"]
    API --> Voice["services/voice"]
    API --> Weather["services/weather"]
    Advisory --> OpenAI["OpenAI"]
    Vision --> OpenAI
    Voice --> Sarvam["Sarvam AI"]
    Weather --> Meteo["Open-Meteo"]
~~~

The backend keeps third-party credentials server-side. The PWA calls the Render API rather than embedding provider API keys in browser code.

## Repository structure

~~~text
.
├── apps/                  # Existing/native client material
├── data/                  # Agricultural/static data
├── docs/                  # Architecture, API, deployment and security docs
├── evaluation/            # Evaluation and benchmark material
├── frontend/              # Mobile-first PWA
│   ├── index.html
│   ├── src/               # Shared browser API/language logic and unit tests
│   ├── scripts/           # Deterministic static build helpers
│   └── Dockerfile
├── models/                # Model-related assets/configuration
├── services/
│   ├── api/               # FastAPI application and routers
│   ├── advisory/          # Advisory generation/localization/safety
│   ├── core/              # Configuration, uploads, security primitives
│   ├── farm/              # Farm intelligence helpers
│   ├── vision/            # Crop-image assessment
│   ├── voice/             # STT/TTS and voice orchestration
│   └── weather/           # Weather provider integration
├── tests/                 # Backend tests
├── Dockerfile             # Production backend image
├── docker-compose.yml     # Backend + PWA local stack
├── Makefile               # Common development commands
├── pyproject.toml         # Python project/tool configuration
└── uv.lock                # Exact Python dependency resolution
~~~

## Quick start: backend

Requirements: Git, Python 3.11/3.12, and uv. Install uv from the official uv documentation or with your preferred Python package manager before running the commands below.

~~~bash
git clone https://github.com/imaginationai31-sys/openkrishi-ai.git
cd openkrishi-ai
uv sync --dev
cp .env.example .env
~~~

Set real provider credentials in .env. Never commit .env.

Start the backend:

~~~bash
uv run uvicorn services.api.server:app --reload --host 0.0.0.0 --port 8000
~~~

Open:

- http://localhost:8000/
- http://localhost:8000/api/v1/health
- http://localhost:8000/docs

## Quick start: PWA

Requirements: Node.js 24+ and npm.

~~~bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
~~~

The static build is written to frontend/dist/.

## Quick start: Makefile

From the repository root:

~~~bash
make install
make dev
make test
make lint
make typecheck
make format
make docker-up
~~~

## Docker

Build and run the complete local stack:

~~~bash
docker compose up --build
~~~

Services:

- Backend: http://localhost:8000
- PWA: http://localhost:8080

The backend image is multi-stage and runs as a non-root user. Both images have health checks.

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| CORS_ALLOW_ORIGINS | Yes | Browser origins allowed by the API |
| OPENAI_API_KEY | Yes | Server-side OpenAI credential |
| OPENAI_VISION_MODEL | Yes | Vision model name |
| OPENAI_ADVISORY_MODEL | Yes | Advisory model name |
| SARVAM_API_KEY | Yes | Server-side Sarvam credential |
| SARVAM_STT_MODEL | Yes | Sarvam STT model |
| SARVAM_TTS_MODEL | Yes | Sarvam TTS model |
| OPEN_METEO_URL | No | Weather provider endpoint |
| SARVAM_STT_URL | No | Sarvam STT endpoint |
| SARVAM_TTS_URL | No | Sarvam TTS endpoint |
| MAX_IMAGE_BYTES | No | Maximum image upload size |
| MAX_AUDIO_BYTES | No | Maximum audio upload size |
| OUTBOUND_TIMEOUT_SECONDS | No | Third-party timeout |
| WEATHER_TIMEOUT_SECONDS | No | Weather timeout |
| AI_RATE_LIMIT | No | Advisory rate limit |
| VOICE_RATE_LIMIT | No | Voice rate limit |
| VISION_RATE_LIMIT | No | Vision rate limit |

See .env.example for the commented template.

## API reference

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/v1/health | Service health |
| POST | /api/v1/advisory | Text advisory |
| POST | /api/v1/vision/assess | Crop image assessment |
| POST | /api/v1/voice/transcribe | Audio transcription |
| POST | /api/v1/voice/advisory | Voice-to-advisory-to-voice |
| POST | /api/v1/voice/vision-advisory | Combined voice + image advisory |
| GET | /api/v1/weather | Weather data |
| GET | /api/v1/farm/plan | Farm plan |
| GET | /api/v1/farm/irrigation | Irrigation guidance |
| GET | /api/v1/farm/fertilizer | Fertilizer guidance |
| GET | /api/v1/farm/pest-alerts | Pest alerts |
| GET | /api/v1/farm/recommendation | Crop recommendation |
| GET | /api/v1/farm/knowledge | Crop knowledge |
| GET | /api/v1/farm/market | Market information |

### Advisory example

~~~bash
curl -X POST http://localhost:8000/api/v1/advisory \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "My rice plants have yellow leaves",
    "language": "en",
    "crop_category": "rice",
    "crop_name": "rice",
    "growth_stage": "vegetative",
    "location": "India",
    "input_mode": "text"
  }'
~~~

### Vision example

~~~bash
curl -X POST http://localhost:8000/api/v1/vision/assess \
  -F 'file=@rice.jpg' \
  -F 'language=en' \
  -F 'crop_category=rice' \
  -F 'growth_stage=vegetative'
~~~

## Testing

Backend tests are designed to run offline. External AI, speech, weather, and Firebase interactions should be mocked.

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

See tests/README.md for the detailed test matrix.

## Deployment

### Render API

render.yaml defines the API service and its health-check path:

~~~text
/api/v1/health
~~~

Production secrets are set in Render environment variables. Secret entries in render.yaml use sync: false.

### PWA hosting

The PWA is static and can be hosted by Hatchable, Render Static Site, Firebase Hosting, GitHub Pages, or another CDN/static host.

## Security

Security controls include centralized Pydantic settings, explicit CORS, rate limiting, upload limits, bounded reads, outbound timeouts, sanitized errors, request trace IDs, Firebase deny-by-default rules, Gitleaks, CodeQL, and dependency audits.

Read SECURITY.md and docs/THREAT_MODEL.md.

## Development workflow

1. Create a short-lived branch.
2. Make one focused change.
3. Add or update tests.
4. Run backend and frontend quality checks affected by the change.
5. Use a Conventional Commit message.
6. Open a pull request with verification details.
7. Do not merge until required CI checks pass.

## Documentation map

- docs/architecture.md
- docs/api.md
- docs/deployment.md
- docs/THREAT_MODEL.md
- SECURITY.md
- CONTRIBUTING.md
- CHANGELOG.md
- ROADMAP.md
- tests/README.md

## Release process

Releases use semantic version tags such as v0.1.0.

Before a release:

1. Update CHANGELOG.md.
2. Verify the full CI suite.
3. Create and push the version tag.
4. The release workflow publishes GitHub Release notes from the changelog.

## License

OpenKrishi AI is released under the **Apache License 2.0 (Apache-2.0)**. See LICENSE.

## Contributing

Read CONTRIBUTING.md before opening a pull request.

---

OpenKrishi AI is intended to make practical agricultural intelligence easier to access in Indian languages while preserving uncertainty, safety, and farmer agency.
