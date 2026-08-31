#!/usr/bin/env bash
#
# Verifies a release AAB without needing bundletool or a device.
#
# Checks that both ABIs, all four asset trees, and the notice file made it into the
# bundle (the assets are symlinks into the repo root, so this catches a checkout that
# failed to materialize them). Version checks live in verify-release-apk.sh, which reads
# them from the same Gradle invocation that produced this bundle.
#
# With --signed, additionally requires a verifiable jar signature. Bundles are jar-signed
# with the upload key, and Google Play rejects an AAB whose signature does not match the
# registered upload certificate, so this is the check that has to pass before an upload.
#
# Usage:   verify-release-bundle.sh <path-to-aab> [--signed]
# Example: android/scripts/verify-release-bundle.sh android/app/build/outputs/bundle/release/app-release.aab
#
# Prints the bundle path on stdout; diagnostics go to stderr.

set -euo pipefail

usage() {
    echo "usage: $0 <path-to-aab> [--signed]" >&2
    exit 2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    usage
fi

aab=$1
signed=0
if [ "$#" -eq 2 ]; then
    [ "$2" = "--signed" ] || usage
    signed=1
fi

if [ ! -f "$aab" ]; then
    echo "no bundle at ${aab}; was bundleRelease run?" >&2
    exit 1
fi

# -Z1 lists bare entry names only; a plain -l listing includes an "Archive: <path>"
# header, which lets the bundle's own on-disk path satisfy an entry check.
listing=$(unzip -Z1 "$aab")
for entry in \
    base/lib/arm64-v8a/libhawkeye.so \
    base/lib/x86_64/libhawkeye.so \
    base/assets/models/ \
    base/assets/shaders/ \
    base/assets/fonts/ \
    base/assets/themes/ \
    base/assets/NOTICE.md ; do
    # Herestring rather than a pipe, for the same pipefail reason as in
    # verify-release-apk.sh.
    if ! grep -qF "$entry" <<<"$listing"; then
        echo "bundle is missing ${entry}" >&2
        exit 1
    fi
done

if [ "$signed" -eq 1 ]; then
    if command -v jarsigner >/dev/null 2>&1; then
        jarsigner=jarsigner
    elif [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/jarsigner" ]; then
        jarsigner="${JAVA_HOME}/bin/jarsigner"
    else
        echo "--signed needs jarsigner on PATH or JAVA_HOME pointing at a JDK" >&2
        exit 1
    fi
    # jarsigner exits 0 even for an unsigned jar (it just prints "jar is unsigned."),
    # so the verdict line is the actual check. -strict is avoided deliberately: it
    # fails on the self-signed certificate every Android upload key has.
    verdict=$("$jarsigner" -verify "$aab")
    printf '%s\n' "$verdict" >&2
    if ! grep -qF "jar verified" <<<"$verdict"; then
        echo "bundle is unsigned or its signature does not verify" >&2
        exit 1
    fi
fi

echo "${aab}: both ABIs, all four asset trees, and the notice file present" >&2
echo "$aab"
