#!/usr/bin/env bash
set -euo pipefail

TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-240}"
INTERVAL_SEC=5
ELAPSED=0

echo "==> Waiting for FlyPro services to become healthy (timeout=${TIMEOUT_SEC}s)"

services=(flypro-mysql flypro-backend flypro-ai flypro-frontend)

is_healthy() {
  local name="$1"
  local status
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || echo missing)"
  [[ "$status" == "healthy" || "$status" == "running" ]]
}

while (( ELAPSED < TIMEOUT_SEC )); do
  all_ok=1
  for svc in "${services[@]}"; do
    if ! is_healthy "$svc"; then
      all_ok=0
      break
    fi
  done
  if (( all_ok == 1 )); then
    # Prefer real HTTP probes for app services
    if curl -fsS "http://127.0.0.1:${BACKEND_PORT:-8080}/actuator/health" >/dev/null \
      && curl -fsS "http://127.0.0.1:${AI_PORT:-8000}/api/v1/ai/health" >/dev/null \
      && curl -fsS "http://127.0.0.1:${FRONTEND_PORT:-80}/healthz" >/dev/null; then
      echo "==> All services healthy"
      exit 0
    fi
  fi
  sleep "$INTERVAL_SEC"
  ELAPSED=$((ELAPSED + INTERVAL_SEC))
  echo "... still waiting (${ELAPSED}s)"
done

echo "==> Timeout waiting for healthy services" >&2
docker compose ps || true
exit 1
