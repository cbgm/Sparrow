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
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) "securechat-control-plane-bundle"
$bundleRoot = Join-Path $stagingRoot "securechat-control-plane"
$controlPlaneRoot = Join-Path $repositoryRoot "server/control-plane"
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
    "README.md"
)) {
    $sourcePath = Join-Path $controlPlaneRoot $relativePath

    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing control-plane bundle file: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $bundleRoot $relativePath) -Force
}

if (-not (Test-Path -LiteralPath $bootstrapSource -PathType Leaf)) {
    throw "Missing control-plane bootstrap source: $bootstrapSource"
}

Copy-Item `
    -LiteralPath $bootstrapSource `
    -Destination (Join-Path $bundleRoot "Bootstrap-ControlPlane.ps1") `
    -Force

[System.IO.File]::WriteAllLines(
    (Join-Path $bundleRoot "securechat.conf"),
    @(
        "# SecureChat control-plane configuration",
        "# MODE: lan or public",
        "# PUBLIC_DOMAIN: leave blank to derive <public-ip>.sslip.io in public mode",
        "MODE=lan",
        "PUBLIC_DOMAIN=",
        "SECURECHAT_IMAGE_PREFIX=$ImagePrefix",
        "SECURECHAT_IMAGE_TAG=$ImageTag"
    ),
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "Start-SecureChatControlPlane.cmd"),
    "@echo off`r`nsetlocal`r`ncd /d `"%~dp0`"`r`nstart `"`" powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"%~dp0Bootstrap-ControlPlane.ps1`"`r`nexit /b 0`r`n",
    [System.Text.UTF8Encoding]::new($false)
)

[System.IO.File]::WriteAllText(
    (Join-Path $bundleRoot "secrets/.gitignore"),
    "*`n!.gitignore`n",
    [System.Text.UTF8Encoding]::new($false)
)

foreach ($required in @("Start-SecureChatControlPlane.cmd", "securechat.conf")) {
    if (-not (Test-Path -LiteralPath (Join-Path $bundleRoot $required) -PathType Leaf)) {
        throw "Control-plane bundle is missing $required."
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $outputPath "securechat-control-plane.zip"
Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $bundleRoot,
    $archivePath,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false
)

$hash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText(
    (Join-Path $outputPath "SHA256SUMS.txt"),
    "$hash  securechat-control-plane.zip`n",
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Created $archivePath"
