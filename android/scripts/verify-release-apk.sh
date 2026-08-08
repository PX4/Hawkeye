#!/usr/bin/env bash
#
# Verifies a release APK without needing a device or an emulator.
#
# Checks that both ABIs are present, that all four asset trees made it into the package
# (they are symlinks into the repo root, so this catches a checkout that failed to
# materialize them), and that the version the build produced is the one it was told to
# produce.
#
# Resolves the APK from AGP's output-metadata.json rather than globbing the output
# directory, so it stays correct if a second variant or an ABI split is ever added.
#
# Usage:   verify-release-apk.sh <apk-output-dir> <expected-version-name>
# Example: android/scripts/verify-release-apk.sh android/app/build/outputs/apk/release 0.4.0
#
# Prints the resolved APK path on stdout; diagnostics go to stderr.

set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: $0 <apk-output-dir> <expected-version-name>" >&2
    exit 2
fi

output_dir=$1
expected_version=$2
metadata="${output_dir}/output-metadata.json"

if [ ! -f "$metadata" ]; then
    echo "no output-metadata.json in ${output_dir}; was the APK built?" >&2
    exit 1
fi

element_count=$(jq '.elements | length' "$metadata")
if [ "$element_count" -ne 1 ]; then
    echo "expected exactly one APK output, found ${element_count}" >&2
    echo "if a split or flavor was added, this script needs to learn which one to ship" >&2
    exit 1
fi

apk="${output_dir}/$(jq -r '.elements[0].outputFile' "$metadata")"
if [ ! -f "$apk" ]; then
    echo "output-metadata.json names ${apk}, which does not exist" >&2
    exit 1
fi

listing=$(unzip -l "$apk")
for entry in \
    lib/arm64-v8a/libhawkeye.so \
    lib/x86_64/libhawkeye.so \
    assets/models/ \
    assets/shaders/ \
    assets/fonts/ \
    assets/themes/ ; do
    # Herestring rather than a pipe: grep -q exits on the first match, and under
    # pipefail a writer killed by the resulting SIGPIPE would fail the pipeline and
    # report a present entry as missing once the listing outgrows the pipe buffer.
    if ! grep -qF "$entry" <<<"$listing"; then
        echo "APK is missing ${entry}" >&2
        exit 1
    fi
done

version_name=$(jq -r '.elements[0].versionName' "$metadata")
version_code=$(jq -r '.elements[0].versionCode' "$metadata")
echo "${apk}: versionName=${version_name} versionCode=${version_code}" >&2

if [ "$version_name" != "$expected_version" ]; then
    echo "versionName is '${version_name}', expected '${expected_version}'" >&2
    exit 1
fi

if [ "$version_code" -lt 1 ]; then
    echo "versionCode is '${version_code}', which Android will not accept" >&2
    exit 1
fi

echo "$apk"
