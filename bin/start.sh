#!/bin/bash

# Uncomment the following line to enable polyglot (Python, JS) plugins support.
#JAVA_OPTS="$JAVA_OPTS -Dloader.path=plugins,plugins/lib,plugins/ext-jars"

# Save the current working directory
CURRENT_DIR=$(pwd)

# Change to the top-level directory (parent of the bin folder)
TOP_DIR=$(dirname "$0")/..
cd "$TOP_DIR" || exit 1

# Ensure JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set. Please set JAVA_HOME before running the script."
    exit 1
fi

# Get the path of the JAR file relative to this script's location
SCRIPT_DIR=$(dirname "$0")
JAR_PATH="$SCRIPT_DIR/../yaci-store.jar"

# Check for profile argument
PROFILE="$1"

if [ -n "$PROFILE" ]; then
    echo "Starting yaci-store with Spring profile: $PROFILE"
    "$JAVA_HOME/bin/java" $JAVA_OPTS \
      -Dspring.profiles.active="$PROFILE" \
      -jar "$JAR_PATH"
else
    echo "Starting yaci-store with default Spring profile (default)"
    "$JAVA_HOME/bin/java" $JAVA_OPTS \
      -jar "$JAR_PATH"
fi

# Return to the original working directory
cd "$CURRENT_DIR" || exit 1
