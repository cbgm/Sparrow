[CmdletBinding()]
param(
    [switch]$DeleteDisposableData,
    [switch]$ReplaceInstalledTestData,
    [string]$Confirmation = '',
    [string]$InstallationDirectory = $PSScriptRoot,
    [ValidateSet('Node','ControlPlane','Combined')][string]$Component = 'Combined'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($InstallationDirectory)
$projects = switch ($Component) {
    'Node' { @('sparrow-community-node') }
    'ControlPlane' { @('sparrow-control-plane') }
    'Combined' { @('sparrow-community-node', 'sparrow-control-plane', 'sparrow-public-proxy') }
}
$parts = switch ($Component) {
    'Node' { @('community-node') }
    'ControlPlane' { @('control-plane') }
    'Combined' { @('community-node', 'control-plane') }
}
if ($ReplaceInstalledTestData -and ($Component -ne 'Combined' -or -not $DeleteDisposableData -or
    $Confirmation -cne 'DELETE SPARROW SERVER DATA')) {
    throw 'Installed server replacement requires Combined and explicit DELETE SPARROW SERVER DATA confirmation.'
}
if (Test-Path -LiteralPath (Join-Path $root '.sparrow-attached.json') -PathType Leaf) {
    throw 'Attached/migrated deployments cannot be reset using the disposable-test cleanup.'
}
foreach ($part in $parts) {
    $folder = Join-Path $root $part
    if (-not $ReplaceInstalledTestData -and (Test-Path -LiteralPath (Join-Path $folder '.env.runtime') -PathType Leaf)) {
        throw "Refusing test-data reset: $part has an installed .env.runtime. Use Install / Start from the original installation folder for updates; do not delete its data."
    }
    $secretDir = Join-Path $folder 'secrets'
    if (Test-Path -LiteralPath $secretDir -PathType Container) {
        $keys = @(Get-ChildItem -LiteralPath $secretDir -Force -Recurse -File | Where-Object { $_.Name -ne '.gitignore' })
        $generated = if ($part -eq 'community-node') {
            @('mailbox-database-password.txt', 'federation-database-password.txt', 'federation-internal-api-token.txt', 'gateway-internal-api-token.txt')
        } else {
            @('node-registry-database-password.txt', 'presence-redis-password.txt', 'push-database-password.txt', 'push-internal-api-token.txt')
        }
        $unknown = @($keys | Where-Object { $_.Name -notin $generated -and $_.Name -ne 'firebase-admin.json' })
        # A routine orphan cleanup must never erase operator-supplied files.
        # In contrast, a CONFIRMED fresh Combined TEST reinstall must clear
        # the entire ACTIVE secrets directory: leaving unknown files behind
        # would cause the next bootstrap to reuse stale credentials/state.
        if (-not $ReplaceInstalledTestData -and $unknown.Count -gt 0) {
            throw "Unknown secrets in $secretDir; refusing automatic replacement."
        }
        if ($ReplaceInstalledTestData) {
            # Never follow a redirected secrets directory or child entry while
            # deleting an explicitly selected install. In that case stop BEFORE
            # touching Docker resources; a reparse point may lead outside root.
            $redirected = @(Get-Item -LiteralPath $secretDir -Force |
                Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint })
            $redirected += @(Get-ChildItem -LiteralPath $secretDir -Force -Recurse |
                Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint })
            if ($redirected.Count -gt 0) {
                throw "Secrets directory contains a redirected path: $secretDir. Refusing deletion outside the selected installation."
            }
        }
    }
}
$containers = [System.Collections.Generic.List[object]]::new()
$volumes = [System.Collections.Generic.List[string]]::new()
foreach ($project in $projects) {
    $ids = @(& docker ps --all --filter "label=com.docker.compose.project=$project" --format '{{.ID}}' 2>$null)
    if ($LASTEXITCODE -ne 0) { throw "Cannot enumerate containers for $project" }
    foreach ($id in $ids) {
        $labelsJson = @(& docker container inspect $id --format '{{json .Config.Labels}}' 2>$null)
        if ($LASTEXITCODE -ne 0 -or $labelsJson.Count -ne 1) { throw "Could not verify container $id ownership." }
        $labels = ([string]$labelsJson[0]) | ConvertFrom-Json -ErrorAction Stop
        if ($null -eq $labels -or
            $labels.'com.docker.compose.project' -ne $project) {
            throw "Container $id has ambiguous Compose ownership; refusing automatic replacement."
        }
        $original = [string]$labels.'com.docker.compose.project.working_dir'
        if ([string]::IsNullOrWhiteSpace($original)) { throw "Container $id has no original Compose workdir; refusing automatic replacement." }
        # Docker Desktop sometimes reports a Windows folder through Linux's
        # host mount prefix; only discard after checking for a real runtime.
        if ($original -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
            $original = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
        }
        if ($original -match '^[A-Za-z]:[\\/]') {
            $originalRuntime = Join-Path $original '.env.runtime'
            if ($ReplaceInstalledTestData) {
                # Never delete a different installation simply because it uses
                # the same hard-coded Compose project name.
                $expected = if ($project -eq 'sparrow-control-plane') {
                    Join-Path $root 'control-plane'
                } elseif ($project -eq 'sparrow-community-node') {
                    Join-Path $root 'community-node'
                } else { Join-Path $root 'public-proxy' }
                $actualPath = [IO.Path]::GetFullPath($original).TrimEnd('\')
                $expectedPath = [IO.Path]::GetFullPath($expected).TrimEnd('\')
                if (-not $actualPath.Equals($expectedPath, [StringComparison]::OrdinalIgnoreCase)) {
                    # A prior interrupted test bundle can leave containers at
                    # an old folder, but an intact runtime there is NEVER ours.
                    if (Test-Path -LiteralPath $originalRuntime -PathType Leaf) {
                        throw "Container $id belongs to an ACTIVE DIFFERENT installer folder ($actualPath). Refusing to delete its data."
                    }
                    Write-Host "Clearing orphaned $project test container from former folder: $actualPath"
                }
            } elseif (Test-Path -LiteralPath $originalRuntime -PathType Leaf) {
                throw "Existing $project runtime still exists in $original. Open that installation for updates; refusing to erase its Docker data."
            }
        } else {
            throw "Cannot verify the original Windows folder for container $id ($original); refusing automatic replacement."
        }
        $containers.Add([pscustomobject]@{ Id=$id; Project=$project })
    }
    $names = @(& docker volume ls --quiet --filter "name=^${project}_" 2>$null)
    if ($LASTEXITCODE -ne 0) { throw "Cannot enumerate volumes for $project" }
    foreach ($volume in $names) {
        if ($volume -notlike "${project}_*") { continue }
        # A matching name with a conflicting ownership label is NOT ours.
        $raw = @(& docker volume inspect $volume --format '{{json .Labels}}' 2>$null)
        if ($LASTEXITCODE -ne 0 -or $raw.Count -ne 1) { throw "Cannot inspect ownership for $volume" }
        $labels = ([string]$raw[0]) | ConvertFrom-Json -ErrorAction Stop
        if ($null -ne $labels) {
            $owner = $labels.PSObject.Properties['com.docker.compose.project']
            if ($owner -and $owner.Value -and $owner.Value -ne $project) {
                throw "Volume $volume belongs to another project; refusing cleanup."
            }
        }
        $volumes.Add($volume)
    }
}
Write-Host "Sparrow TEST replacement inventory for $Component (Docker engine currently selected):"
foreach ($container in $containers) { Write-Host "  Container: $($container.Id) ($($container.Project))" }
foreach ($volume in $volumes) { Write-Host "  DATA VOLUME: $volume" }
if (-not $DeleteDisposableData) {
    Write-Host 'Read-only inventory. To actually delete the listed TEST resources, re-run with -DeleteDisposableData -Confirmation "DELETE SPARROW TEST DATA".'
    exit 0
}
if ($Confirmation -cne $(if ($ReplaceInstalledTestData) { 'DELETE SPARROW SERVER DATA' } else { 'DELETE SPARROW TEST DATA' })) {
    throw 'Deletion refused: incorrect confirmation. This deletes server identities, databases, queues, and TLS certificates.'
}
foreach ($container in $containers) {
    & docker rm -f $container.Id
    if ($LASTEXITCODE -ne 0) { throw "Could not remove $($container.Id); stopping without deleting remaining volumes." }
}
foreach ($volume in $volumes) {
    & docker volume rm $volume
    if ($LASTEXITCODE -ne 0) { throw "Could not remove volume $volume. Inspect Docker usage before retrying." }
}
# A previous failed bootstrap can leave passwords on disk even though no
# runtime file exists. They cannot be reused with fresh DB volumes.
# For an EXPLICIT fresh Combined TEST reinstall, also remove unrecognized
# files from the selected deployment's active secrets folders: otherwise
# a stale credential can be silently reused by the next bootstrap.
foreach ($part in $parts) {
    $folder = Join-Path $root $part
    $secretDir = Join-Path $folder 'secrets'
    if ($ReplaceInstalledTestData -and (Test-Path -LiteralPath $secretDir -PathType Container)) {
        Remove-Item -LiteralPath $secretDir -Recurse -Force -ErrorAction Stop
        Write-Host "Cleared $part TEST secrets for confirmed fresh reinstall."
    }
    $generated = if ($part -eq 'community-node') {
        @('mailbox-database-password.txt', 'federation-database-password.txt', 'federation-internal-api-token.txt', 'gateway-internal-api-token.txt')
    } else {
        @('node-registry-database-password.txt', 'presence-redis-password.txt', 'push-database-password.txt', 'push-internal-api-token.txt')
    }
    foreach ($name in $generated) {
        $path = Join-Path (Join-Path $folder 'secrets') $name
        if (Test-Path -LiteralPath $path -PathType Leaf) { Remove-Item -LiteralPath $path -Force -ErrorAction Stop }
    }
    if ($ReplaceInstalledTestData) {
        # Only after Docker projects and volumes have been removed. Deleting
        # these files before preflight would orphan a still-running deployment.
        foreach ($filename in @('.env.runtime','sparrow.conf')) {
            $path = Join-Path $folder $filename
            if (Test-Path -LiteralPath $path -PathType Leaf) {
                Remove-Item -LiteralPath $path -Force -ErrorAction Stop
            }
        }
    }
    # Keep operator-supplied firebase-admin.json and bundled source templates.
}
if ($ReplaceInstalledTestData) {
    $caddyfile = Join-Path (Join-Path $root 'public-proxy') 'Caddyfile'
    if (Test-Path -LiteralPath $caddyfile -PathType Leaf) {
        Remove-Item -LiteralPath $caddyfile -Force -ErrorAction Stop
    }
    Write-Host 'Existing Combined TEST deployment deleted. Server will be freshly bootstrapped with NEW identities, queues, databases and TLS certificates.'
} else {
    Write-Host 'Selected abandoned TEST Docker resources and old generated passwords removed. Fresh configuration and secrets will be generated by Install / Start.'
}
