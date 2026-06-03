@echo off
setlocal

cd /d "%~dp0.."

if "%1"=="" (
    echo Showing logs for all services.
    docker compose logs -f
) else (
    echo Showing logs for service: %1
    docker compose logs -f %1
)

endlocal