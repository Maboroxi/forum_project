@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo  Campus Forum - Starting all microservices
echo ==========================================
echo.

echo [Step 0] Building common modules first...
call mvn clean install -pl common-core,common-observability -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Common modules build failed!
    pause
    exit /b 1
)
echo Common modules built successfully.
echo.

echo [1/7] Starting gateway-service :8081 ...
start "gateway-service" cmd /c "cd /d "%~dp0gateway-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 8 /nobreak >nul

echo [2/7] Starting user-service :8082 ...
start "user-service" cmd /c "cd /d "%~dp0user-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 5 /nobreak >nul

echo [3/7] Starting oss-service :8084 ...
start "oss-service" cmd /c "cd /d "%~dp0oss-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 5 /nobreak >nul

echo [4/7] Starting forum-service :8088 ...
start "forum-service" cmd /c "cd /d "%~dp0forum-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 5 /nobreak >nul

echo [5/7] Starting notification-service :8085 ...
start "notification-service" cmd /c "cd /d "%~dp0notification-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 3 /nobreak >nul

echo [6/7] Starting announcement-service :8086 ...
start "announcement-service" cmd /c "cd /d "%~dp0announcement-service" && mvn spring-boot:run -DskipTests || pause"

timeout /t 3 /nobreak >nul

echo [7/7] Starting ai-service :8083 ...
start "ai-service" cmd /c "cd /d "%~dp0ai-service" && mvn spring-boot:run -DskipTests || pause"

echo.
echo All services started!
echo Gateway: http://localhost:8081
echo.
echo To stop: run stop-all.bat
pause
