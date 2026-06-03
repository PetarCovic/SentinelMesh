@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo Stopping SentinelMesh Docker services
echo ========================================

docker compose down

endlocal