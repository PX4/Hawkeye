#!/usr/bin/env bash
#
# Builds hawkeye-<version>.tar.gz from the checked-out tree, submodules included, which is
# what the Homebrew formula installs from. GitHub's automatic source archive cannot stand
# in for it: it leaves out the lib/c_library_v2 submodule, and its checksum is not
# guaranteed to stay stable while the formula pins one.
#
# Writes sha256= to $GITHUB_OUTPUT when it is set.
#
# Usage: .github/scripts/desktop/make-source-tarball.sh <version>

set -euo pipefail

VERSION="$1"
TARBALL="hawkeye-${VERSION}.tar.gz"

# Built outside the tree so tar does not try to include its own output.
tar czf "/tmp/${TARBALL}" \
    --exclude='.git' \
    --exclude='tests/fixtures/*.ulg' \
    --exclude='build' \
    --transform="s,^\.,hawkeye-${VERSION}," \
    .
mv "/tmp/${TARBALL}" .

SHA=$(sha256sum "$TARBALL" | awk '{print $1}')
echo "Tarball SHA256: ${SHA}"
echo "sha256=${SHA}" >> "${GITHUB_OUTPUT:-/dev/stdout}"
