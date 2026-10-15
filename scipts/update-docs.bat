@echo off
cd /d "%~dp0"

echo =====================================
echo Updating architecture documentation
echo =====================================

call gradlew.bat architectureReport workflowCatalog

if errorlevel 1 (
    echo.
    echo Documentation generation failed.
    pause
    exit /b 1
)

echo.
echo Documentation updated successfully.
pause
