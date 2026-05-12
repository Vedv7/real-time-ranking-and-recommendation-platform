.PHONY: up down logs seed demo smoke frontend-build ml-check compose-config

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f backend ml-service

seed:
	bash scripts/seed_data.sh

demo:
	bash scripts/demo_requests.sh

smoke:
	python scripts/smoke_test.py

frontend-build:
	cd frontend && npm run build

ml-check:
	cd ml-service && python -m compileall app training

compose-config:
	docker compose config
