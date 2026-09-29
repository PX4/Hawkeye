#!/usr/bin/env bash
#
# Publishes the draft release once every artifact is uploaded. Latest goes to final
# desktop releases only, since that is where the README's desktop download links point;
# Android releases and prereleases never take it.
#
# Usage: .github/scripts/release/publish.sh <tag> <desktop|android> <prerelease: true|false>

set -euo pipefail

TAG="$1"
SCOPE="$2"
PRERELEASE="$3"

LATEST=false
if [ "$SCOPE" = desktop ] && [ "$PRERELEASE" = false ]; then
    LATEST=true
fi

gh release edit "$TAG" --draft=false --latest="$LATEST"
