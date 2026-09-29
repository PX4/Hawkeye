#!/usr/bin/env bash
#
# Derives the release scope, version, and prerelease flag from a release tag.
#
#   desktop-v<version>  desktop release: source tarball, bottle, .debs, Windows zip, tap
#   android-v<version>  Android release: APK on a GitHub release, AAB to Google Play
#
# A bare v<version> tag is rejected. It used to release everything at once, and still
# reaching this script lets it fail with a pointer to the two prefixes rather than doing
# nothing and leaving whoever pushed it to wonder why.
#
# Writes scope=, version=, and prerelease= lines to $GITHUB_OUTPUT when it is set, and to
# stdout otherwise, so it can be run locally to check what a tag would do:
#
#   .github/scripts/release/get-release-versions.sh android-v1.0.1-rc1

set -euo pipefail

TAG="${1:-${GITHUB_REF_NAME:?pass a tag or set GITHUB_REF_NAME}}"

case "$TAG" in
    desktop-v*) SCOPE=desktop; VERSION="${TAG#desktop-v}" ;;
    android-v*) SCOPE=android; VERSION="${TAG#android-v}" ;;
    *)
        echo "::error::'$TAG' is not a release tag. Tag desktop-v<version> for the desktop builds or android-v<version> for the Android app; see docs/developer/releasing.md." >&2
        exit 1
        ;;
esac

if [ -z "$VERSION" ]; then
    echo "::error::'$TAG' has no version after its prefix" >&2
    exit 1
fi

# gh release create infers nothing from the tag name, so without this an rc tag publishes
# as a full release and GitHub serves it as Latest to everyone. Any suffix after the
# patch version marks a prerelease; a bare MAJOR.MINOR.PATCH is the only final form.
case "$VERSION" in
    *-*) PRERELEASE=true ;;
    *)   PRERELEASE=false ;;
esac

{
    echo "scope=${SCOPE}"
    echo "version=${VERSION}"
    echo "prerelease=${PRERELEASE}"
} >> "${GITHUB_OUTPUT:-/dev/stdout}"
