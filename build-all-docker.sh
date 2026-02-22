#!/bin/bash
set -e

# Build the Docker image
echo "Building Docker image 'prism-builder'..."
docker build -t prism-builder -f Dockerfile.build .

# Run the build inside the Docker container
# We mount the project root to /app
# We also mount the Gradle home and Konan (K/N toolchain) home to cache downloads
echo "Running build for all targets (Linux, Windows, WASM, Android) in Docker..."

# Use current user's UID/GID to avoid permission issues with mounted volumes
USER_ID=$(id -u)
GROUP_ID=$(id -g)

docker run --rm \
    -u "$USER_ID:$GROUP_ID" \
    -v "$(pwd):/app" \
    -v "$HOME/.gradle:/home/gradle/.gradle" \
    -v "$HOME/.konan:/home/gradle/.konan" \
    -e GRADLE_USER_HOME=/home/gradle/.gradle \
    -e KONAN_DATA_DIR=/home/gradle/.konan \
    prism-builder \
    assemble --no-daemon
