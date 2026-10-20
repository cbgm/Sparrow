[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BundleDirectory,
    [string]$InstallationDirectory = (Split-Path -Parent $PSScriptRoot)
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Replaces the manager code and the component Compose/bootstrap templates of an
# installation IN PLACE. Runtime state (sparrow.conf, .env.runtime, secrets,
# identities, logs, Docker projects and volumes) is never copied or removed.
# This script always runs from the NEW bundle, so it also knows how to migrate
# installations that still use the previous flat folder layout.
$bundle = [IO.Path]::GetFullPath($BundleDirectory).TrimEnd('\', '/')
$install = [IO.Path]::GetFullPath($InstallationDirectory).TrimEnd('\', '/')

function Get-RelativePath([string]$Base, [string]$Path) {
    return $Path.Substring($Base.Length + 1).Replace('\', '/')
}

foreach ($required in @('app/Invoke-SparrowServer.ps1', 'app/manager_window.py', 'app/bundle.json', 'app/layout.json',
        'community-node/Bootstrap-CommunityNode.ps1', 'control-plane/Bootstrap-ControlPlane.ps1')) {
    if (-not (Test-Path -LiteralPath (Join-Path $bundle $required) -PathType Leaf)) {
        throw "Not a complete Sparrow server bundle: missing $required. No files changed."
    }
}
foreach ($required in @('control-plane/sparrow.conf', 'community-node/sparrow.conf')) {
    if (-not (Test-Path -LiteralPath (Join-Path $install $required) -PathType Leaf)) {
        throw "No Sparrow server installation found at $install. No files changed."
    }
}
if ($bundle.Equals($install, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The bundle is already this installation. No files changed.'
}
if (Test-Path -LiteralPath (Join-Path $install '.sparrow-attached.json') -PathType Leaf) {
    throw 'This manager is attached to a migrated deployment. A separate reviewed update procedure is required for its original folders.'
}
$stagedApp = Join-Path $bundle 'app'
# File lists shared with the Linux/macOS updater and the bundle builder.
$layout = Get-Content -LiteralPath (Join-Path $stagedApp 'layout.json') -Raw | ConvertFrom-Json
$componentFiles = [ordered]@{}
foreach ($property in $layout.components.PSObject.Properties) { $componentFiles[$property.Name] = @($property.Value) }
$topLevelFiles = @($layout.topLevel)
$legacyFiles = @($layout.legacy)
$instanceHelpers = @($layout.instanceHelpers)
if (Test-Path -LiteralPath (Join-Path $stagedApp 'local-images.json') -PathType Leaf) {
    . (Join-Path $stagedApp 'SparrowLocalImages.ps1')
    $local = Read-SparrowLocalImages $stagedApp
    if ((Get-FileHash -LiteralPath (Join-Path $stagedApp $local.archive) -Algorithm SHA256).Hash -ine $local.sha256) {
        throw 'Local image archive checksum mismatch. No files changed.'
    }
} elseif (Test-Path -LiteralPath (Join-Path $stagedApp 'local-images.tar') -PathType Leaf) {
    throw 'Local image archive has no manifest. No files changed.'
}

$changes = [System.Collections.Generic.List[object]]::new()
function Add-Copy([string]$Relative) {
    $destination = Join-Path $install $Relative
    $changes.Add([pscustomobject]@{
        Relative = $Relative; Source = (Join-Path $bundle $Relative); Destination = $destination
        Existed = (Test-Path -LiteralPath $destination -PathType Leaf)
    })
}
function Add-Removal([string]$Relative) {
    $destination = Join-Path $install $Relative
    if (Test-Path -LiteralPath $destination -PathType Leaf) {
        $changes.Add([pscustomobject]@{ Relative = $Relative; Source = $null; Destination = $destination; Existed = $true })
    }
}

$newAppFiles = @(Get-ChildItem -LiteralPath $stagedApp -Recurse -File -Force |
    Where-Object { $_.FullName -notmatch '[\\/]__pycache__[\\/]' } |
    ForEach-Object { 'app/' + (Get-RelativePath $stagedApp $_.FullName) })
foreach ($relative in $newAppFiles) { Add-Copy $relative }
$installedApp = Join-Path $install 'app'
if (Test-Path -LiteralPath $installedApp -PathType Container) {
    foreach ($file in @(Get-ChildItem -LiteralPath $installedApp -Recurse -File -Force |
            Where-Object { $_.FullName -notmatch '[\\/]__pycache__[\\/]' })) {
        $relative = 'app/' + (Get-RelativePath $installedApp $file.FullName)
        if ($relative -notin $newAppFiles) { Add-Removal $relative }
    }
}
foreach ($name in $topLevelFiles) { Add-Copy $name }
foreach ($folder in $componentFiles.Keys) {
    foreach ($name in $componentFiles[$folder]) { Add-Copy "$folder/$name" }
}
# An old flat installation may run a local build; keep its images selected.
$keepLegacyImages = (Test-Path -LiteralPath (Join-Path $install 'local-images.json') -PathType Leaf) -and
    -not (Test-Path -LiteralPath (Join-Path $stagedApp 'local-images.json') -PathType Leaf)
foreach ($relative in $legacyFiles) {
    if ($keepLegacyImages -and $relative -in @('local-images.json', 'local-images.tar')) { continue }
    Add-Removal $relative
}
# A component that was never installed still has the bundle's placeholder
# settings; take the new ones so Install uses this bundle's image tag.
foreach ($component in @('control-plane', 'community-node')) {
    $configured = Select-String -LiteralPath (Join-Path $install "$component/sparrow.conf") -Pattern '^CONFIGURED=true$' -Quiet
    if (-not $configured -and -not (Test-Path -LiteralPath (Join-Path $install "$component/.env.runtime") -PathType Leaf)) {
        Add-Copy "$component/sparrow.conf"
    }
}
foreach ($helper in $instanceHelpers) {
    if (Test-Path -LiteralPath (Join-Path $install "node-instances/$helper") -PathType Leaf) {
        $destination = Join-Path $install "node-instances/$helper"
        $changes.Add([pscustomobject]@{
            Relative = "node-instances/$helper"; Source = (Join-Path $stagedApp $helper)
            Destination = $destination; Existed = $true
        })
    }
}
foreach ($change in $changes) {
    if ($null -ne $change.Source -and -not (Test-Path -LiteralPath $change.Source -PathType Leaf)) {
        throw "Bundle is missing $($change.Relative). No files changed."
    }
}

$backup = Join-Path $install ('.sparrow-code-backups/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' +
    [guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Path $backup -Force | Out-Null
foreach ($change in $changes) {
    if ($change.Existed -and $change.Relative -notmatch '(^|/)local-images\.tar$') {
        $saved = Join-Path $backup $change.Relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $saved) -Force | Out-Null
        Copy-Item -LiteralPath $change.Destination -Destination $saved -ErrorAction Stop
    }
}
$applied = [System.Collections.Generic.List[object]]::new()
try {
    foreach ($change in $changes) {
        if ($null -eq $change.Source) {
            Remove-Item -LiteralPath $change.Destination -Force -ErrorAction Stop
            $applied.Add($change)
            continue
        }
        New-Item -ItemType Directory -Path (Split-Path -Parent $change.Destination) -Force | Out-Null
        $applied.Add($change)
        Copy-Item -LiteralPath $change.Source -Destination $change.Destination -Force -ErrorAction Stop
        if ((Get-FileHash -LiteralPath $change.Source -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $change.Destination -Algorithm SHA256).Hash) {
            throw "Failed to verify updated file: $($change.Relative)"
        }
    }
} catch {
    $reason = $_.Exception.Message
    for ($i = $applied.Count - 1; $i -ge 0; $i--) {
        $change = $applied[$i]
        $saved = Join-Path $backup $change.Relative
        if ($change.Existed -and (Test-Path -LiteralPath $saved -PathType Leaf)) {
            Copy-Item -LiteralPath $saved -Destination $change.Destination -Force -ErrorAction SilentlyContinue
        } elseif (-not $change.Existed) {
            Remove-Item -LiteralPath $change.Destination -Force -ErrorAction SilentlyContinue
        }
    }
    throw "Code update failed; previous code restored: $reason"
}
if ($keepLegacyImages) {
    foreach ($name in @('local-images.json', 'local-images.tar')) {
        $legacy = Join-Path $install $name
        if (Test-Path -LiteralPath $legacy -PathType Leaf) {
            Move-Item -LiteralPath $legacy -Destination (Join-Path $installedApp $name) -Force
        }
    }
    Write-Host 'Kept the installed local build images.'
}
foreach ($folder in @('directory-sync', 'directory-registration')) {
    $path = Join-Path $install $folder
    if ((Test-Path -LiteralPath $path -PathType Container) -and -not @(Get-ChildItem -LiteralPath $path -Force).Count) {
        Remove-Item -LiteralPath $path -Force
    }
}
$removed = @($changes | Where-Object { $null -eq $_.Source }).Count
Write-Host "Updated $($changes.Count - $removed) manager files in place ($removed outdated files removed): $install"
Write-Host "Previous code backup: $backup"
Write-Host 'Preserved: sparrow.conf, .env.runtime, secrets, server identities, Docker projects, volumes and TLS data.'
