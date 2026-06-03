@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo Starting only PostgreSQL and Redis
echo ========================================

docker compose up postgres redis

endlocal