@echo off
rem Builds the signed release APK into dist\ (needs keystore.properties + release.jks in this folder).
cd /d "%~dp0"
call gradlew.bat :app:releaseApk
if errorlevel 1 (pause & exit /b 1)
explorer dist
