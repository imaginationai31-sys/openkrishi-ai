# OpenKrishi AI — repository migration

The repository currently contains the FastAPI backend and Render deployment. It does not currently contain a production Android module or Firebase configuration on main.

Target:
- apps/pwa — farmer-facing PWA
- apps/android — Android/Capacitor shell in a later phase
- firebase — Firebase rules and indexes
- services/api — existing FastAPI API

Safety:
- preserve /api/v1
- keep Gemini credentials server-side
- never commit google-services.json or service-account JSON
- require authenticated ownership for private farmer data
- keep AI diagnosis uncertainty-aware and safety-constrained
- do not remove Render until the Cloud Run replacement passes smoke tests
