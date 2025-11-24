#!/bin/bash
set -e

# ------------------------------------------------------------------------------
# build.sh
#
# This script builds Docker images for the Yaci Store project.
#
# Usage:
#   ./build.sh [tag]
#
# If no tag is provided, it defaults to "dev".
# Example:
#   ./build.sh v2.0.0-beta3
#
# The script builds two images:
#   1. bloxbean/yaci-store:<tag>              – The main Yaci Store image
#   2. bloxbean/yaci-store-admin-cli:<tag>    – The admin CLI image
#
# ------------------------------------------------------------------------------

# Get tag argument or default to 'dev'
TAG=${1:-dev}

echo "Building Docker images with tag: $TAG"

docker build --target yaci-store -t bloxbean/yaci-store:$TAG .
docker build --target yaci-store-admin-cli -t bloxbean/yaci-store-admin-cli:$TAG .
