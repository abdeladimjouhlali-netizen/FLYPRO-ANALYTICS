#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env ]]; then
  echo "Creating .env from .env.example"
  cp .env.example .env
fi

echo "==> Building and starting FlyPro Analytics"
docker compose up --build -d

chmod +x scripts/wait-for-healthy.sh scripts/smoke-test.sh
./scripts/wait-for-healthy.sh
./scripts/smoke-test.sh

echo ""
echo "FlyPro is up:"
echo "  UI:        http://localhost:${FRONTEND_PORT:-80}"
echo "  API:       http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html"
echo "  AI:        http://localhost:${AI_PORT:-8000}/docs"
echo "  Actuator:  http://localhost:${BACKEND_PORT:-8080}/actuator/health"
