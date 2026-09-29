#!/usr/bin/env bash
#
# Packages the bottle built by `brew install --build-bottle PX4/px4/hawkeye`, with its
# root URL pointing at the release it will be uploaded to.
#
# Writes file=, sha256=, and tag= (the bottle tag, such as arm64_sonoma) to
# $GITHUB_OUTPUT when it is set.
#
# Usage: .github/scripts/homebrew/package-bottle.sh <release tag>

set -euo pipefail

TAG="$1"

brew bottle \
    --json \
    --root-url="https://github.com/PX4/Hawkeye/releases/download/${TAG}" \
    PX4/px4/hawkeye

BOTTLE_JSON=$(ls hawkeye--*.bottle.json)
BOTTLE_FILE=$(ls hawkeye--*.bottle.tar.gz)
# brew bottle writes name--version; the formula expects name-version at the root URL.
CLEAN_NAME="${BOTTLE_FILE/--/-}"
mv "$BOTTLE_FILE" "$CLEAN_NAME"

BOTTLE_TAG=$(jq -r '.[].bottle.tags | keys[0]' "$BOTTLE_JSON")
BOTTLE_SHA=$(jq -r --arg t "$BOTTLE_TAG" '.[].bottle.tags[$t].sha256' "$BOTTLE_JSON")

{
    echo "file=${CLEAN_NAME}"
    echo "sha256=${BOTTLE_SHA}"
    echo "tag=${BOTTLE_TAG}"
} >> "${GITHUB_OUTPUT:-/dev/stdout}"
