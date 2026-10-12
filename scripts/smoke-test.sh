#!/usr/bin/env bash
set -euo pipefail

API="http://127.0.0.1:${BACKEND_PORT:-8080}"
AI="http://127.0.0.1:${AI_PORT:-8000}"
WEB="http://127.0.0.1:${FRONTEND_PORT:-80}"

echo "==> Smoke: backend health"
curl -fsS "$API/actuator/health" | tee /tmp/backend-health.json
grep -q '"status":"UP"' /tmp/backend-health.json

echo "==> Smoke: AI health"
curl -fsS "$AI/api/v1/ai/health" | tee /tmp/ai-health.json
grep -Eq '"success"[[:space:]]*:[[:space:]]*true|"status"[[:space:]]*:[[:space:]]*"UP"' /tmp/ai-health.json

echo "==> Smoke: frontend gateway"
curl -fsS "$WEB/healthz" | grep -q ok
curl -fsS -o /dev/null -w "%{http_code}" "$WEB/" | grep -Eq '200|304'

echo "==> Smoke: login via backend"
LOGIN_PAYLOAD='{"username":"admin","password":"Admin@123"}'
curl -fsS -H "Content-Type: application/json" -d "$LOGIN_PAYLOAD" "$API/api/v1/auth/login" | tee /tmp/login.json
grep -q 'accessToken' /tmp/login.json

echo "==> Smoke: login via Nginx proxy"
curl -fsS -H "Content-Type: application/json" -d "$LOGIN_PAYLOAD" "$WEB/api/v1/auth/login" | tee /tmp/login-proxy.json
grep -q 'accessToken' /tmp/login-proxy.json

echo "==> Smoke tests passed"
