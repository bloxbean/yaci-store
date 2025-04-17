@echo off
REM ------------------------------------------------------------
REM start.bat — Launch yaci-store with optional profile
REM Usage:
REM   start.bat            -> no Spring profile
REM   start.bat [profile]  -> activate given Spring profile
REM ------------------------------------------------------------

REM Save the current working directory
SET CURRENT_DIR=%CD%

REM Change to the top-level directory (parent of the bin folder)
CD /D %~dp0..
IF %ERRORLEVEL% NEQ 0 (
    echo Failed to change directory to the top folder.
    exit /b 1
)

REM Ensure JAVA_HOME is set
IF NOT DEFINED JAVA_HOME (
    echo JAVA_HOME is not set. Please set JAVA_HOME before running the script.
    exit /b 1
)

REM Get the path of the JAR file relative to this script's location
SET SCRIPT_DIR=%~dp0
SET JAR_PATH=%SCRIPT_DIR%\..\yaci-store.jar

REM Check for profile argument
IF "%~1"=="" (
    echo Starting yaci-store with default Spring profile (default)...
    "%JAVA_HOME%\bin\java" %JAVA_OPTS% -jar "%JAR_PATH%"
) ELSE (
    echo Starting yaci-store with Spring profile: %~1...
    "%JAVA_HOME%\bin\java" %JAVA_OPTS% -Dspring.profiles.active="%~1" -jar "%JAR_PATH%"
)


REM Return to the original working directory
CD /D %CURRENT_DIR%
