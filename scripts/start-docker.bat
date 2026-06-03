@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo Starting SentinelMesh with Docker Compose
echo ========================================

docker compose up --build

endlocal