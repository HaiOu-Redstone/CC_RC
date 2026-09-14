@echo off
setlocal enabledelayedexpansion
rem QQ broadcast daemon launcher (thought summary / question / error / watchdog)
rem Uses DSH bundled Node to avoid PATH dependency
rem Usage:
rem   start_qq_broadcast.bat           foreground (Ctrl+C to exit)
rem   start_qq_broadcast.bat --hello   start and broadcast hello message
rem   start_qq_broadcast.bat --daemon  hidden background start (for autostart; adds --hello)
setlocal

set "NODE=E:\Program Files\DeepSeek_Harness_Desktop\DeepSeek Harness\resources\runtime\win32-x64\node.exe"
set "SCRIPT=%~dp0qq_broadcast.mjs"
set "LOCK=%TEMP%\ccrc_qq_broadcast.lock"

if not exist "%NODE%" (
  echo [FAIL] DSH builtin Node not found: %NODE%
  exit /b 1
)
if not exist "%SCRIPT%" (
  echo [FAIL] broadcast script not found: %SCRIPT%
  exit /b 1
)

rem ---- single-instance guard ----
if exist "%LOCK%" (
  echo [RUNNING] QQ broadcast daemon already running; skip this start.
  exit /b 0
)
> "%LOCK%" echo 1

rem ---- hidden background mode ----
set "DAEMON=0"
for %%a in (%*) do if /i "%%a"=="--daemon" set "DAEMON=1"

if "!DAEMON!"=="1" (
  set "LOG=%TEMP%\ccrc_qq_broadcast.log"
  start "" /b "%NODE%" "%SCRIPT%" --hello 1>>"!LOG!" 2>&1
  echo [BG] QQ broadcast daemon started hidden, log: "!LOG!"
  exit /b 0
)

echo Starting QQ broadcast daemon (Ctrl+C to exit)...
"%NODE%" "%SCRIPT%" %*
set EXITCODE=%ERRORLEVEL%
del "%LOCK%" 2>nul
exit /b %EXITCODE%