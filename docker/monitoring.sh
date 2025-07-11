#!/bin/bash

# Get the script's absolute path
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
COMPOSE_FILE="$ROOT_DIR/compose/monitoring.yml"
ENV_FILE="$ROOT_DIR/.env" # optional

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

ACTION=$1

if [ -z "$ACTION" ]; then
  echo "Usage: $0 [start|stop]"
  exit 1
fi

if [ "$ACTION" = "start" ]; then
  echo "🚀 Starting Prometheus and Grafana monitoring services..."
  docker compose -f "$COMPOSE_FILE" up -d
  STATUS=$?
  if [ $STATUS -eq 0 ]; then
    echo "✅ Monitoring services started."
  else
    echo "❌ Failed to start monitoring. Exit code: $STATUS"
  fi
  exit $STATUS

elif [ "$ACTION" = "stop" ]; then
  echo "🛑 Stopping monitoring services..."
  docker compose -f "$COMPOSE_FILE" down
  STATUS=$?
  if [ $STATUS -eq 0 ]; then
    echo "✅ Monitoring services stopped."
  else
    echo "❌ Failed to stop monitoring. Exit code: $STATUS"
  fi
  exit $STATUS

else
  echo "❌ Invalid action: $ACTION"
  echo "Usage: $0 [start|stop]"
  exit 1
fi
