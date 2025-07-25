#!/bin/bash

# Uncomment the following line to enable polyglot (Python, JS) plugins support.
#JAVA_OPTS="$JAVA_OPTS -Dloader.path=plugins,plugins/lib,plugins/ext-jars"

# Ensure JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set. Please set JAVA_HOME before running the script."
    exit 1
fi

# Get the path of the JAR file relative to this script's location
SCRIPT_DIR=$(dirname "$0")
JAR_PATH="$SCRIPT_DIR/../yaci-store-ledger-state.jar"

# Start the Spring Boot application with the desired profile
"$JAVA_HOME/bin/java" $JAVA_OPTS -Dspring.profiles.active=ledger-state -jar "$JAR_PATH"
