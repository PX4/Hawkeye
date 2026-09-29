#!/usr/bin/env bash
#
# Installs the freshly built .deb and checks the binary and its data directories landed.
# Only meaningful on a runner that can execute the package's architecture.
#
# Usage: .github/scripts/desktop/smoke-test-linux-deb.sh

set -euo pipefail

sudo dpkg -i build/*.deb
command -v hawkeye
for dir in models shaders fonts; do
    ls "/usr/share/hawkeye/${dir}/"
done
