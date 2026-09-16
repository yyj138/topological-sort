Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $projectRoot 'src'
$outputPath = Join-Path $projectRoot 'out'
$sourceFiles = @(Get-ChildItem -LiteralPath $sourcePath -Recurse -Filter '*.java' -File | ForEach-Object { $_.FullName })

if ($sourceFiles.Count -eq 0) {
    throw 'No Java source files were found.'
}

[void](Get-Command javac -ErrorAction Stop)
[void](New-Item -ItemType Directory -Path $outputPath -Force)
& javac -encoding UTF-8 --release 21 -d $outputPath $sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed with exit code $LASTEXITCODE."
}

Write-Output "Build passed. Output: $outputPath"
