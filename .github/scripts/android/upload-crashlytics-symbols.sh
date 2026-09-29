#!/usr/bin/env bash
#
# Uploads the release build's native symbols to Crashlytics, so native crashes arrive
# symbolicated rather than as raw addresses. Run from android/, in the job that built the
# release, since the unstripped libhawkeye.so only exists in that build tree.
#
# Usage: .github/scripts/android/upload-crashlytics-symbols.sh <version>
# Requires FIREBASE_SERVICE_ACCOUNT_JSON, a service account key for the Firebase project.

set -euo pipefail

VERSION="$1"
: "${FIREBASE_SERVICE_ACCOUNT_JSON:?FIREBASE_SERVICE_ACCOUNT_JSON must be set}"

KEY="${RUNNER_TEMP:-/tmp}/firebase-sa.json"
# Trap rather than a trailing rm, so the key is deleted even when the upload fails and the
# shell exits early.
trap 'rm -f "$KEY"' EXIT
printf '%s' "$FIREBASE_SERVICE_ACCOUNT_JSON" > "$KEY"

GOOGLE_APPLICATION_CREDENTIALS="$KEY" \
    ./gradlew uploadCrashlyticsSymbolFileRelease -PhawkeyeVersionName="$VERSION"
