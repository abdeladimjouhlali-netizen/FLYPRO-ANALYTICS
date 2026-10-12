#!/usr/bin/env bash
# =============================================================================
# scripts/dev-up.sh
# Rôle futur : démarrer l'environnement de développement (Docker Compose).
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --build
echo "FlyPro Analytics stack starting (postgres, backend, frontend, ai-service)."
