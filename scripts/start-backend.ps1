# =============================================================================
# FlyPro – démarrage BACKEND local (sans Docker / sans AI)
# Usage: .\scripts\start-backend.ps1
# Prérequis: MySQL local sur localhost:3306 (service Windows MySQL80 OK)
# =============================================================================

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "flypro-backend"

$javaHome = "C:\Users\pc\.jdks\openjdk-25.0.2"
$mvnBin = "C:\Users\pc\.maven\apache-maven-3.9.11\bin"

if (-not (Test-Path "$javaHome\bin\java.exe")) {
    Write-Error "Java introuvable: $javaHome — installe un JDK 21+ et adapte JAVA_HOME dans ce script."
}
if (-not (Test-Path "$mvnBin\mvn.cmd")) {
    Write-Error "Maven introuvable: $mvnBin — adapte le chemin dans ce script."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mvnBin;$env:Path"

# Ne pas charger le .env racine Docker (hostnames mysql / ai-service)
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DATABASE_URL = "jdbc:mysql://localhost:3306/flypro_analytics?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DATABASE_USERNAME = if ($env:DATABASE_USERNAME) { $env:DATABASE_USERNAME } else { "flypro" }
$env:DATABASE_PASSWORD = if ($env:DATABASE_PASSWORD) { $env:DATABASE_PASSWORD } else { "flypro" }

Write-Host "Java:" (java -version 2>&1 | Select-Object -First 1)
Write-Host "Maven:" (mvn -v 2>&1 | Select-Object -First 1)
Write-Host "MySQL check (port 3306)..."
$tcp = Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue
if (-not $tcp.TcpTestSucceeded) {
    Write-Error "MySQL n'écoute pas sur localhost:3306. Démarre le service Windows MySQL80 (Services.msc)."
}

Write-Host "Starting Spring Boot (profile=dev) on http://localhost:8080 ..."
Set-Location $backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
