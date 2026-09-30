PYTHON ?= python3
PIP ?= $(PYTHON) -m pip

.PHONY: install dev test lint typecheck format docker-up

install:
	$(PIP) install -r services/api/requirements.txt
	$(PIP) install -r requirements-dev.txt

dev:
	uvicorn services.api.server:app --reload --host 0.0.0.0 --port 8000

test:
	pytest -q

lint:
	ruff check services tests

typecheck:
	mypy services

format:
	ruff format services tests

docker-up:
	docker compose up --build
