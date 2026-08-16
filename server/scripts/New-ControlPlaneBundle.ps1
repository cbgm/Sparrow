[CmdletBinding()]
param(
    [string]$OutputDirectory = "dist-control-plane",

    [Parameter(Mandatory = $true)]
    [string]$ImagePrefix,

    [Parameter(Mandatory = $true)]
    [string]$ImageTag
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) "sparrow-control-plane-bundle"
$bundleRoot = Join-Path $stagingRoot "sparrow-control-plane"
$controlPlaneRoot = Join-Path $repositoryRoot "server/control-plane"
$controlPlaneDocumentation = Join-Path $repositoryRoot "docs/server/control-plane.md"
$bootstrapSource = Join-Path $PSScriptRoot "Bootstrap-ControlPlane.Bundle.ps1"

if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') {
    throw "ImagePrefix is not a valid lowercase container-image prefix."
}
if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "ImageTag is not a valid container-image tag."
}

Remove-Item -LiteralPath $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $outputPath -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot "secrets") -Force | Out-Null
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

foreach ($relativePath in @(
    "docker-compose.yml",
    "docker-compose.release.yml",
    "docker-compose.production.yml",
    "Caddyfile",
    "index.html"
)) {
    $sourcePath = Join-Path $controlPlaneRoot $relativePath

    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing control-plane bundle file: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $bundleRoot $relativePath) -Force
}

if (-not (Test-Path -LiteralPath $controlPlaneDocumentation -PathType Leaf)) {
    throw "Missing central control-plane documentation: $controlPlaneDocumentation"
}

Copy-Item `
    -LiteralPath $controlPlaneDocumentation `
    -Destination (Join-Path $bundleRoot "README.md") `
    -Force

if (-not (Test-Path -LiteralPath $bootstrapSource -PathType Leaf)) {
    throw "Missing control-plane bootstrap source: $bootstrapSource"
}

Copy-Item `
    -LiteralPath $bootstrapSource `
    -Destination (Join-Path $bundleRoot "Bootstrap-ControlPlane.ps1") `
    -Force

foreach ($unixLauncher in @(
    "bootstrap-control-plane.sh",
    "start-sparrow-control-plane.sh",
    "Start-SparrowControlPlane.command"
)) {
    $sourcePath = Join-Path $controlPlaneRoot $unixLauncher
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing Control Plane macOS/Linux launcher: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $bundleRoot $unixLauncher) -Force
}

[System.IO.File]::WriteAllLines(
    (Join-Path $bundleRoot "sparrow.conf"),
    @(
        "# Sparrow control-plane configuration",
        "# The launcher opens configuration on every start, prefilled from this file, and writes any changes back here.",
        "CONFIGURED=false",
        "MODE=",
        "PUBLIC_DOMAIN=",
        "CONTROL_PLANE_ID=",
        "SPARROW_IMAGE_PREFIX=$ImagePrefix",
        "SPARROW_IMAGE_TAG=$ImageTag"
    ),
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "Start-SparrowControlPlane.cmd"),
    "@echo off`r`nsetlocal`r`ncd /d `"%~dp0`"`r`nstart `"`" powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"%~dp0Bootstrap-ControlPlane.ps1`"`r`nexit /b 0`r`n",
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "secrets/.gitignore"),
    "*`n!.gitignore`n",
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "secrets/README.txt"),
    "Place firebase-admin.json in this folder before first start.`nThe launcher automatically generates the registry authority and remaining generated secret files here.`n",
    [System.Text.UTF8Encoding]::new($false)
)

foreach ($required in @(
    "Start-SparrowControlPlane.cmd",
    "bootstrap-control-plane.sh",
    "start-sparrow-control-plane.sh",
    "Start-SparrowControlPlane.command",
    "sparrow.conf",
    "index.html",
    "secrets/README.txt"
)) {
    if (-not (Test-Path -LiteralPath (Join-Path $bundleRoot $required) -PathType Leaf)) {
        throw "Control-plane bundle is missing $required."
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $outputPath "sparrow-control-plane.zip"
Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $bundleRoot,
    $archivePath,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false
)

$tarArchivePath = Join-Path $outputPath "sparrow-control-plane.tar.gz"
Remove-Item -LiteralPath $tarArchivePath -Force -ErrorAction SilentlyContinue
& tar -czf $tarArchivePath -C $bundleRoot .
if ($LASTEXITCODE -ne 0) {
    throw "Could not create $tarArchivePath"
}

$zipChecksum = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
$tarChecksum = (Get-FileHash -LiteralPath $tarArchivePath -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllLines(
    (Join-Path $outputPath "SHA256SUMS.txt"),
    @(
        "$zipChecksum  sparrow-control-plane.zip",
        "$tarChecksum  sparrow-control-plane.tar.gz"
    ),
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Created $archivePath"
Write-Host "Created $tarArchivePath"
