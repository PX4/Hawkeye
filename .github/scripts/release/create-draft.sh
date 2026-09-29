#!/usr/bin/env bash
#
# Creates the draft GitHub release for a release tag. Every release starts as a draft and
# is published by publish.sh only once all of its artifacts are uploaded.
#
# A draft left behind by an earlier failed run is reused, so re-running the whole
# workflow works; a release that is already published is never touched.
#
# Usage: .github/scripts/release/create-draft.sh <tag> <desktop|android> <version> <prerelease: true|false>

set -euo pipefail

TAG="$1"
SCOPE="$2"
VERSION="$3"
PRERELEASE="$4"

if IS_DRAFT=$(gh release view "$TAG" --json isDraft --jq .isDraft 2>/dev/null); then
    if [ "$IS_DRAFT" = true ]; then
        echo "Reusing existing draft release for $TAG"
        exit 0
    fi
    echo "::error::Release $TAG is already published; refusing to modify it" >&2
    exit 1
fi

case "$SCOPE" in
    desktop) TITLE="Desktop v${VERSION}" ;;
    android) TITLE="Android v${VERSION}" ;;
    *) echo "::error::Unknown release scope '$SCOPE'" >&2; exit 1 ;;
esac

gh release create "$TAG" \
    --draft \
    --verify-tag \
    --title "$TITLE" \
    --generate-notes \
    --prerelease="$PRERELEASE"
