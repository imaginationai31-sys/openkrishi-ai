PYTHON ?= python3

.PHONY: install dev test lint typecheck format docker-up

install:
	uv sync --dev

dev:
	uv run uvicorn services.api.server:app --reload --host 0.0.0.0 --port 8000

test:
	uv run pytest -q

lint:
	uv run ruff check services tests

typecheck:
	uv run mypy services

format:
	uv run ruff format services tests

docker-up:
	docker compose up --build
