#!/bin/bash

# dev-run.sh
# Flexible development script to run Yaci Store with various profiles and configurations
# Usage: ./dev-run.sh [OPTIONS]

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

heading() {
    echo -e "${BLUE}[${1}]${NC}"
}

# Display usage
usage() {
    echo -e "$(cat << EOF
${BLUE}Yaci Store Development Runner${NC}

Usage: $0 [OPTIONS]

${GREEN}OPTIONS:${NC}
  --profile PROFILE        Spring profile(s) to activate (default: default)
                          Examples: plugins, ledger-state
  --config FILE           Additional config file to load
                          Example: config/application-plugins.yml
  --jar FILE              Path to JAR file (auto-detected if not provided)
  --jvm-opts "OPTS"       Additional JVM options
                          Example: "-Xmx8g -Xms2g"
  --enable-plugins        Enable polyglot plugin support (JS/Python)
  -h, --help              Show this help message

${GREEN}ENVIRONMENT VARIABLES:${NC}
  JAVA_OPTS               Additional JVM options (appended to --jvm-opts)

${GREEN}EXAMPLES:${NC}
  # Run with default profile
  $0

  # Run with plugins support and JS/Python enabled
  $0 --enable-plugins

  # Run with ledger-state profile
  $0 --profile ledger-state

  # Run with custom config
  $0 --enable-plugins --config config/application-plugins.yml

  # Run with custom JVM options
  $0 --profile ledger-state --jvm-opts "-Xmx8g -Xms4g"

  # Combine options
  $0 --enable-plugins --config config/application-plugins.yml \\
     --jvm-opts "-Xmx8g"

${GREEN}COMMON PROFILES:${NC}
  default                 Basic features (default, includes plugins profile)
  ledger-state            Enable ledger state tracking

${GREEN}PLUGIN SUPPORT:${NC}
  - Use --enable-plugins to load GraalVM polyglot libraries for JS/Python
  - Without --enable-plugins, only MVEL plugins work
  - Polyglot libraries add ~224MB to classpath
  - Auto-detected from: components/plugin-polyglot/build/libs/

EOF
)"
    exit 0
}

# Default values
PROFILE="default"
CONFIG_FILE=""
JAR_FILE=""
JVM_OPTS_CUSTOM=""
ENABLE_PLUGINS=false
EXTRA_ARGS=()

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            ;;
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        --jar)
            JAR_FILE="$2"
            shift 2
            ;;
        --jvm-opts)
            JVM_OPTS_CUSTOM="$2"
            shift 2
            ;;
        --enable-plugins)
            ENABLE_PLUGINS=true
            shift
            ;;
        *)
            # Collect unknown arguments to pass to application
            EXTRA_ARGS+=("$1")
            shift
            ;;
    esac
done

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Change to script directory
cd "$SCRIPT_DIR"

heading "YACI STORE DEVELOPMENT RUNNER"
echo ""

# Determine JAR file
if [ -z "$JAR_FILE" ]; then
    info "Auto-detecting JAR file..."
    JAR_FILE=$(find applications/all/build/libs -name "yaci-store-*.jar" \
        -not -name "*-sources.jar" \
        -not -name "*-javadoc.jar" \
        -not -name "*-plain.jar" 2>/dev/null | head -1)

    if [ -z "$JAR_FILE" ]; then
        error "Could not find yaci-store JAR file in applications/all/build/libs/"
        error "Please build the project first: ./gradlew build"
        exit 1
    fi
fi

# Verify JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    error "JAR file not found: $JAR_FILE"
    exit 1
fi

info "Using JAR: $JAR_FILE"
info "Active profile(s): $PROFILE"

# Build JVM options
ALL_JVM_OPTS="${JVM_OPTS_CUSTOM} ${JAVA_OPTS}"

# Build application arguments
APP_ARGS=()

# Add config file if specified
if [ -n "$CONFIG_FILE" ]; then
    if [ ! -f "$CONFIG_FILE" ]; then
        error "Config file not found: $CONFIG_FILE"
        exit 1
    fi
    info "Additional config: $CONFIG_FILE"
    APP_ARGS+=("--spring.config.additional-location=file:${CONFIG_FILE}")
fi

# Add any extra arguments
APP_ARGS+=("${EXTRA_ARGS[@]}")

# Handle plugin support
LOADER_PATH=""
if [ "$ENABLE_PLUGINS" = true ]; then
    POLYGLOT_JAR_DIR="components/plugin-polyglot/build/libs"
    POLYGLOT_LIB_DIR="components/plugin-polyglot/build/libs/plugin-libs"

    if [ ! -d "$POLYGLOT_JAR_DIR" ] || [ ! -d "$POLYGLOT_LIB_DIR" ]; then
        warn "Plugin-polyglot libraries not found!"
        warn "JavaScript and Python plugins will NOT work"
        warn "Only MVEL plugins will be available"
        warn "To enable JS/Python: ./gradlew build"
        warn ""
    else
        # Count polyglot libraries
        LIB_COUNT=$(ls -1 "$POLYGLOT_LIB_DIR"/*.jar 2>/dev/null | wc -l | tr -d ' ')
        info "Polyglot plugin support: ENABLED"
        info "  - Found $LIB_COUNT library JARs (~224MB)"
        info "  - Languages: MVEL, JavaScript, Python"

        LOADER_PATH="${POLYGLOT_JAR_DIR},${POLYGLOT_LIB_DIR}"
    fi
fi

# Display configuration summary
echo ""
heading "CONFIGURATION"
echo "  Profile(s):     $PROFILE"
echo "  JAR:            $(basename $JAR_FILE)"
if [ -n "$CONFIG_FILE" ]; then
    echo "  Config:         $CONFIG_FILE"
fi
if [ -n "$ALL_JVM_OPTS" ]; then
    echo "  JVM Options:    $ALL_JVM_OPTS"
fi
if [ "$ENABLE_PLUGINS" = true ]; then
    if [ -n "$LOADER_PATH" ]; then
        echo "  Plugin Support: ENABLED (MVEL, JS, Python)"
    else
        echo "  Plugin Support: PARTIAL (MVEL only)"
    fi
fi
echo ""

info "Starting Yaci Store..."
info "Press Ctrl+C to stop"
echo "================================================"
echo ""

# Build final java command
JAVA_CMD="java"

# Add spring profiles
JAVA_CMD="$JAVA_CMD -Dspring.profiles.active=$PROFILE"

# Add loader.path if plugins enabled
if [ -n "$LOADER_PATH" ]; then
    JAVA_CMD="$JAVA_CMD -Dloader.path=$LOADER_PATH"
fi

# Add JVM options
if [ -n "$ALL_JVM_OPTS" ]; then
    JAVA_CMD="$JAVA_CMD $ALL_JVM_OPTS"
fi

# Add JAR
JAVA_CMD="$JAVA_CMD -jar $JAR_FILE"

# Add application arguments
for arg in "${APP_ARGS[@]}"; do
    JAVA_CMD="$JAVA_CMD \"$arg\""
done

# Execute
eval exec $JAVA_CMD
