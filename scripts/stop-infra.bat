@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo Stopping PostgreSQL and Redis
echo ========================================

docker compose stop postgres redis

endlocal