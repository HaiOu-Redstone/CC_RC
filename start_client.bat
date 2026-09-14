@echo off
chcp 65001 >nul
title CC: Reactor Console - Minecraft Client

set JAVA_HOME=E:\trae\java17_temp\jdk-17.0.20+8
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo   CC: Reactor Console
echo   Starting Minecraft Client...
echo ========================================
echo.

rem === single-instance guard ===============================================
rem If a CC_RC client (BootstrapLauncher JVM) is already running, abort:
rem a 2nd launch shares run/ files and dies early (exit code 268435466).
rem NOTE: keep this file pure ASCII and avoid ( ) anywhere - cmd parses
rem if-blocks by counting brackets, a stray parenthesis swallows the rest.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start_client_guard.ps1"
if %ERRORLEVEL% EQU 1 goto already_running

rem === launch with auto-retry ===============================================
rem runClient sometimes exits early on Windows with code 268435466 when a
rem previous game session is still winding down; retry a few times.
set ATTEMPTS=0
:retry_launch
set /a ATTEMPTS+=1
"%~dp0gradlew.bat" runClient > "%~dp0run\start_client.log" 2>&1
if %ERRORLEVEL% EQU 0 exit /b 0
if %ATTEMPTS% LSS 3 (
    echo.
    echo [INFO] Launch attempt %ATTEMPTS% failed. Retrying in 8 seconds...
    timeout /t 8 /nobreak >nul
    goto retry_launch
)
goto launch_failed

:already_running
echo.
echo [INFO] A game client is already running. Close it first, then try again.
echo       Two clients cannot share the same run folder.
pause
exit /b 1

:launch_failed
echo.
echo [ERROR] Minecraft client failed to start after 3 attempts.
echo [INFO] Full log saved to run\start_client.log
echo.
echo ------- last log lines -------
type "%~dp0run\start_client.log"
echo.
echo If this keeps happening: close ALL game windows, wait 10 seconds, then
echo double-click this exact file:
echo   E:\trae\program\CC_RC\start_client.bat
pause
exit /b 1