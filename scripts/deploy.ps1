$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not (Test-Path ".env")) {
  Write-Host "Creating .env from .env.example"
  Copy-Item ".env.example" ".env"
}

Write-Host "==> Building and starting FlyPro Analytics"
docker compose up --build -d

Write-Host "==> Waiting for health endpoints..."
$deadline = (Get-Date).AddMinutes(5)
do {
  try {
    $backend = Invoke-WebRequest -Uri "http://127.0.0.1:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    $ai = Invoke-WebRequest -Uri "http://127.0.0.1:8000/api/v1/ai/health" -UseBasicParsing -TimeoutSec 5
    $web = Invoke-WebRequest -Uri "http://127.0.0.1/healthz" -UseBasicParsing -TimeoutSec 5
    if ($backend.StatusCode -eq 200 -and $ai.StatusCode -eq 200 -and $web.StatusCode -eq 200) {
      Write-Host "All services healthy"
      break
    }
  } catch {
    Start-Sleep -Seconds 5
  }
  if ((Get-Date) -gt $deadline) { throw "Timeout waiting for healthy services" }
} while ($true)

Write-Host ""
Write-Host "FlyPro is up:"
Write-Host "  UI:       http://localhost/"
Write-Host "  API:      http://localhost:8080/swagger-ui.html"
Write-Host "  AI:       http://localhost:8000/docs"
Write-Host "  Actuator: http://localhost:8080/actuator/health"
