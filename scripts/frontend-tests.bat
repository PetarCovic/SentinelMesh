@echo off
setlocal

cd /d "%~dp0..\frontend"

echo ========================================
echo Running frontend tests
echo ========================================

call npm run test:run

if errorlevel 1 (
    echo Frontend tests failed.
    endlocal
    exit /b 1
)

echo ========================================
echo Running frontend production build
echo ========================================

call npm run build

if errorlevel 1 (
    echo Frontend build failed.
    endlocal
    exit /b 1
)

echo Frontend tests and build passed.

endlocal