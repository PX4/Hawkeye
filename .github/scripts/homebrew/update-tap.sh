#!/usr/bin/env bash
#
# Points the hawkeye formula in PX4/homebrew-px4 at a published release's source tarball
# and bottle. Run only after the release is published: the formula uses its public
# download URLs, which do not resolve while it is a draft.
#
# Usage: .github/scripts/homebrew/update-tap.sh <release tag> <version> <tarball sha256> <bottle tag> <bottle sha256>
# Requires TAP_TOKEN, a token with push access to PX4/homebrew-px4.

set -euo pipefail

TAG="$1"
VERSION="$2"
TARBALL_SHA="$3"
BOTTLE_TAG="$4"
BOTTLE_SHA="$5"
: "${TAP_TOKEN:?TAP_TOKEN must be set}"

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
RELEASE_URL="https://github.com/PX4/Hawkeye/releases/download/${TAG}"

git clone "https://x-access-token:${TAP_TOKEN}@github.com/PX4/homebrew-px4.git" tap

python3 "$SCRIPTS/render-formula.py" \
    --url "${RELEASE_URL}/hawkeye-${VERSION}.tar.gz" \
    --sha256 "$TARBALL_SHA" \
    --bottle-root-url "$RELEASE_URL" \
    --bottle-tag "$BOTTLE_TAG" \
    --bottle-sha256 "$BOTTLE_SHA" \
    > tap/Formula/hawkeye.rb

cd tap
git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add Formula/hawkeye.rb
git commit -m "hawkeye ${VERSION}"
git push
