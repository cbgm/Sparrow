# Explicit, in-place legacy Public -> shared-Caddy cutover. This script does not
# migrate data volumes or change the original Compose project/working directory.
# -Apply requires an offline snapshot, staged TLS and an unchanged readiness plan.
# -Rollback stops the new proxy and restores the original files; with TWO legacy
# Public projects only one can ever bind 80/443, so both are left stopped.
[CmdletBinding(DefaultParameterSetName='Apply')]
param(
    [Parameter(Mandatory=$true)][string]$PlanDirectory,
    [Parameter(Mandatory=$true,ParameterSetName='Apply')][switch]$Apply,
    [Parameter(Mandatory=$true,ParameterSetName='Rollback')][switch]$Rollback
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-SparrowDockerLabel.ps1')

function Absolute([string]$Path) { return [System.IO.Path]::GetFullPath($Path).TrimEnd([char[]]@([char]92,[char]47)) }
function File-Hash([string]$Path) { return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash }
function Docker([string[]]$Arguments) {
    $output = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed: $($Arguments[0]) ($($output -join ' '))" }
    foreach ($line in $output) { Write-Host $line }
}
function Docker-Result([string[]]$Arguments) {
    $output = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "Docker inspection failed: $($output -join ' ')" }
    foreach ($line in $output) { Write-Output $line }
}
function Write-Text([string]$File, [string]$Text) {
    [System.IO.File]::WriteAllText($File, $Text, [System.Text.UTF8Encoding]::new($false))
}
function Save-Transaction([object]$Transaction) {
    $tmp = "${transactionFile}.pending"
    Write-Text $tmp ($Transaction | ConvertTo-Json -Depth 12)
    Move-Item -LiteralPath $tmp -Destination $transactionFile -Force
}
function Compose-Args([object]$Component, [bool]$UseShared) {
    $root = [string]$Component.OriginalDirectory
    $args = @('compose','--project-directory',$root,'--env-file',(Join-Path $root '.env.runtime'),
        '-f',(Join-Path $root 'docker-compose.yml'),'-f',(Join-Path $root 'docker-compose.release.yml'),
        '-f',(Join-Path $root 'docker-compose.production.yml'))
    if ($UseShared) { $args += @('-f',(Join-Path $root 'docker-compose.shared-proxy.yml')) }
    # Preserve an existing Firebase override; it is not a subject of this cutover.
    if ($Component.Component -eq 'control-plane') {
        $runtime = Read-Config (Join-Path $root '.env.runtime')
        $credential = [string]$runtime['FIREBASE_ADMIN_CREDENTIALS']
        if ($credential -and (Test-Path -LiteralPath $credential -PathType Leaf)) {
            if (-not (Test-Path -LiteralPath (Join-Path $root 'docker-compose.firebase.yml') -PathType Leaf)) {
                throw 'Original Firebase credentials exist but the original Firebase Compose override is missing.'
            }
            $args += @('-f',(Join-Path $root 'docker-compose.firebase.yml'))
        }
    }
    return ,$args
}
function Read-Config([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing file: $Path" }
    $result = @{}
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') { $result[$Matches[1]] = $Matches[2].Trim().Trim([char]34).Trim([char]39) }
    }
    return $result
}
function Set-SharedSetting([string]$Path) {
    $original = Get-Content -LiteralPath $Path -Raw
    $hits = [regex]::Matches($original, '(?m)^SHARED_PROXY=.*(?:\r?\n|$)')
    if ($hits.Count -gt 1) { throw "Multiple SHARED_PROXY settings in $Path" }
    $updated = if ($hits.Count -eq 1) {
        [regex]::Replace($original, '(?m)^SHARED_PROXY=.*(?=\r?$)', 'SHARED_PROXY=true')
    } else { $original.TrimEnd([char[]]@([char]13,[char]10)) + "`r`nSHARED_PROXY=true`r`n" }
    return $updated
}
function Verify-ProjectOwner([object]$Component) {
    $project = [string]$Component.Project
    $containers = @(Docker-Result @('ps','--all','--filter',"label=com.docker.compose.project=$project",'--format','{{.ID}}'))
    if ($containers.Count -eq 0) { throw "Original Compose containers are missing for $project" }
    foreach ($id in $containers) {
        $owner = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir')
        $normal = ([string]$owner[0]).Trim().TrimEnd([char[]]@([char]92,[char]47))
        if ($normal -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
            $normal = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
        }
        if (-not $normal.Equals((Absolute ([string]$Component.OriginalDirectory)),[StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to change $project: unexpected working directory for container $id ($normal)."
        }
    }
}
function Assert-SourceFile([string]$Source,[string]$Snapshot,[string]$ExpectedHash) {
    if (-not (Test-Path -LiteralPath $Source -PathType Leaf) -or -not (Test-Path -LiteralPath $Snapshot -PathType Leaf) -or
        (File-Hash $Source) -ne $ExpectedHash -or (File-Hash $Snapshot) -ne $ExpectedHash) {
        throw "Original source file changed since the offline snapshot: $Source"
    }
}
function Write-Phase([string]$State) { $transaction.State = $State; Save-Transaction $transaction; Write-Host "Cutover phase: $State" }
function Stop-SharedProxy([object]$Record) {
    $proxy = [string]$Record.ProxyDirectory
    $ids = @(Docker-Result @('ps','--all','--filter','label=com.docker.compose.project=sparrow-public-proxy','--format','{{.ID}}'))
    foreach ($id in $ids) {
        $owner = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir')
        $workdir = ([string]$owner[0]).Trim().TrimEnd([char[]]@([char]92,[char]47))
        if ($workdir -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
            $workdir = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
        }
        if (-not $workdir.Equals((Absolute $proxy), [StringComparison]::OrdinalIgnoreCase)) {
            throw "Cannot stop another shared proxy owned by $workdir. Manual recovery required."
        }
    }
    if ($ids.Count -gt 0) { Docker @('compose','-p','sparrow-public-proxy','-f',(Join-Path $proxy 'docker-compose.yml'),'stop') }
}
function Restore-Originals([object]$Record) {
    Write-Host 'Stopping the new public entry point before reverting backend configuration.'
    Stop-SharedProxy $Record
    foreach ($component in @($Record.Components)) {
        Verify-ProjectOwner $component
        $root = [string]$component.OriginalDirectory
        # `compose stop` uses the original file set even if the new overlay was
        # only partly installed; container selection remains project-scoped.
        $args = Compose-Args $component $false
        Docker ($args + @('stop'))
    }
    foreach ($component in @($Record.Components)) {
        $root = [string]$component.OriginalDirectory
        $config = Join-Path $root 'sparrow.conf'
        $before = Join-Path $backup "components/$($component.Component)/sparrow.conf"
        if (-not (Test-Path -LiteralPath $before -PathType Leaf) -or (File-Hash $before) -ne [string]$component.OriginalConfigSha256) {
            throw "The verified original configuration snapshot is no longer intact: $before"
        }
        $overlay = Join-Path $root 'docker-compose.shared-proxy.yml'
        if (-not (Test-Path -LiteralPath $config -PathType Leaf) -or
            (File-Hash $config) -notin @([string]$component.CutoverConfigSha256,[string]$component.OriginalConfigSha256)) {
            throw "Configuration $config was modified outside the cutover; refusing to overwrite it. New proxy is stopped. Recover manually using $before."
        }
        if (Test-Path -LiteralPath $overlay -PathType Leaf) {
            if ((File-Hash $overlay) -ne [string]$component.OverlaySha256) {
                throw "Overlay $overlay was modified; refusing to remove it automatically. New proxy is stopped."
            }
        }
        if ((File-Hash $config) -ne [string]$component.OriginalConfigSha256) {
            Copy-Item -LiteralPath $before -Destination $config -Force
            if ((File-Hash $config) -ne [string]$component.OriginalConfigSha256) { throw "Failed to restore original $config" }
        }
        if (Test-Path -LiteralPath $overlay -PathType Leaf) { Remove-Item -LiteralPath $overlay -Force }
    }
    $Record.State = 'ROLLED_BACK_ORIGINALS_STOPPED'
    Save-Transaction $Record
    # Both old standalone Public projects publish the SAME host TCP ports. It
    # is impossible to restart both simultaneously; do not pick one silently.
    if (@($Record.Components).Count -eq 1) {
        $component = @($Record.Components)[0]
        Write-Host "Restoring original standalone Public service: $($component.Component)."
        Docker ((Compose-Args $component $false) + @('up','-d','--no-build','--pull','never'))
        $Record.State = 'ROLLED_BACK_ORIGINAL_STARTED'
        Save-Transaction $Record
    } else {
        Write-Warning 'Two legacy standalone Public projects cannot both bind 80/443. Both remain STOPPED. Their configuration, containers and original named volumes remain intact. Start ONE original project manually after checking port ownership.'
    }
    Write-Host "Rollback finished: $($Record.State). Staged TLS volumes and the original data volumes have NOT been deleted."
}

$plan = Absolute $PlanDirectory
$transactionFile = Join-Path $plan 'cutover-transaction.json'
$readinessFile = Join-Path $plan 'cutover-readiness.json'
if (-not (Test-Path -LiteralPath $readinessFile -PathType Leaf)) { throw 'Readiness plan is missing.' }
$readiness = Get-Content -LiteralPath $readinessFile -Raw | ConvertFrom-Json
if ($readiness.State -ne 'READINESS_ONLY_NOT_AUTHORIZED_TO_CUT_OVER' -or $readiness.FormatVersion -ne 1) {
    throw 'Unexpected or incomplete readiness plan.'
}
$backup = Absolute ([string]$readiness.Snapshot)
$stage = Absolute ([string]$readiness.TlsStage)
$bundle = Absolute $PSScriptRoot
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI required.' }
# Plan-level lock, shared by apply and rollback in this plan. Other readiness
# plans cannot adopt this snapshot because stage/volume ownership is rechecked.
$lockPath = Join-Path $plan 'cutover.lock'
$lock = [System.IO.File]::Open($lockPath,[System.IO.FileMode]::OpenOrCreate,[System.IO.FileAccess]::ReadWrite,[System.IO.FileShare]::None)
try {
    if ($Rollback) {
        if (-not (Test-Path -LiteralPath $transactionFile -PathType Leaf)) { throw 'No transaction record to roll back.' }
        $transaction = Get-Content -LiteralPath $transactionFile -Raw | ConvertFrom-Json
        if ($transaction.FormatVersion -ne 1 -or $transaction.PlanDirectory -ne $plan -or
            $transaction.State -in @('ROLLED_BACK_ORIGINALS_STOPPED','ROLLED_BACK_ORIGINAL_STARTED')) {
            throw 'Transaction does not match this plan or has already been rolled back.'
        }
        Restore-Originals $transaction
        exit 0
    }
    if (Test-Path -LiteralPath $transactionFile) { throw 'A cutover transaction already exists; inspect or explicitly roll it back. Do not apply twice.' }
    if ((File-Hash (Join-Path $backup 'manifest.json')) -ne $readiness.SnapshotManifestSha256 -or
        (File-Hash (Join-Path $stage 'tls-stage.json')) -ne $readiness.TlsStageManifestSha256 -or
        (File-Hash (Join-Path $plan 'Caddyfile.candidate')) -ne $readiness.CandidateCaddySha256) {
        throw 'Offline snapshot, TLS stage or candidate proxy configuration changed since readiness.'
    }
    # An entirely NEW plan rechecks all original files, project ownership, ports
    # and unused staged volumes immediately before the first mutation.
    $recheck = Join-Path ([System.IO.Path]::GetTempPath()) ("sparrow-public-recheck-" + [Guid]::NewGuid().ToString('N'))
    try {
        & (Join-Path $bundle 'Test-SparrowCutoverReadiness.ps1') -BackupDirectory $backup -StageDirectory $stage -PlanDirectory $recheck
        if (-not $?) { throw 'Cutover readiness could not be reconfirmed.' }
        $current = Get-Content -LiteralPath (Join-Path $recheck 'cutover-readiness.json') -Raw | ConvertFrom-Json
        if ($current.CandidateCaddySha256 -ne $readiness.CandidateCaddySha256 -or
            $current.SnapshotManifestSha256 -ne $readiness.SnapshotManifestSha256 -or
            $current.TlsStageManifestSha256 -ne $readiness.TlsStageManifestSha256) {
            throw 'Readiness changed since the recorded plan.'
        }
    } finally {
        if (Test-Path -LiteralPath $recheck -PathType Container) { Remove-Item -LiteralPath $recheck -Recurse -Force }
    }
    $components = @($readiness.OriginalComponents)
    if ($components.Count -lt 1 -or $components.Count -gt 2) { throw 'Cutover requires one or two identified legacy Public components.' }
    # Verify in-place adoption compatibility. Keeping the ORIGINAL working
    # directory ensures Compose reuses its original named identities/data volumes.
    $prepared = @()
    foreach ($component in $components) {
        $root = Absolute ([string]$component.OriginalDirectory)
        $name = [string]$component.Component
        if ($name -notin @('community-node','control-plane') -or $component.Project -ne "sparrow-$name") {
            throw 'Unexpected component or Compose project in the readiness record.'
        }
        $snapshotConfig = Join-Path $backup "components/$name/sparrow.conf"
        $config = Join-Path $root 'sparrow.conf'
        $originalHash = File-Hash $snapshotConfig
        Assert-SourceFile $config $snapshotConfig $originalHash
        $originalCaddy = Join-Path $root 'Caddyfile'
        $bundleCaddy = Join-Path $bundle "$name/Caddyfile"
        $snapshotCaddy = Join-Path $backup "components/$name/Caddyfile"
        # A route difference might silently remove a custom/legacy endpoint.
        # This first migration path intentionally fails closed in that case.
        Assert-SourceFile $originalCaddy $snapshotCaddy (File-Hash $bundleCaddy)
        $overlay = Join-Path $root 'docker-compose.shared-proxy.yml'
        if (Test-Path -LiteralPath $overlay) { throw "An existing shared proxy overlay must not be overwritten: $overlay" }
        $sourceOverlay = Join-Path $bundle "$name/docker-compose.shared-proxy.yml"
        $newConfig = Set-SharedSetting $config
        $info = [PSCustomObject]@{
            Component=$name; Project=[string]$component.Project; OriginalDirectory=$root
            Hostname=[string]$component.Hostname; OriginalConfigSha256=$originalHash
            CutoverConfigSha256=''
            OverlaySha256=File-Hash $sourceOverlay
        }
        # Hash the exact UTF-8 bytes that will be written on Windows PowerShell 5.1.
        $tempConfig = Join-Path $plan "config-preview-$name.tmp"
        Write-Text $tempConfig $newConfig
        $info.CutoverConfigSha256 = File-Hash $tempConfig
        Remove-Item -LiteralPath $tempConfig -Force
        Docker ((Compose-Args $info $false) + @('config','--quiet'))
        # Validate overlay in its actual ORIGINAL Compose project directory.
        Docker ((Compose-Args $info $false) + @('-f',$sourceOverlay,'config','--quiet'))
        $prepared += [PSCustomObject]@{ Info=$info; NewConfig=$newConfig; SourceOverlay=$sourceOverlay }
    }
    # Keep the proxy's configuration and ORIGINAL index pages in a private
    # transaction-owned directory; no bundle or old deployment is overwritten.
    $proxyRoot = Join-Path $plan 'proxy'
    if (Test-Path -LiteralPath $proxyRoot) { throw 'Plan already has a proxy directory; do not overwrite it.' }
    foreach ($name in @('community-node','control-plane')) {
        $folder = Join-Path $plan $name
        if (Test-Path -LiteralPath $folder) { throw "Plan has an unexpected pre-existing directory: $folder" }
    }
    $transaction = [PSCustomObject]@{
        FormatVersion=1; PlanDirectory=$plan; BackupDirectory=$backup; TlsStageDirectory=$stage
        ProxyDirectory=$proxyRoot; State='PREPARED_NO_SERVICES_CHANGED'
        Components=@($prepared | ForEach-Object { $_.Info })
        Notes='Rollback never removes original Docker volumes or staged TLS volumes. A two-component legacy Public rollback leaves both originals stopped because they cannot both publish 80/443.'
    }
    Save-Transaction $transaction  # Recovery instructions exist BEFORE changes.
    try {
        New-Item -ItemType Directory -Path $proxyRoot -ErrorAction Stop | Out-Null
        Copy-Item -LiteralPath (Join-Path $bundle 'public-proxy/docker-compose.yml') -Destination (Join-Path $proxyRoot 'docker-compose.yml')
        Copy-Item -LiteralPath (Join-Path $plan 'Caddyfile.candidate') -Destination (Join-Path $proxyRoot 'Caddyfile')
        foreach ($name in @('community-node','control-plane')) {
            $source = @($prepared | Where-Object { $_.Info.Component -eq $name })
            $index = if ($source.Count -eq 1) { Join-Path $source[0].Info.OriginalDirectory 'index.html' } else { Join-Path $bundle "$name/index.html" }
            New-Item -ItemType Directory -Path (Join-Path $plan $name) | Out-Null
            Copy-Item -LiteralPath $index -Destination (Join-Path $plan "$name/index.html")
        }
        foreach ($part in $prepared) {
            Copy-Item -LiteralPath $part.SourceOverlay -Destination (Join-Path $part.Info.OriginalDirectory 'docker-compose.shared-proxy.yml')
            if ((File-Hash (Join-Path $part.Info.OriginalDirectory 'docker-compose.shared-proxy.yml')) -ne $part.Info.OverlaySha256) { throw 'Overlay copy verification failed.' }
            Write-Text (Join-Path $part.Info.OriginalDirectory 'sparrow.conf') $part.NewConfig
            if ((File-Hash (Join-Path $part.Info.OriginalDirectory 'sparrow.conf')) -ne $part.Info.CutoverConfigSha256) { throw 'Settings update verification failed.' }
        }
        Write-Phase 'FILES_PREPARED'
        $network = @(& docker network inspect sparrow-public-edge --format '{{.Name}}' 2>$null)
        if ($LASTEXITCODE -ne 0) { Docker @('network','create','--driver','bridge','sparrow-public-edge') }
        foreach ($part in $prepared) {
            Verify-ProjectOwner $part.Info
            Docker ((Compose-Args $part.Info $true) + @('up','-d','--no-build','--pull','never'))
        }
        Write-Phase 'COMPONENTS_STARTED'
        $proxyIds = @(Docker-Result @('ps','--all','--filter','label=com.docker.compose.project=sparrow-public-proxy','--format','{{.ID}}'))
        if ($proxyIds.Count -ne 0) { throw 'A shared proxy appeared during the cutover. Refusing to adopt its containers.' }
        Docker @('compose','-p','sparrow-public-proxy','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'config','--quiet')
        Docker @('compose','-p','sparrow-public-proxy','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'up','-d','--no-build','--pull','never')
        Write-Phase 'PUBLIC_PROXY_STARTED'
        if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) { throw 'curl.exe is required to verify local HTTPS without weakening TLS validation.' }
        foreach ($part in $prepared) {
            $path = if ($part.Info.Component -eq 'community-node') { '/health/gateway' } else { '/health/registry' }
            $url = "https://$($part.Info.Hostname)$path"
            $ok = $false
            for ($attempt=0; $attempt -lt 12; $attempt++) {
                & curl.exe --silent --show-error --fail --max-time 12 --resolve "$($part.Info.Hostname):443:127.0.0.1" $url 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) { $ok = $true; break }
                Start-Sleep -Seconds 5
            }
            if (-not $ok) { throw "Local verified HTTPS probe failed: $url. External reachability has not been tested." }
            Write-Host "Local verified HTTPS OK: $url"
        }
        Write-Phase 'ACTIVE_LOCAL_HTTPS_VERIFIED_EXTERNAL_PENDING'
        Write-Warning 'Shared proxy is active and locally HTTPS-verified. EXTERNAL DNS/TLS/WebSocket, FCM, restart and database recovery remain untested. Preserve original projects/volumes and the offline snapshot.'
    } catch {
        $failure = $_.Exception.Message
        Write-Warning "Cutover activation failed: $failure. Attempting a non-destructive rollback."
        try { Restore-Originals $transaction } catch {
            $transaction.State = 'RECOVERY_REQUIRED'
            Save-Transaction $transaction
            Write-Warning "Automatic rollback could not complete: $($_.Exception.Message). Original data volumes were not deleted. Use this plan and its verified offline snapshot for manual recovery."
        }
        throw "Cutover failed: $failure. Transaction state: $($transaction.State)."
    }
} finally {
    $lock.Dispose()
}
