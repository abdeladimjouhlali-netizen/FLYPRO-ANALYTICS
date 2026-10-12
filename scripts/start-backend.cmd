@echo off
REM Bypass ExecutionPolicy and start Spring Boot backend (local, no Docker)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"
