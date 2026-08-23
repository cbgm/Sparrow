$ErrorActionPreference = "Stop"

$root = (Get-Location).Path
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$payload = Join-Path $scriptRoot "payload"

$required = Join-Path $root "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\data\classifier\GeneratedMessageSafetyMlpModel.kt"
if (-not (Test-Path $required)) {
    throw "Run this from the Sparrow repository root after the MLP integration. Missing: $required"
}

Get-ChildItem -Path $payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($payload.Length).TrimStart('\', '/')
    $target = Join-Path $root $relative
    $targetDir = Split-Path -Parent $target
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Copy-Item $_.FullName $target -Force
}

$obsolete = @(
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\domain\model\MessageSafetyRisk.kt",
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\domain\resolver\MessageSafetyRiskResolver.kt",
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\domain\classifier\MessageSafetyClassifier.kt",
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\domain\analyzer\MessageSafetyStructuralAnalyzer.kt",
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\data\classifier\MessageSafetyClassifierPolicy.kt",
    "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\data\classifier\MessageSafetyClassifierPrototypes.kt",
    "feature\safety\src\commonTest\kotlin\com\cbgm\sparrow\feature\safety\data\classifier\MessageSafetyClassifierPolicyTest.kt",
    "feature\safety\src\commonTest\kotlin\com\cbgm\sparrow\feature\safety\domain\resolver\MessageSafetyRiskResolverTest.kt",
    "feature\safety\src\commonTest\kotlin\com\cbgm\sparrow\feature\safety\domain\analyzer\MessageSafetyStructuralAnalyzerTest.kt"
)

foreach ($relative in $obsolete) {
    $path = Join-Path $root $relative
    if (Test-Path $path) {
        Remove-Item $path -Force
    }
}

# Remove empty obsolete domain directories when possible.
@("resolver", "classifier", "analyzer") | ForEach-Object {
    $dir = Join-Path $root "feature\safety\src\commonMain\kotlin\com\cbgm\sparrow\feature\safety\domain\$_"
    if ((Test-Path $dir) -and -not (Get-ChildItem $dir -Force)) {
        Remove-Item $dir -Force
    }
}

Write-Host "Safety clean-architecture refactor applied."
Write-Host "Run: .\gradlew :feature:safety:allTests"
