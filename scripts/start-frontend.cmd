@echo off
REM Bypass ExecutionPolicy and start Vite frontend (local, no Docker)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1"
