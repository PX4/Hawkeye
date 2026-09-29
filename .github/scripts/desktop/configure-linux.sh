#!/usr/bin/env bash
#
# Configures the CMake build for a Linux .deb, natively for amd64 or cross-compiled for
# arm64 with the toolchain install-linux-deps.sh arm64 installs.
#
# Usage: .github/scripts/desktop/configure-linux.sh <amd64|arm64> <version>

set -euo pipefail

ARCH="$1"
VERSION="$2"

ARGS=(
    -DCMAKE_BUILD_TYPE=Release
    -DCMAKE_INSTALL_PREFIX=/usr
    -DHAWKEYE_VERSION="$VERSION"
)

case "$ARCH" in
    amd64) ;;
    arm64)
        ARGS+=(
            -DCMAKE_SYSTEM_NAME=Linux
            -DCMAKE_SYSTEM_PROCESSOR=aarch64
            -DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc
            -DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++
            "-DCMAKE_FIND_ROOT_PATH=/usr/aarch64-linux-gnu;/usr"
            -DCMAKE_FIND_ROOT_PATH_MODE_PROGRAM=NEVER
            -DCMAKE_FIND_ROOT_PATH_MODE_LIBRARY=ONLY
            -DCMAKE_FIND_ROOT_PATH_MODE_INCLUDE=BOTH
            -DCMAKE_LIBRARY_ARCHITECTURE=aarch64-linux-gnu
            -DCPACK_DEBIAN_PACKAGE_ARCHITECTURE=arm64
        )
        ;;
    *)
        echo "::error::Unknown architecture '$ARCH'; expected amd64 or arm64" >&2
        exit 1
        ;;
esac

cmake -B build "${ARGS[@]}"
