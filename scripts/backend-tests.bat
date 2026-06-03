@echo off
setlocal

cd /d "%~dp0..\backend"

echo ========================================
echo Running backend tests
echo ========================================

call mvnw.cmd clean test

if errorlevel 1 (
    echo Backend tests failed.
    endlocal
    exit /b 1
)

echo Backend tests passed.

endlocal