@echo off
setlocal EnableExtensions
call "%~dp0Build-SparrowServer.cmd" -IncludeLocalImages %*
set "SPARROW_LOCAL_BUILD_EXIT=%ERRORLEVEL%"
if not "%SPARROW_LOCAL_BUILD_EXIT%"=="0" (
    echo Local server build failed. Review the error above.
) else (
    echo Local installer ready in the project dist folder: sparrow-server.zip
)
pause
exit /b %SPARROW_LOCAL_BUILD_EXIT%
