# Stages the Windows build into hawkeye-<version>-windows-x64/ with its data directories
# and license files, and zips it for the release.
#
# Usage: .github/scripts/desktop/stage-windows-zip.ps1 -Version <version>

param([Parameter(Mandatory)][string]$Version)

$ErrorActionPreference = 'Stop'

$stage = "hawkeye-$Version-windows-x64"
New-Item -ItemType Directory -Path $stage | Out-Null

Copy-Item "build\Release\hawkeye.exe" $stage
foreach ($dir in 'models', 'shaders', 'fonts', 'textures', 'themes') {
    Copy-Item -Recurse "build\Release\$dir" $stage
}
foreach ($file in 'LICENSE', 'NOTICE.md', 'README.md') {
    Copy-Item $file $stage
}

Compress-Archive -Path $stage -DestinationPath "$stage.zip"
