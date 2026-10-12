#!/usr/bin/env bash
# =============================================================================
# scripts/dev-down.sh
# Rôle futur : arrêter l'environnement de développement.
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose down
echo "FlyPro Analytics stack stopped."
