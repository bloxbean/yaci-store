#!/bin/bash

# Absolute path of the script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
ENV_FILE="$ROOT_DIR/compose/.env"
COMPOSE_FILE="$ROOT_DIR/compose/admin-cli-compose.yml"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
  echo "❌ Docker is not installed or not found in PATH."
  echo "👉 Please install Docker: https://docs.docker.com/get-docker/"
  exit 1
fi

# Check if `docker compose` is available
if ! docker compose version &> /dev/null; then
  echo "❌ 'docker compose' command is not available."
  echo "👉 Make sure you're using Docker v20.10+ with the Compose plugin installed."
  exit 1
fi

# Load .env file if available
if [ -f "$ENV_FILE" ]; then
  echo "📦 Loading environment variables from .env"
  set -a
  source "$ENV_FILE"
  set +a
else
  echo "⚠️  .env file not found at: $ENV_FILE"
  echo "👉 Please create a .env file at the root with content like: tag=dev"
  exit 1
fi

# Check if rollback is being attempted
if [[ "$1" == rollback* ]]; then
  echo "⚠️  You are about to perform a rollback operation: '$1'"
  echo "🛑 Please make sure Yaci Store is stopped, but PostgreSQL is still running."

  # Check for interactive shell
  if [ -t 1 ]; then
    read -p "❓ Do you want to continue? [y/N]: " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
      echo "❌ Rollback cancelled by user."
      exit 1
    fi
  else
    echo "🔄 Non-interactive mode detected — proceeding without confirmation."
  fi
fi

# Run the admin CLI with all provided arguments
docker compose -f "$COMPOSE_FILE" run --rm admin-cli "$@"
exit $?

