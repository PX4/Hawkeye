# Checks the Windows build produced hawkeye.exe and that it runs.
#
# Usage: .github/scripts/desktop/smoke-test-windows.ps1

$ErrorActionPreference = 'Stop'

$exe = "build\Release\hawkeye.exe"
if (-not (Test-Path $exe)) { throw "hawkeye.exe not produced" }
& $exe --help
if ($LASTEXITCODE -ne 0) { throw "hawkeye --help exited $LASTEXITCODE" }
