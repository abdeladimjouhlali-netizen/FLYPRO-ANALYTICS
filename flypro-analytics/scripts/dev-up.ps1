# =============================================================================
# scripts/dev-up.ps1
# Rôle : démarrer la stack Docker Compose sous Windows.
# =============================================================================
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
docker compose up -d --build
Write-Host "FlyPro Analytics stack starting (postgres, backend, frontend, ai-service)."
