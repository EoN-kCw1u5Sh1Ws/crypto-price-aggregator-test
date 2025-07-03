#!/bin/sh
if command -v podman >/dev/null 2>&1; then
    podman run --rm --env-file .env -p 8080:8080 -d crypto-price-aggregator:distroless
elif command -v docker >/dev/null 2>&1; then
    docker run --rm --env-file .env -p 8080:8080 -d crypto-price-aggregator:distroless
else
    echo "Error: Neither podman nor docker found"
    exit 1
fi