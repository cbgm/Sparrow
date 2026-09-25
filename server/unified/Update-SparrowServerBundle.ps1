[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$BundleZip,
    [string]$InstallationDirectory = $PSScriptRoot,
    [switch]$ApplyImages,
    [ValidateSet('Combined','Node','ControlPlane')][string]$Component = 'Combined'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Update the manager AND backend Compose/bootstrap files IN PLACE. Never extract
# an installer archive over a configured deployment: the ZIP has placeholder
# sparrow.conf files and does not contain its generated .env.runtime or secrets.
$install = [IO.Path]::GetFullPath($InstallationDirectory).TrimEnd('\','/')
$archive = [IO.Path]::GetFullPath($BundleZip)
if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw "Bundle ZIP not found: $archive" }
if (-not (Test-Path -LiteralPath (Join-Path $install 'Invoke-SparrowServer.ps1') -PathType Leaf)) {
    throw "No Sparrow unified installer found at $install. Choose the ORIGINAL installed bundle directory."
}
if (Test-Path -LiteralPath (Join-Path $install '.sparrow-attached.json') -PathType Leaf) {
    throw 'This manager is attached to a migrated deployment. A separate reviewed update procedure is required for its original folders.'
}

$stage = Join-Path ([IO.Path]::GetTempPath()) ("sparrow-update-" + [guid]::NewGuid().ToString('N'))
$backup = Join-Path $install (".sparrow-code-backups\" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
$manifest = @{
    '' = @('Start-SparrowServer.cmd', 'Start-SparrowServer.ps1', 'Invoke-SparrowServer.ps1',
        'Get-SparrowDockerLabel.ps1', 'Attached-SparrowDeployment.ps1', 'README.md',
        'Backup-SparrowDeployment.ps1', 'Stage-SparrowPublicTls.ps1',
        'Invoke-SparrowPublicCutover.ps1', 'Invoke-SparrowServer.py',
        'Start-SparrowServer.sh', 'Start-SparrowServer.command',
        'Update-SparrowServerBundle.ps1', 'Update-SparrowFromGitHub.ps1',
        'Reset-SparrowDeployment.ps1', 'bundle-source.json')
    'community-node' = @('docker-compose.yml', 'docker-compose.release.yml',
        'docker-compose.production.yml', 'docker-compose.shared-proxy.yml',
        'Caddyfile', 'index.html', 'Bootstrap-CommunityNode.ps1',
        'bootstrap-community-node.sh', 'start-sparrow-node.sh', 'Start-SparrowNode.command')
    'control-plane' = @('docker-compose.yml', 'docker-compose.release.yml',
        'docker-compose.production.yml', 'docker-compose.shared-proxy.yml',
        'docker-compose.firebase.yml', 'Caddyfile', 'index.html',
        'Bootstrap-ControlPlane.ps1', 'bootstrap-control-plane.sh',
        'start-sparrow-control-plane.sh', 'Start-SparrowControlPlane.command')
    'public-proxy' = @('docker-compose.yml')
}
$changes = [System.Collections.Generic.List[object]]::new()
try {
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    Expand-Archive -LiteralPath $archive -DestinationPath $stage -Force
    foreach ($required in @('Invoke-SparrowServer.ps1','Start-SparrowServer.ps1',
            'community-node/Bootstrap-CommunityNode.ps1','control-plane/Bootstrap-ControlPlane.ps1')) {
        if (-not (Test-Path -LiteralPath (Join-Path $stage $required) -PathType Leaf)) {
            throw "Not a complete compatible Sparrow server bundle: missing $required. No files changed."
        }
    }
    # Source ZIPs may contain placeholder config and secrets/.gitignore; ONLY
    # explicit code/Compose files in the manifest are copied.
    foreach ($folder in $manifest.Keys) {
        foreach ($name in $manifest[$folder]) {
            $relative = if ($folder) { "$folder/$name" } else { $name }
            $source = Join-Path $stage $relative
            if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { continue }
            $destination = Join-Path $install $relative
            $changes.Add([pscustomobject]@{ Relative = $relative; Source = $source;
                Destination = $destination; Existed = (Test-Path -LiteralPath $destination -PathType Leaf) })
        }
    }
    if ($changes.Count -lt 20) { throw 'Bundle appears incomplete. No files changed.' }
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    foreach ($change in $changes) {
        if ($change.Existed) {
            $saved = Join-Path $backup $change.Relative
            New-Item -ItemType Directory -Path (Split-Path -Parent $saved) -Force | Out-Null
            Copy-Item -LiteralPath $change.Destination -Destination $saved -ErrorAction Stop
        }
    }
    $applied = [System.Collections.Generic.List[object]]::new()
    try {
        foreach ($change in $changes) {
            New-Item -ItemType Directory -Path (Split-Path -Parent $change.Destination) -Force | Out-Null
            $applied.Add($change)
            Copy-Item -LiteralPath $change.Source -Destination $change.Destination -Force -ErrorAction Stop
            if ((Get-FileHash -LiteralPath $change.Source -Algorithm SHA256).Hash -ne
                (Get-FileHash -LiteralPath $change.Destination -Algorithm SHA256).Hash) {
                throw "Failed to verify updated code: $($change.Relative)"
            }
        }
    } catch {
        $reason = $_.Exception.Message
        for ($i = $applied.Count - 1; $i -ge 0; $i--) {
            $change = $applied[$i]
            if ($change.Existed) {
                Copy-Item -LiteralPath (Join-Path $backup $change.Relative) -Destination $change.Destination -Force -ErrorAction Stop
            } else {
                Remove-Item -LiteralPath $change.Destination -Force -ErrorAction Stop
            }
        }
        throw "Code update failed; previous code restored: $reason"
    }
    Write-Host "Updated $($changes.Count) code/Compose files IN PLACE: $install"
    Write-Host "Previous code backup: $backup"
    Write-Host 'Preserved: sparrow.conf, .env.runtime, secrets, server identities, Docker projects, named volumes, database contents and TLS data.'
    if (-not $ApplyImages) { Write-Host 'Code-only update completed. Reopen the manager in this ORIGINAL folder; backend images have not been updated.' }

    if ($ApplyImages) {
        # This is an update, NOT a fresh bootstrap: never regenerate passwords,
        # identities, configs, or Docker volumes. Pull image tags from GHCR and
        # recreate only existing services in their original Compose projects.
        $parts = if ($Component -eq 'Combined') { @('control-plane','community-node') } elseif ($Component -eq 'Node') { @('community-node') } else { @('control-plane') }
        foreach ($part in $parts) {
            $folder = Join-Path $install $part
            $runtime = Join-Path $folder '.env.runtime'
            $config = Join-Path $folder 'sparrow.conf'
            if (-not (Test-Path -LiteralPath $runtime -PathType Leaf) -or -not (Test-Path -LiteralPath $config -PathType Leaf)) {
                throw "Updated manager code, but $part has no original installed config/runtime. Docker images were NOT updated."
            }
            $settings = @{}
            foreach ($line in (Get-Content -LiteralPath $config)) {
                if ($line -match '^([A-Z_][A-Z_0-9]*)=(.*)$') { $settings[$Matches[1]] = $Matches[2] }
            }
            $expectedProject = if ($part -eq 'community-node') { 'sparrow-community-node' } else { 'sparrow-control-plane' }
            $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$expectedProject")
            if ($LASTEXITCODE -ne 0 -or $ids.Count -eq 0) {
                throw "Updated manager code, but $expectedProject has no existing containers. Refusing to initialize a new deployment during Update."
            }
            # Explicitly validate original Compose ownership before any Docker pull/up.
            $rootCanonical = [IO.Path]::GetFullPath($folder).TrimEnd('\','/')
            foreach ($id in $ids) {
                $json = @(& docker container inspect $id --format '{{json .Config.Labels}}')
                if ($LASTEXITCODE -ne 0 -or $json.Count -ne 1) { throw "Cannot inspect $expectedProject container ownership." }
                $labels = [string]$json[0] | ConvertFrom-Json -ErrorAction Stop
                $owner = [string]$labels.'com.docker.compose.project.working_dir'
                if ($owner -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
                    $owner = "$($Matches[1]):\$($Matches[2].Replace('/','\'))"
                }
                if ([string]::IsNullOrWhiteSpace($owner) -or
                    -not [IO.Path]::GetFullPath($owner).TrimEnd('\','/').Equals($rootCanonical,[StringComparison]::OrdinalIgnoreCase)) {
                    throw "Existing $expectedProject container $id belongs to $owner, not $folder. Refusing to update a different deployment."
                }
            }
            $composeArguments = @('compose','--env-file',$runtime,'-f',(Join-Path $folder 'docker-compose.yml'),'-f',(Join-Path $folder 'docker-compose.release.yml'))
            if ($settings['MODE'] -eq 'public') { $composeArguments += @('-f',(Join-Path $folder 'docker-compose.production.yml')) }
            if ($settings['MODE'] -eq 'public' -and $settings['SHARED_PROXY'] -eq 'true') { $composeArguments += @('-f',(Join-Path $folder 'docker-compose.shared-proxy.yml')) }
            if ($part -eq 'control-plane') {
                $runtimeProperties = @{}
                foreach ($line in (Get-Content -LiteralPath $runtime)) {
                    if ($line -match '^([A-Z_][A-Z_0-9]*)=(.*)$') { $runtimeProperties[$Matches[1]] = $Matches[2] }
                }
                if ($runtimeProperties['FIREBASE_ADMIN_CREDENTIALS'] -and
                    (Test-Path -LiteralPath $runtimeProperties['FIREBASE_ADMIN_CREDENTIALS'] -PathType Leaf)) {
                    $composeArguments += @('-f',(Join-Path $folder 'docker-compose.firebase.yml'))
                }
            }
            Write-Host "Validating installed $part Compose config (without changing runtime data) ..."
            & docker @composeArguments config --quiet
            if ($LASTEXITCODE -ne 0) { throw "Updated manager code, but $part Compose config failed validation. No image pull for $part." }
            # Pull/update ONLY Sparrow backend services. Never implicitly upgrade
            # PostgreSQL, Redis, Caddy, or their storage formats during an app update.
            $backendServices = if ($part -eq 'community-node') { @('mailbox','federation','gateway') } else { @('node-registry','presence-directory','push') }
            foreach ($service in $backendServices) {
                $serviceIds = @(& docker ps -aq --filter "label=com.docker.compose.project=$expectedProject" --filter "label=com.docker.compose.service=$service")
                if ($LASTEXITCODE -ne 0 -or $serviceIds.Count -eq 0) {
                    throw "Existing $expectedProject service $service not found. Refusing to create missing services during Update."
                }
            }
            Write-Host "Pulling $part backend images from the configured GitHub Container Registry ..."
            & docker @composeArguments pull @backendServices
            if ($LASTEXITCODE -ne 0) { throw "Updated manager code, but $part image pull failed. Existing containers/data retained." }
            if ($part -eq 'community-node') {
                # Update uses `up --no-deps`, which bypasses gateway.depends_on.
                # Explicitly initialize the EXISTING named blob volume before updating gateway.
                # The one-shot helper is separate from the capability-restricted gateway.
                Write-Host 'Ensuring existing gateway blob storage is writable (no data reset) ...'
                & docker @composeArguments run --rm --no-deps blob-storage-init
                if ($LASTEXITCODE -ne 0) { throw 'Blob storage initialization failed. Gateway image update not started.' }
            }
            Write-Host "Applying the pulled images to the existing $part Compose project ..."
            & docker @composeArguments up -d --no-deps @backendServices
            if ($LASTEXITCODE -ne 0) { throw "Updated manager code, but $part service update failed. Inspect Docker logs and original backup." }
        }
        Write-Host 'Selected existing backend services updated from GHCR. Database/Redis/Caddy service images were not pulled or recreated; secrets, TLS volumes, queues and identities were not explicitly removed.'
    }
    foreach ($part in @('control-plane','community-node')) {
        if (-not (Test-Path -LiteralPath (Join-Path $install "$part/.env.runtime") -PathType Leaf)) {
            Write-Warning "$part has no local .env.runtime. Updating code cannot recover lost deployment secrets; Status now reports any orphan Docker resources."
        }
    }
} finally {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
