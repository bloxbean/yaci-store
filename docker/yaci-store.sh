#!/bin/bash

# Get the absolute path of the script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Root folder of the extracted zip (where .env and compose/config exist)
ROOT_DIR="$SCRIPT_DIR"

# Compose file location
COMPOSE_FILE="$ROOT_DIR/compose/yaci-store-monolith.yml"
ENV_FILE="$ROOT_DIR/compose/.env"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
  echo "❌ Docker is not installed or not found in PATH."
  echo "👉 Please install Docker: https://docs.docker.com/get-docker/"
  exit 1
fi

# Check if `docker compose` is available
if ! docker compose version &> /dev/null; then
  echo "❌ 'docker compose' command is not available."
  echo "👉 Make sure you're using Docker with the Compose plugin installed."
  exit 1
fi

ACTION=$1

if [ -z "$ACTION" ]; then
  echo "Usage: $0 [start|stop|stop:yaci-store|logs|logs:yaci-store|logs:db]"
  exit 1
fi

# Load .env variables if present
if [ -f "$ENV_FILE" ]; then
  echo "📦 Loading environment variables from .env"
  set -a
  source "$ENV_FILE"
  set +a
else
  echo "⚠️  .env file not found at: $ENV_FILE"
  echo "👉 Please create a .env file at the root with content like: tag=<version>"
  exit 1
fi

if [ "$ACTION" = "start" ]; then
  echo "🚀 Starting Yaci Store in detached mode..."
  docker compose -f "$COMPOSE_FILE" up -d
  STATUS=$?

  if [ $STATUS -eq 0 ]; then
    echo "✅ Yaci Store started successfully (in background)."
    echo "📋 View logs: ./yaci-store.sh logs"
  else
    echo "❌ Failed to start Yaci Store. Exit code: $STATUS"

    # Check for container name conflict
    CONFLICT_MSG=$(docker compose -f "$COMPOSE_FILE" ps 2>&1 | grep "is already in use")
    if [[ "$CONFLICT_MSG" == *"is already in use"* ]]; then
      echo ""
      echo "⚠️  It looks like a container with the name is already running or exists."
      echo "👉 Run the following to remove the existing container and try again:"
      echo ""
      echo "   docker rm yaci-store"
      echo ""
      echo "⚠️  Or use 'docker ps -a' to inspect and clean up existing containers."
     fi
  fi

  exit $STATUS

elif [ "$ACTION" = "stop" ]; then
  echo "🛑 Stopping Yaci Store..."
  docker compose -f "$COMPOSE_FILE" down
  STATUS=$?

  if [ $STATUS -eq 0 ]; then
    echo "✅ Yaci Store stopped successfully."
  else
    echo "❌ Failed to stop Yaci Store. Exit code: $STATUS"
  fi

  exit $STATUS

elif [ "$ACTION" = "stop:yaci-store" ]; then
  echo "🛑 Stopping only the Yaci Store container (PostgreSQL will keep running)..."
  docker compose -f "$COMPOSE_FILE" stop yaci-store
  STATUS=$?

  if [ $STATUS -eq 0 ]; then
    echo "✅ Yaci Store container stopped. PostgreSQL is still running."
  else
    echo "❌ Failed to stop Yaci Store. Exit code: $STATUS"
  fi

  exit $STATUS

elif [ "$ACTION" = "logs" ]; then
  echo "📋 Showing logs for Yaci Store and PostgreSQL (press Ctrl+C to exit)"
  docker compose -f "$COMPOSE_FILE" logs -f
  exit $?

elif [ "$ACTION" = "logs:yaci-store" ]; then
  echo "📋 Showing logs for Yaci Store only (press Ctrl+C to exit)"
  docker compose -f "$COMPOSE_FILE" logs -f yaci-store
  exit $?

elif [ "$ACTION" = "logs:db" ]; then
  echo "📋 Showing logs for PostgreSQL only (press Ctrl+C to exit)"
  docker compose -f "$COMPOSE_FILE" logs -f yaci-store-postgres
  exit $?

else
  echo "❌ Invalid action: $ACTION"
  echo "Usage: $0 [start|stop|stop:yaci-store|logs|logs:yaci-store|logs:db]"
  exit 1
fi
