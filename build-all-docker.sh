#!/bin/bash
set -e

# Build the Docker image (skip if PRISM_SKIP_DOCKER_BUILD=1, e.g. when CI builds
# the image separately with layer caching via docker/build-push-action).
if [ -z "$PRISM_SKIP_DOCKER_BUILD" ]; then
  echo "Building Docker image 'prism-builder'..."
  docker build -t prism-builder -f Dockerfile.build .
else
  echo "Skipping Docker image build (PRISM_SKIP_DOCKER_BUILD is set)."
fi

# Run the build inside the Docker container.
# Mounts:
#   /app            — project source (bind mount)
#   /home/gradle    — all per-user caches; HOME is set to this path so that
#                     Gradle, Konan, and Maven Local all resolve correctly:
#     .gradle       — Gradle wrapper, dependency & build caches
#     .konan        — Kotlin/Native toolchain
#     .m2           — Maven Local (wgpu4k and webgpu-ktypes klibs/JARs)
echo "Running build for all targets (Linux, Windows, WASM, Android) in Docker..."

# Use current user's UID/GID to avoid permission issues with mounted volumes.
USER_ID=$(id -u)
GROUP_ID=$(id -g)

# Ensure cache directories exist and are owned by the current user before mounting.
# Docker creates missing bind-mount sources as root-owned, making them unwritable
# inside the container when running as a non-root UID.
mkdir -p "$HOME/.gradle" "$HOME/.konan" "$HOME/.m2" "$HOME/.android" "$HOME/.kotlin"

# Pass Android SDK into the container if available on the host.
ANDROID_ARGS=()
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
  ANDROID_ARGS=(-v "$ANDROID_HOME:$ANDROID_HOME:ro" -e "ANDROID_HOME=$ANDROID_HOME")
fi

# Build only the Linux/Windows/WASM outputs.
# Avoid 'assemble' — it triggers macosArm64 metadata resolution on modules that
# unconditionally declare that target, but macosArm64 wgpu4k klibs are only built
# on macOS runners. Native Apple targets are built by the Apple CI job instead.
docker run --rm \
    -u "$USER_ID:$GROUP_ID" \
    -v "$(pwd):/app" \
    -v "$HOME/.gradle:/home/gradle/.gradle" \
    -v "$HOME/.konan:/home/gradle/.konan" \
    -v "$HOME/.m2:/home/gradle/.m2" \
    -v "$HOME/.android:/home/gradle/.android" \
    -v "$HOME/.kotlin:/home/gradle/.kotlin" \
    "${ANDROID_ARGS[@]}" \
    -e HOME=/home/gradle \
    -e GRADLE_USER_HOME=/home/gradle/.gradle \
    -e KONAN_DATA_DIR=/home/gradle/.konan \
    prism-builder \
    :prism-native:linkReleaseSharedLinuxX64 \
    :prism-native:linkReleaseSharedMingwX64 \
    :prism-js:wasmJsBrowserDistribution \
    :prism-js:generateSdkTypes \
    :prism-android-demo:assembleDebug \
    --no-daemon
