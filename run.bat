@echo off
call mvn --batch-mode --no-transfer-progress clean verify spring-boot:repackage
if errorlevel 1 exit /b %errorlevel%