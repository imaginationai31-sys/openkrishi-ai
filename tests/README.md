# Test Guide

## Backend

Backend tests live in tests/ and use pytest.

Run:

~~~bash
make test
~~~

The test suite is intended to be offline-safe. Provider calls must be mocked for OpenAI, Sarvam, weather HTTP requests, and Firebase interactions.

The endpoint matrix covers success, validation, upload limits/types, and controlled upstream failures.

## Frontend

PWA unit tests live under frontend/src and use Vitest.

~~~bash
cd frontend
npm test
~~~

The frontend quality job also runs ESLint, TypeScript checking, Prettier validation, and the static build.

## Coverage

Backend CI publishes pytest coverage output and is intended to enforce an 80% minimum once the endpoint matrix reaches that threshold.

Frontend Vitest is configured with an 80% global threshold for lines, functions, branches, and statements.

## Test quality rules

- Prefer real assertions over smoke tests.
- Mock provider boundaries, not the business logic being tested.
- Add a regression test for every bug fixed.
- Include valid and invalid inputs for public endpoints.
- Never use real API credentials in tests.
