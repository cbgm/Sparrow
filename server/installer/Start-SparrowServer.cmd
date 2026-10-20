@echo off
cd /d "%~dp0"
where pyw >nul 2>nul && (start "" pyw -3 "%~dp0app\manager_window.py" & exit /b 0)
where pythonw >nul 2>nul && (start "" pythonw "%~dp0app\manager_window.py" & exit /b 0)
echo Sparrow Server needs Python 3.9 or newer.
echo Install it from python.org (tick 'Add python.exe to PATH'), then start this file again.
start "" https://www.python.org/downloads/windows/
pause
