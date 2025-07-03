#!/bin/sh

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Go to the project root (one level up from container-scripts)
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

if command -v podman >/dev/null 2>&1; then
    podman build -t crypto-price-aggregator:distroless .
elif command -v docker >/dev/null 2>&1; then
    docker build -t crypto-price-aggregator:distroless .
else
    echo "Error: Neither podman nor docker found"
    exit 1
fi