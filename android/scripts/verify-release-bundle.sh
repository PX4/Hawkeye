#!/usr/bin/env bash
#
# Verifies a release AAB without needing bundletool or a device.
#
# Checks that both ABIs and all four asset trees made it into the bundle (the assets
# are symlinks into the repo root, so this catches a checkout that failed to
# materialize them). Version checks live in verify-release-apk.sh, which reads them
# from the same Gradle invocation that produced this bundle.
#
# Usage:   verify-release-bundle.sh <path-to-aab>
# Example: android/scripts/verify-release-bundle.sh android/app/build/outputs/bundle/release/app-release.aab
#
# Prints the bundle path on stdout; diagnostics go to stderr.

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: $0 <path-to-aab>" >&2
    exit 2
fi

aab=$1

if [ ! -f "$aab" ]; then
    echo "no bundle at ${aab}; was bundleRelease run?" >&2
    exit 1
fi

listing=$(unzip -l "$aab")
for entry in \
    base/lib/arm64-v8a/libhawkeye.so \
    base/lib/x86_64/libhawkeye.so \
    base/assets/models/ \
    base/assets/shaders/ \
    base/assets/fonts/ \
    base/assets/themes/ ; do
    # Herestring rather than a pipe, for the same pipefail reason as in
    # verify-release-apk.sh.
    if ! grep -qF "$entry" <<<"$listing"; then
        echo "bundle is missing ${entry}" >&2
        exit 1
    fi
done

echo "${aab}: both ABIs and all four asset trees present" >&2
echo "$aab"
