#!/usr/bin/env bash
#
# Installs the Linux build dependencies for a native amd64 build, or the aarch64 cross
# toolchain plus arm64 copies of the same libraries for a cross-compiled arm64 build.
#
# Usage: .github/scripts/desktop/install-linux-deps.sh <amd64|arm64>

set -euo pipefail

ARCH="$1"
LIBS=(libgl1-mesa-dev libx11-dev libxrandr-dev libxinerama-dev libxcursor-dev libxi-dev)

case "$ARCH" in
    amd64)
        sudo apt-get update
        sudo apt-get install -y "${LIBS[@]}"
        ;;
    arm64)
        sudo dpkg --add-architecture arm64
        # Pin all existing sources (DEB822 .sources and legacy .list) to amd64, since the
        # default mirrors do not carry arm64 and apt would fail fetching it from them.
        for f in /etc/apt/sources.list.d/*.sources; do
            [ -f "$f" ] && sudo sed -i '/^Architectures:/d; /^Types:/a Architectures: amd64' "$f" || true
        done
        for f in /etc/apt/sources.list.d/*.list /etc/apt/sources.list; do
            [ -f "$f" ] && sudo sed -i '/^\s*#/!{/\[arch=/!s/^deb /deb [arch=amd64] /}' "$f" || true
        done
        # arm64 packages come from the ports mirror instead.
        CODENAME=$(lsb_release -cs)
        printf "Types: deb\nURIs: http://ports.ubuntu.com/\nSuites: %s %s-updates\nComponents: main restricted universe multiverse\nArchitectures: arm64\n" \
            "$CODENAME" "$CODENAME" | sudo tee /etc/apt/sources.list.d/arm64-ports.sources
        sudo apt-get update
        sudo apt-get install -y gcc-aarch64-linux-gnu g++-aarch64-linux-gnu
        sudo apt-get install -y "${LIBS[@]/%/:arm64}"
        ;;
    *)
        echo "::error::Unknown architecture '$ARCH'; expected amd64 or arm64" >&2
        exit 1
        ;;
esac
