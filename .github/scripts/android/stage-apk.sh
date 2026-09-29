#!/usr/bin/env bash
#
# Verifies the release APK and copies it to its release asset name. A build signed with
# the upload key (HAWKEYE_UPLOAD_KEYSTORE set) is checked with --signed and named
# hawkeye-<version>-android.apk; an unsigned fork build is named -android-unsigned.apk so
# nobody mistakes it for an installable one.
#
# Writes file= to $GITHUB_OUTPUT when it is set.
#
# Usage: .github/scripts/android/stage-apk.sh <version>

set -euo pipefail

VERSION="$1"
APK_DIR=android/app/build/outputs/apk/release

if [ -n "${HAWKEYE_UPLOAD_KEYSTORE:-}" ]; then
    SRC=$(android/scripts/verify-release-apk.sh "$APK_DIR" "$VERSION" --signed)
    NAME="hawkeye-${VERSION}-android.apk"
else
    SRC=$(android/scripts/verify-release-apk.sh "$APK_DIR" "$VERSION")
    NAME="hawkeye-${VERSION}-android-unsigned.apk"
fi

cp "$SRC" "$NAME"
echo "file=${NAME}" >> "${GITHUB_OUTPUT:-/dev/stdout}"
