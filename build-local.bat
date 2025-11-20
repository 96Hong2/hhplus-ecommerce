@echo off
REM Build script with local Gradle cache to avoid Windows file locking issues
REM This script sets GRADLE_USER_HOME to a project-local directory

setlocal

REM Set Gradle user home to project-local directory
set GRADLE_USER_HOME=%~dp0.gradle-cache

REM Create cache directory if it doesn't exist
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

echo Using local Gradle cache: %GRADLE_USER_HOME%
echo.

REM Run gradle with all provided arguments
"%~dp0gradlew.bat" %*

endlocal
