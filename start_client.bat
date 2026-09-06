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

.\gradlew runClient

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Minecraft client exited with code %ERRORLEVEL%
    pause
)