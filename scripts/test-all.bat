@echo off
setlocal

cd /d "%~dp0.."

echo ========================================
echo Running all SentinelMesh checks
echo ========================================

echo.
echo [1/2] Backend tests
echo ----------------------------------------
call scripts\backend-tests.bat

if errorlevel 1 (
    echo.
    echo Full test suite failed during backend tests.
    endlocal
    exit /b 1
)

echo.
echo [2/2] Frontend tests and build
echo ----------------------------------------
call scripts\frontend-tests.bat

if errorlevel 1 (
    echo.
    echo Full test suite failed during frontend checks.
    endlocal
    exit /b 1
)

echo.
echo ========================================
echo All SentinelMesh checks passed.
echo ========================================

endlocal