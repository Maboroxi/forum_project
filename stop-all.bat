@echo off
echo Stopping all microservices...
echo.

taskkill /fi "windowtitle eq gateway-service*" /f 2>nul
taskkill /fi "windowtitle eq user-service*" /f 2>nul
taskkill /fi "windowtitle eq oss-service*" /f 2>nul
taskkill /fi "windowtitle eq forum-service*" /f 2>nul
taskkill /fi "windowtitle eq notification-service*" /f 2>nul
taskkill /fi "windowtitle eq announcement-service*" /f 2>nul
taskkill /fi "windowtitle eq ai-service*" /f 2>nul

echo Done.
pause
