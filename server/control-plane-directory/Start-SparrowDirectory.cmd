@echo off
cd /d "%~dp0"
start "Sparrow Directory Installer" powershell.exe -NoLogo -NoProfile -STA -ExecutionPolicy Bypass -File "%~dp0Start-SparrowDirectory.ps1"
