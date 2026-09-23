@echo off
setlocal EnableExtensions
rem Run only this build process with the required script execution policy.
rem Do not change machine/user PowerShell policy or unblock unrelated scripts.
pushd "%~dp0..\.." || exit /b 1
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0New-SparrowServerBundle.ps1" %*
set "SPARROW_BUILD_EXIT=%ERRORLEVEL%"
popd
exit /b %SPARROW_BUILD_EXIT%
