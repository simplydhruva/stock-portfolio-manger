@echo off
cd /d "%~dp0"

REM Kill any existing Java processes to free up the database
taskkill /f /im java.exe >nul 2>&1

REM Clean any existing database lock files
if exist "data\stockportfolio.lock.db" del "data\stockportfolio.lock.db"

REM Run the application
.\apache-maven-3.9.11\bin\mvn exec:java
