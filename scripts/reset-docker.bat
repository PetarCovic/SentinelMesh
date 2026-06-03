@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo WARNING: This will stop containers and delete Docker volumes.
echo This will remove the Docker PostgreSQL and Redis data.
echo ========================================
echo.

set /p CONFIRM=Type RESET to continue: 

if not "%CONFIRM%"=="RESET" (
    echo Reset cancelled.
    endlocal
    exit /b 0
)

docker compose down -v

echo Docker services and volumes removed.

endlocal