Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'build.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $projectRoot 'out'
[void](Get-Command java -ErrorAction Stop)
& java -cp $outputPath EnvironmentCheck
if ($LASTEXITCODE -ne 0) {
    throw "Environment check failed with exit code $LASTEXITCODE."
}
