# API Guide

Base URL for local development: http://localhost:8000.

Production API: https://openkrishi-ai-api.onrender.com.

## Health

~~~bash
curl http://localhost:8000/api/v1/health
~~~

## Advisory

~~~bash
curl -X POST http://localhost:8000/api/v1/advisory \
  -H 'Content-Type: application/json' \
  -d '{"query":"My rice plants have yellow leaves","language":"en","crop_category":"rice","crop_name":"rice","growth_stage":"vegetative","input_mode":"text"}'
~~~

The response contains advisory text, confidence, uncertainty, recommendations, and safety information.

## Vision

~~~bash
curl -X POST http://localhost:8000/api/v1/vision/assess \
  -F 'file=@rice.jpg' \
  -F 'language=en' \
  -F 'crop_category=rice' \
  -F 'growth_stage=vegetative'
~~~

Images are size- and type-limited.

## Voice transcription

~~~bash
curl -X POST http://localhost:8000/api/v1/voice/transcribe \
  -F 'file=@farmer.ogg' \
  -F 'language=bn'
~~~

## Voice advisory

~~~bash
curl -X POST http://localhost:8000/api/v1/voice/advisory \
  -F 'file=@farmer.ogg' \
  -F 'language=bn' \
  -F 'crop_category=rice'
~~~

## Voice + vision advisory

~~~bash
curl -X POST http://localhost:8000/api/v1/voice/vision-advisory \
  -F 'file=@farmer.ogg' \
  -F 'image=@rice.jpg' \
  -F 'language=bn' \
  -F 'crop_category=rice'
~~~

## Weather

The weather endpoint accepts the location parameters documented by the OpenAPI schema. Provider failures are returned as controlled service errors.

## Farm intelligence

The farm routes expose deterministic helpers for plan, irrigation, fertilizer, pest alerts, recommendation, crop knowledge, and market information.

## Validation and errors

- 422: invalid structured request or unsupported language/crop selection.
- 400: malformed or empty upload/input.
- 413: upload exceeds the configured size limit.
- 502/503: an upstream provider is unavailable or returns an unusable response.
- 500: unexpected server error; the API intentionally returns a generic message and logs a trace ID.

Use /docs or /openapi.json for the exact current request and response schema.
