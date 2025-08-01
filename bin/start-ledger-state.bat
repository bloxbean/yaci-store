@echo off

REM Save the current working directory
SET CURRENT_DIR=%CD%

REM Uncomment the following line to enable polyglot (Python, JS) plugins support.
:: set "JAVA_OPTS=%JAVA_OPTS% -Dloader.path=plugins,plugins/lib,plugins/ext-jars"

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
SET JAR_PATH=%SCRIPT_DIR%\..\yaci-store-ledger-state.jar

REM Start the Spring Boot application with the desired profile
"%JAVA_HOME%\bin\java" %JAVA_OPTS% -Dspring.profiles.active=ledger-state -jar "%JAR_PATH%"

REM Return to the original working directory
CD /D %CURRENT_DIR%
