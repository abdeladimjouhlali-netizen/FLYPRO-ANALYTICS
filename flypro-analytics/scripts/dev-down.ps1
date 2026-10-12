# =============================================================================
# scripts/dev-down.ps1
# Rôle : arrêter la stack Docker Compose sous Windows.
# =============================================================================
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
docker compose down
Write-Host "FlyPro Analytics stack stopped."
