# OpenKrishi AI API

FastAPI service for the OpenKrishi AI MVP.

## Run locally

From the repository root:

```bash
pip install -r services/api/requirements.txt
uvicorn services.api.server:app --reload
```

The API will be available at `http://127.0.0.1:8000`.

## Endpoints

- `GET /api/v1/health` — service health check
- `POST /api/v1/advisory` — text-based farmer advisory request
- `GET /docs` — interactive OpenAPI documentation

The current advisory engine is deterministic and intentionally conservative. It does not perform disease diagnosis, live weather analysis, or pesticide recommendations yet.
