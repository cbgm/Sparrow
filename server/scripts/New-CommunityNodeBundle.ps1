[CmdletBinding()]
param(
    [string]$OutputDirectory = "dist",
    [string]$ImagePrefix = "ghcr.io/cbgm/securechat",
    [string]$ImageTag = "latest"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) "securechat-community-node-bundle"
$bundleRoot = Join-Path $stagingRoot "securechat-community-node"
$communityNodeRoot = Join-Path $repositoryRoot "server/community-node"

if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') {
    throw "ImagePrefix is not a valid lowercase container-image prefix."
}
if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "ImageTag is not a valid container-image tag."
}

$bundleFiles = @(
    "docker-compose.yml",
    "docker-compose.release.yml",
    "docker-compose.production.yml",
    "Caddyfile",
    "index.html",
    "Bootstrap-CommunityNode.ps1",
    "Start-SecureChatNode.cmd",
    "bootstrap-community-node.sh",
    "start-securechat-node.sh",
    "Start-SecureChatNode.command",
    "README.md"
)

Remove-Item -LiteralPath $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $outputPath -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

foreach ($relativePath in $bundleFiles) {
    $sourcePath = Join-Path $communityNodeRoot $relativePath
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Deployment bundle source file does not exist: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $bundleRoot $relativePath) -Force
}

[System.IO.File]::WriteAllLines(
    (Join-Path $bundleRoot "securechat.conf"),
    @(
        "# SecureChat community-node configuration",
        "# The launcher opens configuration on every start, prefilled from this file, and writes any changes back here.",
        "CONFIGURED=false",
        "MODE=",
        "PUBLIC_DOMAIN=",
        "CONTROL_PLANE_DIRECTORY_URL=",
        "SECURECHAT_IMAGE_PREFIX=$ImagePrefix",
        "SECURECHAT_IMAGE_TAG=$ImageTag"
    ),
    [System.Text.UTF8Encoding]::new($false)
)

New-Item -ItemType Directory -Path (Join-Path $bundleRoot "secrets") -Force | Out-Null

foreach ($required in @("Start-SecureChatNode.cmd", "securechat.conf", "index.html")) {
    if (-not (Test-Path -LiteralPath (Join-Path $bundleRoot $required) -PathType Leaf)) {
        throw "Community-node bundle is missing $required."
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archivePath = Join-Path $outputPath "securechat-community-node.zip"
Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $bundleRoot,
    $archivePath,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false
)

$tarArchivePath = Join-Path $outputPath "securechat-community-node.tar.gz"
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
        "$zipChecksum  securechat-community-node.zip",
        "$tarChecksum  securechat-community-node.tar.gz"
    ),
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Created $archivePath"
Write-Host "Created $tarArchivePath"
