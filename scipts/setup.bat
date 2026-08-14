@echo off
cd /d "%~dp0"

echo =====================================
echo Sparrow Setup
echo =====================================

call gradlew.bat setup

echo.
echo Setup complete.
pause
