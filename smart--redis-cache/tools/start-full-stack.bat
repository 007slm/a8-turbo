@echo off
title Redis Smart Cache Full Stack

echo =====================================================
echo   Redis Smart Cache Full Stack Launcher
echo =====================================================
echo.
echo Starting backend server (Spring Boot)...
echo.

cd /d %~dp0\redis-smart-cache-client

start "Backend Server" cmd /k "title Backend Server && gradlew.bat bootRun --args='--spring.profiles.active=dev' || gradle bootRun --args='--spring.profiles.active=dev'"

echo.
echo Waiting for backend to start...
timeout /t 10 /nobreak > nul

echo.
echo Starting frontend UI (React + Vite)...
echo.

cd /d %~dp0\redis-smart-cache-ui-bytecode

start "Frontend UI" cmd /k "title Frontend UI && npm run dev"

echo.
echo =====================================================
echo   Both applications are starting...
echo =====================================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Press any key to exit launcher...
pause > nul