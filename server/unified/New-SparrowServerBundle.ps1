[CmdletBinding()]
param(
    [string]$OutputDirectory = 'dist',
    [string]$ImagePrefix = 'ghcr.io/cbgm/sparrow',
    [string]$ImageTag = 'latest'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$bundleRoot = Join-Path ([System.IO.Path]::GetTempPath()) 'sparrow-unified-server-bundle'
$dest = Join-Path ([System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))) 'sparrow-server.zip'
if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') { throw 'Invalid image prefix.' }
if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') { throw 'Invalid image tag.' }
if (Test-Path -LiteralPath $bundleRoot) { Remove-Item -LiteralPath $bundleRoot -Recurse -Force }
New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null

# Runtime-only bundle: optional diagnostic scripts are deliberately excluded.
# Do not require deleted Test-* files when producing the end-user installer.
$sharedFiles = @(
    'Invoke-SparrowServer.ps1', 'Get-SparrowDockerLabel.ps1',
    'Get-SparrowVerifiedDirectory.ps1',
    'Start-SparrowServer.ps1', 'Backup-SparrowDeployment.ps1',
    'Stage-SparrowPublicTls.ps1', 'Invoke-SparrowPublicCutover.ps1',
    'Attached-SparrowDeployment.ps1', 'Update-SparrowServerBundle.ps1',
    'Update-SparrowFromGitHub.ps1',
    'Invoke-SparrowServer.py', 'Manage-SparrowNodes.py', 'Manage-SparrowNodes.ps1', 'control_plane_directory_client.py', 'control_plane_directory_registration.py', 'control_plane_directory_sync.py', '.dockerignore', 'Start-SparrowServer.sh',
    'Start-SparrowServer.command'
)
foreach ($name in $sharedFiles) {
    $source = Join-Path $PSScriptRoot $name
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Required installer file missing: $source" }
    Copy-Item -LiteralPath $source -Destination (Join-Path $bundleRoot $name) -Force
}
# The fresh-reinstall helper is an OPERATION, not a diagnostic script. Keep
# the installed bundle free of misleading legacy filenames without changing
# the source helper (and therefore without risking its destructive semantics).
$resetSource = Join-Path $PSScriptRoot 'Reset-SparrowTestDeployment.ps1'
if (-not (Test-Path -LiteralPath $resetSource -PathType Leaf)) {
    throw 'Required fresh-reinstall helper missing. Restore the server reset helper in the source project before building.'
}
Copy-Item -LiteralPath $resetSource -Destination (Join-Path $bundleRoot 'Reset-SparrowDeployment.ps1') -Force
$managerFile = Join-Path $bundleRoot 'Invoke-SparrowServer.ps1'
$managerContent = [System.IO.File]::ReadAllText($managerFile)
if ($managerContent.IndexOf('Reset-SparrowTestDeployment.ps1', [StringComparison]::Ordinal) -lt 0) {
    throw 'Cannot locate the existing reset-helper reference in the Windows manager; review compatibility before bundling.'
}
$managerContent = $managerContent.Replace('Reset-SparrowTestDeployment.ps1', 'Reset-SparrowDeployment.ps1')
[System.IO.File]::WriteAllText($managerFile, $managerContent, [System.Text.UTF8Encoding]::new($false))
# Bundle a concise runtime README instead of source-only verification instructions.
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README_RUNTIME.md') -Destination (Join-Path $bundleRoot 'README.md') -Force
foreach ($name in @('Dockerfile')) {
    $target = Join-Path $bundleRoot 'directory-sync'
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "directory-sync/$name") -Destination $target -Force
}
# Distribute only the one-shot PUBLIC registration client image definition.
# Never copy server/control-plane-directory or any server identities/secrets.
$registrationTarget = Join-Path $bundleRoot 'directory-registration'
New-Item -ItemType Directory -Path $registrationTarget -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'directory-registration/Dockerfile') `
    -Destination $registrationTarget -Force
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'node-instances/routes') -Force | Out-Null
foreach ($name in @('docker-compose.yml')) {
    $target = Join-Path $bundleRoot 'public-proxy'
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "public-proxy/$name") -Destination $target -Force
}
# The existing build/release variable is a Directory Server base address, not
# a legacy JSON list path. Operators can override it in the installer.
$defaultDirectoryUrl = if ($env:CONTROL_PLANE_RELEASE_DIRECTORY_URL) {
    $env:CONTROL_PLANE_RELEASE_DIRECTORY_URL.Trim()
} elseif ($env:CONTROL_PLANE_DIRECTORY_URL) {
    $env:CONTROL_PLANE_DIRECTORY_URL.Trim()
} else { '' }
if ($defaultDirectoryUrl) {
    $directoryUri = $null
    if (-not [Uri]::TryCreate($defaultDirectoryUrl, [UriKind]::Absolute, [ref]$directoryUri) -or
        $directoryUri.Scheme -ne 'https' -or -not $directoryUri.Host -or
        $directoryUri.UserInfo -or $directoryUri.Fragment -or $directoryUri.Query -or
        $directoryUri.AbsolutePath -ne '/') {
        throw 'CONTROL_PLANE_DIRECTORY_URL must be an HTTPS Directory Server base URL without path, query or credentials.'
    }
    $defaultDirectoryUrl = $directoryUri.GetLeftPart([System.UriPartial]::Authority).TrimEnd('/')
}
# The public verification key is a trust pin, NOT an administrator credential.
# Must be provisioned out of band and must never be downloaded from the
# directory URL we are about to trust. An empty value disables remote discovery.
$defaultDirectoryPublicKey = if ($env:CONTROL_PLANE_DIRECTORY_PUBLIC_KEY) {
    $env:CONTROL_PLANE_DIRECTORY_PUBLIC_KEY.Trim()
} else { '' }
if ($defaultDirectoryPublicKey -and $defaultDirectoryPublicKey -notmatch '^[A-Za-z0-9_-]{58,100}$') {
    throw 'CONTROL_PLANE_DIRECTORY_PUBLIC_KEY must be a base64url-encoded DER Ed25519 public key.'
}
$componentFiles = @{
    'community-node' = @(
        'docker-compose.yml', 'docker-compose.release.yml', 'docker-compose.production.yml',
        'docker-compose.shared-proxy.yml', 'docker-compose.directory-sync.yml', 'Caddyfile', 'index.html',
        'Bootstrap-CommunityNode.ps1', 'bootstrap-community-node.sh', 'start-sparrow-node.sh', 'Start-SparrowNode.command'
    )
    'control-plane' = @(
        'docker-compose.yml', 'docker-compose.release.yml', 'docker-compose.production.yml',
        'docker-compose.shared-proxy.yml', 'docker-compose.firebase.yml',
        'Caddyfile', 'index.html', 'bootstrap-control-plane.sh', 'start-sparrow-control-plane.sh', 'Start-SparrowControlPlane.command'
    )
}
foreach ($component in $componentFiles.Keys) {
    $target = Join-Path $bundleRoot $component
    $secretRoot = Join-Path $target 'secrets'
    New-Item -ItemType Directory -Path $secretRoot -Force | Out-Null
    foreach ($name in $componentFiles[$component]) {
        Copy-Item -LiteralPath (Join-Path $repositoryRoot "server/$component/$name") -Destination $target -Force
    }
    [System.IO.File]::WriteAllText((Join-Path $secretRoot '.gitignore'), "*`n!.gitignore`n", [System.Text.UTF8Encoding]::new($false))
    $configPath = Join-Path $target 'sparrow.conf'
    [System.IO.File]::WriteAllLines($configPath, @(
        'CONFIGURED=false', 'MODE=lan', 'PUBLIC_DOMAIN=', 'SHARED_PROXY=false',
        "SPARROW_IMAGE_PREFIX=$ImagePrefix", "SPARROW_IMAGE_TAG=$ImageTag",
        $(if ($component -eq 'control-plane') { 'CONTROL_PLANE_ID=' } else { 'CONTROL_PLANE_URLS=' }),
        "CONTROL_PLANE_DIRECTORY_URL=$defaultDirectoryUrl",
        "CONTROL_PLANE_DIRECTORY_PUBLIC_KEY=$defaultDirectoryPublicKey"
    ), [System.Text.UTF8Encoding]::new($false))
}
# The repository is embedded at build time, not selected by the server operator.
# GitHub Actions sets GITHUB_REPOSITORY to the actual source repository.
$releaseRepository = if ($env:GITHUB_REPOSITORY) { $env:GITHUB_REPOSITORY } else { 'cbgm/sparrow' }
if ($releaseRepository -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$') {
    throw 'Invalid GitHub release repository.'
}
[System.IO.File]::WriteAllText((Join-Path $bundleRoot 'bundle-source.json'),
    (@{ repository = $releaseRepository } | ConvertTo-Json -Compress),
    [System.Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $repositoryRoot 'server/scripts/Bootstrap-ControlPlane.Bundle.ps1') -Destination (Join-Path $bundleRoot 'control-plane/Bootstrap-ControlPlane.ps1') -Force
[System.IO.File]::WriteAllText((Join-Path $bundleRoot 'Start-SparrowServer.cmd'),
    "@echo off`r`ncd /d `"%~dp0`"`r`nstart `"`" powershell.exe -STA -NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"%~dp0Start-SparrowServer.ps1`"`r`n",
    [System.Text.UTF8Encoding]::new($false))
New-Item -ItemType Directory -Path (Split-Path -Parent $dest) -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $dest) { Remove-Item -LiteralPath $dest -Force }
[System.IO.Compression.ZipFile]::CreateFromDirectory($bundleRoot, $dest, [System.IO.Compression.CompressionLevel]::Optimal, $false)
Write-Host "Created $dest (fresh installations only; existing deployment migration must preserve original secrets/config and volumes)."
