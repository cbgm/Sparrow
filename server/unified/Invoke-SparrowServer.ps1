[CmdletBinding()]
param(
    [ValidateSet('Start', 'Stop', 'Restart', 'Status', 'Logs', 'Verify', 'Preflight', 'Backup', 'Attach')][string]$Action = 'Status',
    [ValidateSet('Node', 'ControlPlane', 'Combined', 'Proxy')][string]$Component = 'Combined',
    [ValidateSet('lan', 'public')][string]$Mode = 'lan',
    [string]$NodeDomain = '',
    [string]$ControlPlaneDomain = '',
    [ValidateSet('Manual','CaddyAutomatic')][string]$PublicHostnameMode = 'Manual',
    [string]$DirectoryUrl = '',
    [string]$ImagePrefix = 'ghcr.io/cbgm/sparrow',
    [string]$ImageTag = 'latest',
    [string]$FirebaseCredentialsPath = '',
    [ValidateSet('Enabled', 'Disabled')][string]$FirebaseMode = 'Enabled',
    [string]$ExistingNodeDirectory = '',
    [string]$ExistingControlPlaneDirectory = '',
    [string]$ExistingProxyDirectory = '',
    [string]$BackupDirectory = '',
    [string]$PlanDirectory = '',
    [ValidateSet('Any', 'Running', 'Stopped')][string]$ExpectedState = 'Any',
    [string]$ResultPath = '',
    [string]$FailurePath = '',
    [ValidateSet('Keep','ReplaceTestData')][string]$ExistingDeployment = 'Keep',
    [string]$ReplacementConfirmation = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-SparrowDockerLabel.ps1')
. (Join-Path $PSScriptRoot 'Get-SparrowVerifiedDirectory.ps1')
$root = $PSScriptRoot
$nodeRoot = Join-Path $root 'community-node'
$cpRoot = Join-Path $root 'control-plane'
$proxyRoot = Join-Path $root 'public-proxy'
$attachmentFile = Join-Path $root '.sparrow-attached.json'
$nodeSelected = $Component -in @('Node', 'Combined')
$cpSelected = $Component -in @('ControlPlane', 'Combined')
$shared = $Mode -eq 'public'

function Start-SparrowDirectoryBackgroundSync {
    $cpConfig = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
    if ($cpConfig['MODE'] -ne 'public' -or
        [string]::IsNullOrWhiteSpace([string]$cpConfig['CONTROL_PLANE_DIRECTORY_URL']) -or
        [string]::IsNullOrWhiteSpace([string]$cpConfig['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']) -or
        -not (Test-Path -LiteralPath (Join-Path $proxyRoot 'Caddyfile') -PathType Leaf)) { return }
    # The dedicated discovery container cannot block the existing server on
    # image-build failure or directory downtime; it has no server secrets.
    try {
        $composeFile = Join-Path $proxyRoot 'docker-compose.yml'
        & docker compose -f $composeFile --profile directory up -d --no-deps directory-sync 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Host 'Directory background sync unavailable; local server remains online.' }
        else { Write-Host 'Directory background sync enabled (15-minute interval).' }
    } catch {
        Write-Host "Directory background sync deferred: $($_.Exception.Message)"
    }
}

function Refresh-PublicControlPlaneDirectory {
    # Separate durable CP cache: never replace an authenticated snapshot with
    # an unsigned/invalid result, and never block local chat on an outage.
    $settings = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
    if (-not $settings['CONTROL_PLANE_DIRECTORY_URL'] -or -not $settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']) { return }
    try {
        $null = @(Get-SparrowVerifiedDirectoryUrls `
            -DirectoryUrl $settings['CONTROL_PLANE_DIRECTORY_URL'] `
            -PinnedPublicKey $settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY'] `
            -CacheFile (Join-Path $cpRoot '.control-plane-directory-verified.json') `
            -ClientScript (Join-Path $root 'control_plane_directory_client.py'))
        Write-Host 'Control Plane signed-directory cache refreshed or reused.'
    } catch {
        Write-Host 'Control Plane directory unavailable; local server and previous verified cache remain unchanged.'
    }
}

function Register-PublicControlPlaneInDirectory {
    # This is an opt-in, best-effort registration attempt. Admin approval
    # remains central and directory failure never prevents chat delivery.
    $settings = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
    if ($settings['MODE'] -ne 'public' -or
        [string]::IsNullOrWhiteSpace($settings['CONTROL_PLANE_DIRECTORY_URL']) -or
        [string]::IsNullOrWhiteSpace($settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']) -or
        [string]::IsNullOrWhiteSpace($settings['PUBLIC_DOMAIN'])) { return }
    $identity = Join-Path $cpRoot 'secrets/registry-root.identity'
    if (-not (Test-Path -LiteralPath $identity -PathType Leaf)) { return }
    $marker = Join-Path $cpRoot '.directory-registration-last-attempt'
    if ((Test-Path -LiteralPath $marker -PathType Leaf) -and
        (Get-Item -LiteralPath $marker).LastWriteTime -ge (Get-Item -LiteralPath $identity).LastWriteTime -and
        (Get-Item -LiteralPath $marker).LastWriteTime -ge (Get-Item -LiteralPath (Join-Path $cpRoot 'sparrow.conf')).LastWriteTime -and
        ((Get-Date) - (Get-Item -LiteralPath $marker).LastWriteTime).TotalMinutes -lt 9) { return }
    # Docker contains the pinned crypto dependency; no host Python required.
    # The registration job is one-shot, with no admin credential, Docker socket,
    # or access to any secret other than this existing single-file identity.
    $composeFile = Join-Path $proxyRoot 'docker-compose.yml'
    try {
        $buildOutput = @(& docker compose -f $composeFile --profile registration build directory-register 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Host 'Directory registration deferred: client image could not be built. Local server remains online.'
            return
        }
        $directoryArgument = 'CONTROL_PLANE_DIRECTORY_URL=' + [string]$settings['CONTROL_PLANE_DIRECTORY_URL']
        $pinArgument = 'CONTROL_PLANE_DIRECTORY_PUBLIC_KEY=' + [string]$settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']
        $planeArgument = 'SPARROW_REGISTRATION_PLANE_URL=https://' + [string]$settings['PUBLIC_DOMAIN']
        $result = @(& docker compose -f $composeFile --profile registration run --rm --no-deps `
            -e $directoryArgument -e $pinArgument -e $planeArgument directory-register 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Host 'Directory registration deferred; local Control Plane continues operating.'
            return
        }
        Write-Host "Directory registration: $($result -join ' ')"
        [System.IO.File]::WriteAllText($marker, (Get-Date).ToUniversalTime().ToString('o'))
    } catch {
        Write-Host "Directory registration deferred; local Control Plane remains available: $($_.Exception.Message)"
    }
}

function Read-Properties([string]$Path) {
    $properties = @{}
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        foreach ($line in (Get-Content -LiteralPath $Path)) {
            if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') { $properties[$Matches[1]] = $Matches[2] }
        }
    }
    return $properties
}
function Write-Properties([string]$Path, [hashtable]$Updates) {
    $old = Read-Properties $Path
    foreach ($key in $Updates.Keys) { $old[$key] = [string]$Updates[$key] }
    $lines = @('# Sparrow Server settings; do not remove existing server identities or secret files.')
    foreach ($key in ($old.Keys | Sort-Object)) { $lines += "$key=$($old[$key])" }
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $backup = "$Path.backup.$(Get-Date -Format yyyyMMddHHmmss)"
        Copy-Item -LiteralPath $Path -Destination $backup -ErrorAction Stop
    }
    $tmp = "$Path.pending"
    [System.IO.File]::WriteAllLines($tmp, $lines, [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $tmp -Destination $Path -Force
}
function Check-Domain([string]$Domain) {
    if ($Domain.Length -gt 253 -or $Domain -notmatch '^(?=.{1,253}$)[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)+$') {
        throw "Invalid public hostname: $Domain. Enter a hostname, without a scheme, path or port."
    }
    return $Domain.ToLowerInvariant()
}
# Caddy manages HTTPS; sslip.io provides the public DNS names (not a Caddy feature).
# Resolve the WAN IPv4 in the worker, before changing any Docker or identity state.
function Test-PublicIPv4([string]$Value) {
    $address = $null
    if (-not [System.Net.IPAddress]::TryParse($Value, [ref]$address) -or
        $address.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork -or
        $address.ToString() -ne $Value) { return $false }
    $o = @($address.GetAddressBytes() | ForEach-Object { [int]$_ })
    if ($o[0] -eq 0 -or $o[0] -eq 10 -or $o[0] -eq 127 -or $o[0] -ge 224 -or
        ($o[0] -eq 100 -and $o[1] -ge 64 -and $o[1] -le 127) -or
        ($o[0] -eq 169 -and $o[1] -eq 254) -or
        ($o[0] -eq 172 -and $o[1] -ge 16 -and $o[1] -le 31) -or
        ($o[0] -eq 192 -and ($o[1] -eq 168 -or ($o[1] -eq 0 -and $o[2] -eq 0) -or
            ($o[1] -eq 0 -and $o[2] -eq 2))) -or
        ($o[0] -eq 198 -and (($o[1] -eq 18 -or $o[1] -eq 19) -or
            ($o[1] -eq 51 -and $o[2] -eq 100))) -or
        ($o[0] -eq 203 -and $o[1] -eq 0 -and $o[2] -eq 113)) { return $false }
    return $true
}
function Get-WanIPv4 {
    foreach ($uri in @('https://api.ipify.org', 'https://ipv4.icanhazip.com')) {
        try {
            $value = ([string](Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 7 -UseBasicParsing -ErrorAction Stop)).Trim()
            if (Test-PublicIPv4 $value) { return $value }
        } catch { Write-Host "Public IPv4 service unavailable: $uri" }
    }
    throw 'Cannot detect a public IPv4 address. Check internet/CGNAT or choose manual public hostnames; no installation state was changed.'
}
function Set-CaddyAutomaticHostnames {
    if ($PublicHostnameMode -ne 'CaddyAutomatic' -or -not $shared) { return }
    # Reuse installed hostnames exactly: WAN IP changes require a reviewed DNS,
    # TLS and signed-directory cutover, NOT a silent in-place hostname change.
    $freshSelected = @()
    $generatedHostnames = @()
    foreach ($selectedService in @(@{ Selected=$nodeSelected; Root=$nodeRoot; Role='Node' },
                            @{ Selected=$cpSelected; Root=$cpRoot; Role='ControlPlane' })) {
        if (-not $selectedService.Selected) { continue }
        # Explicit fresh replacement ignores stale LAN/Public metadata: it will
        # erase the selected TEST deployment, NOT try to migrate it.
        if ($ExistingDeployment -eq 'ReplaceTestData') {
            $freshSelected += $selectedService.Role
            continue
        }
        if (Test-Path -LiteralPath (Join-Path $selectedService.Root '.env.runtime') -PathType Leaf) {
            $config = Read-Properties (Join-Path $selectedService.Root 'sparrow.conf')
            $project = if ($selectedService.Role -eq 'Node') { 'sparrow-community-node' } else { 'sparrow-control-plane' }
            $effectiveMode = Get-SafeInstalledPublicMode $selectedService.Root $project
            if ($effectiveMode -eq 'public') {
                if ($selectedService.Role -eq 'Node') { $script:NodeDomain = $config['PUBLIC_DOMAIN'] }
                else { $script:ControlPlaneDomain = $config['PUBLIC_DOMAIN'] }
            } elseif ($effectiveMode -eq 'lan' -and $Component -eq 'Combined' -and $nodeSelected -and $cpSelected) {
                # A broken prior install may have left public metadata without
                # actually converting the LAN Docker deployment. Its live
                # Compose labels, not a missing hostname, determine eligibility.
                # The full migration still performs all ownership/data checks.
                $freshSelected += $selectedService.Role
            } else {
                throw "Existing $($selectedService.Role) is a LAN deployment but selected component is '$Component'. Select Combined to convert both installed components together; no data was changed."
            }
        } else { $freshSelected += $selectedService.Role }
    }
    if ($freshSelected.Count -eq 0) { return }
    $wan = Get-WanIPv4
    $dashed = $wan.Replace('.', '-')
    if ('Node' -in $freshSelected) { $script:NodeDomain = "node-$dashed.sslip.io"; $generatedHostnames += $NodeDomain }
    if ('ControlPlane' -in $freshSelected) { $script:ControlPlaneDomain = "control-$dashed.sslip.io"; $generatedHostnames += $ControlPlaneDomain }
    foreach ($hostname in $generatedHostnames) {
        try {
            $addresses = @([System.Net.Dns]::GetHostAddresses($hostname) | ForEach-Object { $_.ToString() })
            if ($wan -notin $addresses) { throw "DNS resolved to $($addresses -join ', ') instead of $wan" }
        } catch { throw "Automatic hostname $hostname does not resolve to $wan yet. Check DNS before installing: $($_.Exception.Message)" }
    }
    Write-Host "Caddy automatic DNS selected: WAN IPv4 $wan; sslip.io generates free DNS names (not owned by Caddy)."
    Write-Host 'Caddy must still obtain publicly trusted TLS certificates; DNS/port forwarding/CGNAT/external reachability are not yet verified.'
}
function Show-PublicEndpoints {
    $cp = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
    $node = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
    if (-not (($cp['MODE'] -eq 'public' -and $cp['PUBLIC_DOMAIN'] -and (Test-Path -LiteralPath (Join-Path $cpRoot '.env.runtime') -PathType Leaf)) -or
               ($node['MODE'] -eq 'public' -and $node['PUBLIC_DOMAIN'] -and (Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf)))) { return }
    Write-Host '==== Configured public addresses (copy after install; external reachability NOT verified) ===='
    if ($cp['MODE'] -eq 'public' -and $cp['PUBLIC_DOMAIN'] -and
        (Test-Path -LiteralPath (Join-Path $cpRoot '.env.runtime') -PathType Leaf)) {
        Write-Host "Control Plane: https://$($cp['PUBLIC_DOMAIN'])"
        Write-Host "Signed directory: https://$($cp['PUBLIC_DOMAIN'])/v1/nodes"
    }
    if ($node['MODE'] -eq 'public' -and $node['PUBLIC_DOMAIN'] -and
        (Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf)) {
        Write-Host "Community Node: https://$($node['PUBLIC_DOMAIN'])"
        Write-Host "WebSocket gateway: wss://$($node['PUBLIC_DOMAIN'])/v1/gateway"
    }
}
function Get-ComponentName([string]$Directory) {
    if ($Directory -eq $cpRoot) { return 'Control Plane' }
    return 'Community Node'
}
function Get-ConfiguredComponent([string]$Directory) {
    $cfg = Read-Properties (Join-Path $Directory 'sparrow.conf')
    $runtime = Join-Path $Directory '.env.runtime'
    return [PSCustomObject]@{
        Name = Get-ComponentName $Directory
        Directory = $Directory
        Installed = (Test-Path -LiteralPath $runtime -PathType Leaf)
        Mode = $cfg['MODE']
        Hostname = $cfg['PUBLIC_DOMAIN']
        Shared = $cfg['SHARED_PROXY'] -eq 'true'
    }
}
function Normalize-ComposeWorkdir([string]$Value) {
    $clean = $Value.Trim().TrimEnd('/', '\')
    # Docker Desktop may describe Windows bind paths through its Linux mount.
    if ($clean -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
        $clean = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
    }
    if ($clean -match '^[a-zA-Z]:[\\/]') {
        return [System.IO.Path]::GetFullPath($clean).TrimEnd('\')
    }
    # A workdir from another OS cannot be proven to belong to this bundle.
    return $clean
}
function Assert-DeploymentsNotRunningElsewhere {
    # A second bundle targeting an already-running project must not replace the
    # live installation's bind mounts, config or Compose application directory.
    foreach ($component in @(@{ Name='sparrow-community-node'; Root=$nodeRoot },
                             @{ Name='sparrow-control-plane'; Root=$cpRoot })) {
        $ids = @(& docker ps --all --filter "label=com.docker.compose.project=$($component.Name)" --format '{{.ID}}' 2>$null)
        if ($LASTEXITCODE -ne 0) { throw 'Could not inspect existing Compose projects.' }
        if (($component.Name -eq 'sparrow-community-node' -and -not $nodeSelected) -or
            ($component.Name -eq 'sparrow-control-plane' -and -not $cpSelected)) { continue }
        $ours = Normalize-ComposeWorkdir $component.Root
        foreach ($id in $ids) {
            $workdir = Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir'
            if ([string]::IsNullOrWhiteSpace($workdir)) {
                throw "Cannot verify the existing Compose working directory for $($component.Name) ($id)."
            }
            $actual = Normalize-ComposeWorkdir $workdir
            if (-not $actual.Equals($ours, [StringComparison]::OrdinalIgnoreCase)) {
                throw "$($component.Name) is already managed from $actual. Do not initialize or start a replacement project from $ours. Preserve its original directory and use the adoption preflight."
            }
        }
    }
}
function Show-AdoptionPreflight([string]$Source, [string]$Destination, [string]$Project) {
    if ([string]::IsNullOrWhiteSpace($Source)) { return }
    $sourcePath = [System.IO.Path]::GetFullPath($Source)
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Container)) { throw "Existing deployment folder was not found: $sourcePath" }
    $originalConfig = Read-Properties (Join-Path $sourcePath 'sparrow.conf')
    $originalRuntime = Read-Properties (Join-Path $sourcePath '.env.runtime')
    if (-not $originalConfig['MODE'] -or -not $originalRuntime.Count) {
        throw "Existing deployment lacks sparrow.conf or .env.runtime: $sourcePath. No changes made."
    }
    if ($originalConfig['MODE'] -eq 'public') {
        Write-Host "PUBLIC CUTOVER REQUIRED: $Project currently binds the public ports. Its Caddy certificate volumes and DNS must be backed up and cut over deliberately. No automatic adoption or service interruption was performed."
    }
    $sourceProject = $(if ($Project -eq 'sparrow-control-plane') { $originalRuntime['CONTROL_PLANE_PROJECT_NAME'] } else { $originalRuntime['COMMUNITY_NODE_PROJECT_NAME'] })
    if ($sourceProject -and $sourceProject -ne $Project) { throw "Unexpected Compose project $sourceProject at $sourcePath. This installer cannot safely adopt it." }
    foreach ($required in @('sparrow.conf', '.env.runtime', 'secrets')) {
        if (-not (Test-Path -LiteralPath (Join-Path $sourcePath $required))) { throw "Missing existing deployment state: $required. No changes made." }
    }
    Write-Host "EXISTING: $Project in $sourcePath; mode=$($originalConfig['MODE']); target=$Destination"
    Write-Host 'PRECHECK ONLY: no files, Docker resources, identities, secrets or certificates have been copied or changed.'
    Write-Host 'The existing installation must be backed up and its project working directory reconciled before adoption. Do not start a second bundle using the same project/volumes.'
}
function Show-Preflight {
    Write-Host '==== Sparrow Server — read-only deployment preflight ===='
    Show-AdoptionPreflight $ExistingNodeDirectory $nodeRoot 'sparrow-community-node'
    Show-AdoptionPreflight $ExistingControlPlaneDirectory $cpRoot 'sparrow-control-plane'
    foreach ($directory in @($nodeRoot, $cpRoot)) {
        $component = Get-ConfiguredComponent $directory
        Write-Host "$($component.Name): installed=$($component.Installed); mode=$($component.Mode); sharedProxy=$($component.Shared); hostname=$($component.Hostname)"
        if ($component.Installed) {
            $runtime = Read-Properties (Join-Path $directory '.env.runtime')
            $project = if ($directory -eq $nodeRoot) { $runtime['COMMUNITY_NODE_PROJECT_NAME'] } else { $runtime['CONTROL_PLANE_PROJECT_NAME'] }
            Write-Host "  Compose project: $project; local state path: $directory"
        }
    }
    foreach ($project in @('sparrow-community-node', 'sparrow-control-plane', 'sparrow-public-proxy')) {
        Write-Host "==== Persistent Docker volumes for $project ===="
        $volumes = @(& docker volume ls --quiet --filter "name=^${project}_" 2>$null)
        if ($LASTEXITCODE -ne 0) { throw "Could not inspect $project volumes." }
        $filtered = @($volumes | Where-Object { $_ -like "${project}_*" })
        if ($filtered.Count -eq 0) { Write-Host 'No matching named volumes detected.' }
        foreach ($volume in $filtered) { Write-Host "  $volume (metadata only; no database snapshot taken)" }
    }
    Write-Host '==== Compose-managed containers and published ports ===='
    & docker ps --all --filter 'label=com.docker.compose.project' --format 'container={{.Names}} status={{.Status}} ports={{.Ports}}' 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect existing containers.' }
    Write-Host 'No migration or deployment changes were made.'
}
function Show-ComponentStatus([string]$Directory) {
    $component = Get-ConfiguredComponent $Directory
    Write-Host "==== $($component.Name) ===="
    if (-not $component.Installed) {
        $project = if ($Directory -eq $cpRoot) { 'sparrow-control-plane' } else { 'sparrow-community-node' }
        $ids = @(& docker ps --all --filter "label=com.docker.compose.project=$project" --format '{{.ID}}' 2>$null)
        if ($LASTEXITCODE -ne 0) { throw "Could not inspect Docker project $project." }
        $volumes = @(& docker volume ls --quiet --filter "name=^${project}_" 2>$null |
            Where-Object { $_ -like "${project}_*" })
        if ($LASTEXITCODE -ne 0) { throw "Could not inspect Docker volumes for $project." }
        Write-Host "No local .env.runtime in $Directory; this folder cannot manage the existing deployment."
        if ($ids.Count -eq 0 -and $volumes.Count -eq 0) {
            Write-Host 'No matching containers or named data volumes in Docker. Fresh installation can create new configuration.'
        } else {
            Write-Host "Docker still has $($ids.Count) container(s) and $($volumes.Count) named volume(s) for $project."
            foreach ($id in $ids) {
                $owner = Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir'
                if (-not $owner) { $owner = '(unknown)' }
                Write-Host "  Container: $id; original Compose directory: $owner"
            }
            foreach ($volume in $volumes) { Write-Host "  Data volume: $volume" }
            Write-Host 'UPDATE: apply a newer bundle IN PLACE to the original installation folder, keeping .env.runtime, sparrow.conf and secrets.'
            Write-Host 'MISSING ORIGINAL CONFIG: retrieve its original folder/backup; if all data is disposable, use the separately confirmed test reset before fresh Install / Start.'
        }
        return
    }
    Write-Host "Mode: $($component.Mode); shared public proxy: $($component.Shared)"
    $endpoint = if ($component.Mode -eq 'public') { "https://$($component.Hostname)" } elseif ($Directory -eq $cpRoot) { 'http://localhost:8390' } else { 'http://localhost:8490' }
    Write-Host "Configured endpoint: $endpoint (DNS/external availability not verified)"
    Invoke-ComponentCommand $Directory @('ps', '--all')
    if ($Directory -eq $cpRoot) {
        $runtime = Read-Properties (Join-Path $Directory '.env.runtime')
        $path = [string]$runtime['FIREBASE_ADMIN_CREDENTIALS']
        if ($path -and (Test-Path -LiteralPath $path -PathType Leaf)) {
            Write-Host 'FCM: service-account file configured. Authorization, FCM acceptance and delivery NOT verified.'
        } else {
            Write-Host 'FCM: not configured. Core encrypted messaging does not depend on push.'
        }
    }
}
function Require-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is not available.' }
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker engine is not running.' }
}
function Invoke-Docker([string[]]$Arguments) {
    Write-Host "Docker: $($Arguments -join ' ')"
    # Docker Compose writes pull progress and diagnostics to stderr even when a
    # command succeeds. Under Windows PowerShell 5.1, Stop + 2>&1 turns those
    # lines into terminating NativeCommandError records and hides the ACTUAL
    # registry error. Stream both channels and check the native exit code.
    $previousPreference = $ErrorActionPreference
    $exitCode = -1
    $recentOutput = [System.Collections.Generic.List[string]]::new()
    try {
        $ErrorActionPreference = 'Continue'
        & docker @Arguments 2>&1 | ForEach-Object {
            $line = [string]$_
            Write-Host $line
            if ($recentOutput.Count -ge 12) { $recentOutput.RemoveAt(0) }
            $recentOutput.Add($line)
        }
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        $lastLines = ($recentOutput | Select-Object -Last 6) -join '; '
        throw "Docker command failed (exit $exitCode). $lastLines"
    }
}
function Get-ComposeArguments([string]$Directory) {
    $conf = Read-Properties (Join-Path $Directory 'sparrow.conf')
    $runtimeFile = Join-Path $Directory '.env.runtime'
    if (-not (Test-Path -LiteralPath $runtimeFile -PathType Leaf)) {
        throw "No installed deployment found in $Directory. Start that component first."
    }
    $composeArgs = @('compose', '--env-file', $runtimeFile, '-f', (Join-Path $Directory 'docker-compose.yml'))
    if ($conf['PUBLIC_TRANSITION'] -eq 'lan-in-place') {
        # Preserve the exact LAN Compose layering in use before conversion.
        # In particular, do not introduce the production override, which
        # changes database authentication and the Control Plane signing volume.
        if ($conf['PUBLIC_TRANSITION_COMPOSE'] -eq 'release') {
            $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.release.yml'))
        } elseif ($conf['PUBLIC_TRANSITION_COMPOSE'] -ne 'base') {
            throw "Unrecognized LAN transition Compose configuration in $Directory."
        }
        $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.public-transition.yml'))
    } else {
        $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.release.yml'))
        if ($conf['MODE'] -eq 'public') { $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.production.yml')) }
        if ($conf['MODE'] -eq 'public' -and $conf['SHARED_PROXY'] -eq 'true') { $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.shared-proxy.yml')) }
    }
    if ($Directory -eq $cpRoot) {
        $runtime = Read-Properties $runtimeFile
        if (-not [string]::IsNullOrWhiteSpace($runtime['FIREBASE_ADMIN_CREDENTIALS']) -and
            (Test-Path -LiteralPath $runtime['FIREBASE_ADMIN_CREDENTIALS'] -PathType Leaf)) {
            $composeArgs += @('-f', (Join-Path $Directory 'docker-compose.firebase.yml'))
        }
    }
    return ,$composeArgs
}
function Invoke-ComponentCommand([string]$Directory, [string[]]$Command) {
    $composeArgs = Get-ComposeArguments $Directory
    Invoke-Docker -Arguments ($composeArgs + $Command)
}
function Start-Component([string]$Directory, [string]$Bootstrap) {
    $script = Join-Path $Directory $Bootstrap
    if (-not (Test-Path -LiteralPath $script -PathType Leaf)) { throw "Missing component installer: $script" }
    Write-Host "Installing/starting $(Split-Path -Leaf $Directory) ..."
    & powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File $script -Headless 2>&1 |
        ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) { throw "Component installer failed: $Directory (exit $LASTEXITCODE). Existing volumes were not deleted." }
}
function Start-OrUpdateComponent([string]$Directory, [string]$Bootstrap, [string[]]$BackendServices) {
    $runtimePath = Join-Path $Directory '.env.runtime'
    if (-not (Test-Path -LiteralPath $runtimePath -PathType Leaf)) {
        # Fresh install: the original bootstrap still generates credentials,
        # identities and volumes and fetches its required images.
        Start-Component $Directory $Bootstrap
        return
    }
    # Existing installation: no bootstrap, no regenerated secrets, and never
    # pull/recreate PostgreSQL, Redis or Caddy just to update backend images.
    $config = Read-Properties (Join-Path $Directory 'sparrow.conf')
    if ($config['PUBLIC_TRANSITION'] -eq 'lan-in-place' -and
        $config['PUBLIC_TRANSITION_COMPOSE'] -eq 'base') {
        # A legacy base-only Control Plane may still use the original registry
        # identity volume. Switching it to the GHCR release Compose override
        # would silently change its signing identity. Keep it running as-is.
        $composeArgs = Get-ComposeArguments $Directory
        Invoke-Docker -Arguments ($composeArgs + @('config', '--quiet'))
        Invoke-Docker -Arguments ($composeArgs + @('up', '-d', '--no-deps', '--pull', 'never') + $BackendServices)
        Write-Host 'Legacy base-only backend retained without image upgrade; migrating its signing identity requires a separate backed-up release transition.'
        return
    }
    $runtime = Read-Properties $runtimePath
    if (-not $runtime.Count) { throw "Missing installed runtime configuration in $Directory." }
    $project = if ($Directory -eq $cpRoot) { 'sparrow-control-plane' } else { 'sparrow-community-node' }
    $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$project")
    if ($LASTEXITCODE -ne 0 -or $ids.Count -eq 0) {
        throw "Existing $project runtime was found but no owned containers exist. Refusing to recreate an ambiguous deployment."
    }
    $ours = Normalize-ComposeWorkdir $Directory
    foreach ($id in $ids) {
        $actual = Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir'
        if ([string]::IsNullOrWhiteSpace($actual) -or
            -not (Normalize-ComposeWorkdir $actual).Equals($ours, [StringComparison]::OrdinalIgnoreCase)) {
            throw "$project container $id is not owned by $Directory. Do not update another installation."
        }
    }
    foreach ($service in $BackendServices) {
        $serviceIds = @(& docker ps -aq --filter "label=com.docker.compose.project=$project" --filter "label=com.docker.compose.service=$service")
        if ($LASTEXITCODE -ne 0 -or $serviceIds.Count -eq 0) {
            throw "$project is missing service $service. Refusing to create a partial replacement during update."
        }
    }
    # The image selection in the manager must reach Compose's runtime env,
    # not merely sparrow.conf. Keep all other runtime keys and secret paths.
    if ($runtime['SPARROW_IMAGE_PREFIX'] -ne $config['SPARROW_IMAGE_PREFIX'] -or
        $runtime['SPARROW_IMAGE_TAG'] -ne $config['SPARROW_IMAGE_TAG']) {
        Write-Properties $runtimePath @{
            SPARROW_IMAGE_PREFIX = $config['SPARROW_IMAGE_PREFIX']
            SPARROW_IMAGE_TAG = $config['SPARROW_IMAGE_TAG']
        }
    }
    $composeArgs = Get-ComposeArguments $Directory
    Invoke-Docker -Arguments ($composeArgs + @('config', '--quiet'))
    if ($Directory -eq $cpRoot) {
        $runtime = Read-Properties $runtimePath
        if (-not [string]::IsNullOrWhiteSpace($runtime['FIREBASE_ADMIN_CREDENTIALS']) -and
            (Test-Path -LiteralPath $runtime['FIREBASE_ADMIN_CREDENTIALS'] -PathType Leaf)) {
            # Compare the exact Compose file path, not a regex against serialized arguments.
            # The previous double-escaped regex rejected the correctly appended file.
            if ($composeArgs -notcontains (Join-Path $Directory 'docker-compose.firebase.yml')) {
                throw 'Firebase credentials are saved but docker-compose.firebase.yml was not applied.'
            }
            Write-Host 'Firebase Compose override enabled for the existing Control Plane push service.'
        }
    }
    Write-Host "Fetching the latest selected $project backend images from GHCR ..."
    Invoke-Docker -Arguments ($composeArgs + @('pull') + $BackendServices)
    # 'up' rather than 'restart' recreates a backend only if its image/config
    # actually changed; --no-deps prevents database/redis replacement.
    Invoke-Docker -Arguments ($composeArgs + @('up', '-d', '--no-deps', '--pull', 'never') + $BackendServices)
    Write-Host "$project backend images applied; persistent volumes and existing server secrets were not removed."
}
function Sync-InstalledNodeDiscovery([string]$RequestedDirectoryUrl) {
    # Install/Start must be able to refresh a changed JSON directory on an
    # installed node. The old implementation wrote these URLs only on the first
    # bootstrap, leaving gateway /v1/control-planes advertising stale hosts.
    $configPath = Join-Path $nodeRoot 'sparrow.conf'
    $runtimePath = Join-Path $nodeRoot '.env.runtime'
    if (-not (Test-Path -LiteralPath $runtimePath -PathType Leaf)) { return }
    $config = Read-Properties $configPath
    $runtime = Read-Properties $runtimePath
    if (-not $runtime['CLIENT_ENDPOINT']) { throw 'Installed node is missing CLIENT_ENDPOINT; cannot advertise a reachable Control Plane.' }
    $gateway = [Uri]$runtime['CLIENT_ENDPOINT']
    $mode = [string]$config['MODE']
    if ($mode -notin @('lan','public')) { throw 'Installed node has an invalid network mode.' }

    $advertised = [System.Collections.Generic.List[string]]::new()
    $inside = [System.Collections.Generic.List[string]]::new()
    $probeUrls = [System.Collections.Generic.List[string]]::new()
    $addCandidate = {
        param([string]$InputUrl)
        $candidate = $InputUrl.Trim().TrimEnd('/')
        if (-not $candidate) { return }
        $parsed = $null
        if (-not [Uri]::TryCreate($candidate, [UriKind]::Absolute, [ref]$parsed) -or
            $parsed.Scheme -notin @('http','https') -or
            ($mode -eq 'public' -and $parsed.Scheme -ne 'https') -or
            -not $parsed.Host -or $parsed.UserInfo -or $parsed.Fragment -or $parsed.Query) {
            throw "Invalid Control Plane address in directory: $candidate"
        }
        $forContainers = $candidate
        $forClients = $candidate
        if ($parsed.Host -in @('localhost','127.0.0.1','::1')) {
            if ($mode -eq 'public') {
                throw 'Public Control Plane discovery cannot advertise localhost.'
            }
            # This is the critical Combined/LAN fix: localhost in sparrow.conf
            # is fine for host-side bootstrap, but is WRONG for Android clients.
            # Reuse the actual LAN IP already advertised by the running gateway.
            $forClients = "http://$($gateway.Host):$($parsed.Port)"
            $forContainers = "http://host.docker.internal:$($parsed.Port)"
        } elseif ($mode -eq 'public' -and $config['SHARED_PROXY'] -eq 'true' -and
                  $parsed.Host -eq $(if ($Component -eq 'Combined') { $ControlPlaneDomain } else { $config['LOCAL_CONTROL_PLANE_DOMAIN'] })) {
            # Keep the existing private edge-network route for Combined Public.
            # Public DNS / NAT loopback may not work from backend containers.
            $forContainers = 'http://sparrow-control-edge:8080'
        }
        if (-not $advertised.Contains($forClients)) { $advertised.Add($forClients) }
        if (-not $inside.Contains($forContainers)) { $inside.Add($forContainers) }
        if (-not $probeUrls.Contains($candidate)) { $probeUrls.Add($candidate) }
    }
    # A Combined install MUST include its own Control Plane even if an older
    # directory file or prior LAN configuration advertises only retired peers.
    # Put the owned Control Plane first and retain other configured candidates.
    if ($Component -eq 'Combined') {
        if ($mode -eq 'public') { & $addCandidate "https://$ControlPlaneDomain" }
        else { & $addCandidate 'http://localhost:8390' }
    }
    foreach ($item in ([string]$config['CONTROL_PLANE_URLS'] -split '[,;]')) {
        & $addCandidate $item
    }
    # Preserve only the explicit local/manual routes as static endpoints.
    # The signed worker is authoritative for dynamic additions/removals.
    $staticInside = @($inside)
    if ($RequestedDirectoryUrl) {
        $controlPlaneDirectorySettings = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
        $pin = if ($config['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']) {
            [string]$config['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']
        } else {
            [string]$controlPlaneDirectorySettings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY']
        }
        if (-not $pin) {
            Write-Host 'No pinned directory public key; using local/manual Control Plane addresses only.'
        } else {
            try {
                $remoteUrls = @(Get-SparrowVerifiedDirectoryUrls `
                    -DirectoryUrl $RequestedDirectoryUrl `
                    -PinnedPublicKey $pin `
                    -CacheFile (Join-Path $nodeRoot '.control-plane-directory-verified.json') `
                    -ClientScript (Join-Path $root 'control_plane_directory_client.py'))
                foreach ($remote in $remoteUrls) { & $addCandidate $remote }
            } catch {
                Write-Host "Verified directory unavailable; using local/manual addresses: $($_.Exception.Message)"
            }
        }
    }
    if ($staticInside.Count -eq 0) { throw 'No configured local Control Plane. Keep the existing node configuration.' }
    $selected = $staticInside[0]
    for ($index = 0; $index -lt $probeUrls.Count; $index++) {
        try {
            $null = Invoke-WebRequest -Uri "$($probeUrls[$index])/v1/nodes" -UseBasicParsing -TimeoutSec 4 -ErrorAction Stop
            # Remote verified endpoints are never frozen as the local static
            # CONTROL_PLANE_URL: they may later be revoked by the directory.
            if ($inside[$index] -in $staticInside) { $selected = $inside[$index] }
            break
        } catch { }
    }
    # Only these three public/discovery values change; NEVER run bootstrap or
    # regenerate node.identity, database passwords, secrets or named volumes.
    $updates = @{
        CONTROL_PLANE_URL = $selected
        CONTROL_PLANE_URLS = ($staticInside -join ',')
        ADVERTISED_CONTROL_PLANE_URLS = ($advertised -join ',')
        MANUAL_CONTROL_PLANE_URLS = [string]$config['CONTROL_PLANE_URLS']
        LOCAL_CONTROL_PLANE_DOMAIN = [string]$config['LOCAL_CONTROL_PLANE_DOMAIN']
    }
    # Never resurrect opaque unsigned runtime addresses on failure. The
    # separately authenticated snapshot cache is the only remote fallback.
    $changed = ([string]$runtime['CONTROL_PLANE_URL'] -ne $updates.CONTROL_PLANE_URL -or
        [string]$runtime['CONTROL_PLANE_URLS'] -ne $updates.CONTROL_PLANE_URLS -or
        [string]$runtime['ADVERTISED_CONTROL_PLANE_URLS'] -ne $updates.ADVERTISED_CONTROL_PLANE_URLS -or
        [string]$config['CONTROL_PLANE_DIRECTORY_URL'] -ne $RequestedDirectoryUrl -or
        ($Component -eq 'Combined' -and $mode -eq 'public' -and
         [string]$config['LOCAL_CONTROL_PLANE_DOMAIN'] -ne $ControlPlaneDomain))
    if (-not $changed) { return }
    Write-Host "Updating installed node discovery: $($updates.ADVERTISED_CONTROL_PLANE_URLS)"
    # Validate the effective Compose configuration before touching any files.
    $argsBefore = Get-ComposeArguments $nodeRoot
    Invoke-Docker -Arguments ($argsBefore + @('config','--quiet'))
    $configUpdates = @{ CONTROL_PLANE_DIRECTORY_URL = $RequestedDirectoryUrl }
    if ($Component -eq 'Combined' -and $mode -eq 'public') {
        $configUpdates['LOCAL_CONTROL_PLANE_DOMAIN'] = $ControlPlaneDomain
    }
    Write-Properties $configPath $configUpdates
    Write-Properties $runtimePath $updates
    $argsAfter = Get-ComposeArguments $nodeRoot
    Invoke-Docker -Arguments ($argsAfter + @('config','--quiet'))
    # The gateway only reads ADVERTISED_CONTROL_PLANE_URLS at startup.
    # --pull never avoids blocking this configuration-only correction on GHCR.
    Invoke-Docker -Arguments ($argsAfter + @('up','-d','--no-deps','--pull','never','mailbox','federation','gateway'))
    Write-Host 'Node discovery refreshed without deleting data or changing server identity.'
}

# This is a post-install acceptance gate, not a destructive repair. A running
# Caddy container is not proof that Android can discover a usable node.
function Assert-CombinedNodeDiscoverable {
    $cpRuntime = Read-Properties (Join-Path $cpRoot '.env.runtime')
    $nodeRuntime = Read-Properties (Join-Path $nodeRoot '.env.runtime')
    $advertisedGateway = [string]$nodeRuntime['CLIENT_ENDPOINT']
    $advertisedMailbox = [string]$nodeRuntime['MAILBOX_ENDPOINT']
    if (-not $advertisedGateway -or -not $advertisedMailbox) {
        throw 'Combined installation has no advertised gateway/mailbox endpoint. Preserve the installed server and inspect node runtime configuration.'
    }
    $nodeGateway = [Uri]$advertisedGateway
    $nodeMailbox = [Uri]$advertisedMailbox
    if ($shared -and ($nodeGateway.Scheme -ne 'wss' -or $nodeMailbox.Scheme -ne 'https' -or
                     $nodeGateway.Host -ne $NodeDomain -or $nodeMailbox.Host -ne $NodeDomain)) {
        throw 'Combined Public node advertises an incorrect gateway or mailbox hostname; the existing installation was preserved.'
    }

    # The installed component Caddy ports are loopback-only in Public mode.
    # Query the LOCAL route to avoid false failures due to FRITZ!Box NAT hairpin;
    # external TLS/WSS must be tested separately from outside the LAN.
    $directoryUrl = 'http://127.0.0.1:8390/v1/nodes'
    $gatewayHealth = 'http://127.0.0.1:8490/health/gateway'
    $lastFailure = 'No registered healthy node found.'
    $restartTried = $false
    for ($attempt = 1; $attempt -le 24; $attempt++) {
        try {
            $signed = Invoke-RestMethod -Uri $directoryUrl -Method Get -TimeoutSec 5 -ErrorAction Stop
            if (-not $signed -or -not $signed.directory -or -not $signed.signature -or
                -not $signed.authorityNodeId) {
                throw 'Control Plane did not return a signed node directory.'
            }
            $matching = @($signed.directory.nodes | Where-Object {
                $_.clientEndpoint -eq $advertisedGateway -and
                $_.mailboxEndpoint -eq $advertisedMailbox -and
                $_.nodeId -and $_.signature
            })
            if ($matching.Count -ne 1) {
                throw "Signed directory has $(@($signed.directory.nodes).Count) healthy node(s), but not exactly one matching this installed Node's advertised gateway and mailbox."
            }
            $null = Invoke-WebRequest -Uri $gatewayHealth -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            Write-Host "Combined discovery READY: node $($matching[0].nodeId) advertises $advertisedGateway and $advertisedMailbox."
            return
        } catch {
            $lastFailure = $_.Exception.Message
            Write-Host "Combined node registration pending ($attempt/24): $lastFailure"
        }
        # Registration runs automatically in the Federation service, and may
        # be waiting for registry readiness after the first boot. Give it time.
        # If it remains absent, refresh only the application service once,
        # never databases, Caddy, signing keys, identities, or Docker volumes.
        if ($attempt -eq 12 -and -not $restartTried) {
            $restartTried = $true
            Write-Host 'Refreshing federation registration agent without replacing persistent data...'
            $composeArgs = Get-ComposeArguments $nodeRoot
            Invoke-Docker -Arguments ($composeArgs + @('up','-d','--no-deps','--force-recreate','--pull','never','federation'))
        }
        if ($attempt -lt 24) { Start-Sleep -Seconds 3 }
    }
    # An operator can retry Install/Start WITHOUT running the fresh-reinstall
    # flow. Do not report success and leave Android with an empty directory.
    throw "Combined installation NOT READY: $lastFailure. The existing server and identities were preserved. Inspect sparrow-community-node-federation-1 logs for registration errors and sparrow-control-plane-node-registry-1 logs; use Install / Start to retry (NOT Reinstall)."
}

function Assert-NotOrphaned([string]$Directory, [string]$Project) {
    if (Test-Path -LiteralPath (Join-Path $Directory '.env.runtime') -PathType Leaf) { return }
    # A different extracted ZIP may share Compose names with a previous project.
    # In particular, Docker volumes do not imply that this folder owns the
    # credentials/identities needed to continue that project.
    $containers = @(& docker ps --all --filter "label=com.docker.compose.project=$Project" --format '{{.ID}}' 2>$null)
    if ($LASTEXITCODE -ne 0) { throw "Could not inspect $Project containers." }
    $volumes = @(& docker volume ls --quiet --filter "name=^${Project}_" 2>$null)
    if ($LASTEXITCODE -ne 0) { throw "Could not inspect $Project volumes." }
    $volumes = @($volumes | Where-Object { $_ -like "${Project}_*" })
    if ($containers.Count -ne 0 -or $volumes.Count -ne 0) {
        throw "[SPARROW_ORPHANED_TEST_DATA] $Project has $($containers.Count) container(s) and $($volumes.Count) Docker volume(s), but $Directory has no .env.runtime. Install / Start will replace its abandoned TEST resources and generate fresh credentials; an intact installation in its original folder is protected."
    }
}
function Resolve-OrphanedTestData {
    # Install / Start is also the fresh-install action. If a selected component
    # has no local runtime but still has old Docker project resources, replace
    # ONLY that orphaned component's TEST Docker resources automatically.
    # A real installed component (with .env.runtime) is an image update: never
    # remove its existing volumes, containers, secrets or server identity.
    $orphanNode = $false
    $orphanControlPlane = $false
    if ($nodeSelected) {
        try { Assert-NotOrphaned $nodeRoot 'sparrow-community-node' }
        catch {
            if ($_.Exception.Message.StartsWith('[SPARROW_ORPHANED_TEST_DATA]', [StringComparison]::Ordinal)) {
                $orphanNode = $true
            } else { throw }
        }
    }
    if ($cpSelected) {
        try { Assert-NotOrphaned $cpRoot 'sparrow-control-plane' }
        catch {
            if ($_.Exception.Message.StartsWith('[SPARROW_ORPHANED_TEST_DATA]', [StringComparison]::Ordinal)) {
                $orphanControlPlane = $true
            } else { throw }
        }
    }
    if (-not $orphanNode -and -not $orphanControlPlane) { return }
    if (Test-Path -LiteralPath $attachmentFile -PathType Leaf) {
        throw 'An attached/migrated installation may not be automatically reset.'
    }
    # Do not reset an installed component merely because the OTHER component
    # is orphaned. Proxy resources are reset only for a wholly orphaned Combined
    # deployment, never while one component still owns a valid installation.
    $resetSelection = if ($orphanNode -and $orphanControlPlane) { 'Combined' } elseif ($orphanNode) { 'Node' } else { 'ControlPlane' }
    Write-Host "Fresh-install cleanup: $resetSelection has old Docker resources but no local runtime. Replacing its old containers, volumes and generated secrets with a new installation."
    & (Join-Path $root 'Reset-SparrowTestDeployment.ps1') -InstallationDirectory $root -Component $resetSelection -DeleteDisposableData -Confirmation 'DELETE SPARROW TEST DATA'
    if (-not $?) { throw 'Could not replace orphaned test Docker resources. The installation was not started.' }
    if ($orphanNode) { Assert-NotOrphaned $nodeRoot 'sparrow-community-node' }
    if ($orphanControlPlane) { Assert-NotOrphaned $cpRoot 'sparrow-control-plane' }
    # An incomplete earlier install may have written an ID before bootstrapping
    # .env.runtime. A fresh CP must not reuse that abandoned identifier.
    if ($orphanControlPlane) {
        $cpConfigPath = Join-Path $cpRoot 'sparrow.conf'
        if (Test-Path -LiteralPath $cpConfigPath -PathType Leaf) {
            Write-Properties $cpConfigPath @{ CONTROL_PLANE_ID = '' }
        }
    }
}

function Get-PushCredential {
    $cpConfigPath = Join-Path $cpRoot 'sparrow.conf'
    $cpConfig = Read-Properties $cpConfigPath
    $credentialOwner = [string]$cpConfig['CONTROL_PLANE_ID']
    if ($credentialOwner -notmatch '^[a-zA-Z0-9-]{1,64}$') {
        # A previously interrupted installation can leave CONTROL_PLANE_ID
        # empty even though .env.runtime and the live Control Plane exist.
        # That value is metadata, not a reason to reinstall or regenerate
        # the registry signing identity. Store Firebase credentials under a
        # separately persisted, opaque owner ID instead.
        $credentialOwner = [string]$cpConfig['FIREBASE_CREDENTIAL_STORE_ID']
        if ($credentialOwner -notmatch '^[a-zA-Z0-9-]{1,64}$') {
            $credentialOwner = [Guid]::NewGuid().ToString('N')
            Write-Properties $cpConfigPath @{ FIREBASE_CREDENTIAL_STORE_ID = $credentialOwner }
        }
    }
    $secretRoot = Join-Path $env:LOCALAPPDATA "Sparrow\Server\Secrets\$credentialOwner"
    $destination = Join-Path $secretRoot 'firebase-admin.json'
    if ([string]::IsNullOrWhiteSpace($FirebaseCredentialsPath)) {
        if (Test-Path -LiteralPath $destination -PathType Leaf) { return $destination }
        return
    }
    if (-not (Test-Path -LiteralPath $FirebaseCredentialsPath -PathType Leaf)) {
        Write-Host 'Firebase credentials not found; continuing without FCM.'; return
    }
    try {
        $json = Get-Content -LiteralPath $FirebaseCredentialsPath -Raw | ConvertFrom-Json -ErrorAction Stop
        if ($json.type -ne 'service_account' -or -not $json.client_email -or -not $json.private_key) {
            throw 'Expected a Google service-account JSON.'
        }
    } catch {
        Write-Host 'Firebase credentials could not be parsed; continuing without FCM.'; return
    }
    # This location is under the operator profile, NOT included in distributable archives.
    New-Item -ItemType Directory -Path $secretRoot -Force | Out-Null
    if ([System.IO.Path]::GetFullPath($FirebaseCredentialsPath) -ne [System.IO.Path]::GetFullPath($destination)) {
        if (Test-Path -LiteralPath $destination -PathType Leaf) {
            $backup = "$destination.backup.$(Get-Date -Format yyyyMMddHHmmss)"
            Copy-Item -LiteralPath $destination -Destination $backup -ErrorAction Stop
        }
        Copy-Item -LiteralPath $FirebaseCredentialsPath -Destination $destination -Force
    }
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $destination /inheritance:r /grant:r "*${sid}:(F)" '*S-1-5-18:(F)' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not protect imported Firebase credentials with a restricted ACL.' }
    return $destination
}
function Ensure-SharedNetwork {
    # Inspect of a *missing* network writes to stderr. With Windows PowerShell
    # 5.1 and ErrorActionPreference=Stop, 2>$null can still turn that routine
    # result into a terminating NativeCommandError before the create branch.
    # Listing names is non-destructive and does not emit a not-found error.
    $previousPreference = $ErrorActionPreference
    $networkNames = @()
    $exitCode = -1
    try {
        $ErrorActionPreference = 'Continue'
        $networkNames = @(& docker network ls --format '{{.Name}}' 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        throw "Could not list Docker networks (exit $exitCode): $($networkNames -join '; ')"
    }
    if ('sparrow-public-edge' -notin @($networkNames | ForEach-Object { [string]$_ })) {
        Write-Host 'Shared public Docker network is absent; creating sparrow-public-edge.'
        Invoke-Docker -Arguments @('network', 'create', '--driver', 'bridge', 'sparrow-public-edge')
    }
}
function Get-ComponentRoutes([string]$Directory, [string]$Marker, [hashtable]$Aliases, [string]$IndexRoot) {
    $source = Get-Content -LiteralPath (Join-Path $Directory 'Caddyfile') -Raw
    $end = $source.IndexOf($Marker, [StringComparison]::Ordinal)
    if ($end -lt 0) { throw "Could not identify existing Caddy route definition in $Directory." }
    $routes = $source.Substring(0, $end)
    foreach ($old in $Aliases.Keys) {
        if (-not $routes.Contains($old)) { throw "Unexpected Caddy upstream $old in $Directory. Shared route generation was aborted." }
        $routes = $routes.Replace($old, $Aliases[$old])
    }
    $routes = $routes.Replace('root * /srv', "root * $IndexRoot")
    return $routes
}
function Write-ProxyConfig {
    # Editing or updating one component must not drop the other component's
    # already-configured public route from the shared entry point.
    $nodeConfig = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
    $cpConfig = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
    $publishNode = $nodeSelected -or ($nodeConfig['MODE'] -eq 'public' -and
        $nodeConfig['SHARED_PROXY'] -eq 'true' -and
        (Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf))
    $publishCP = $cpSelected -or ($cpConfig['MODE'] -eq 'public' -and
        $cpConfig['SHARED_PROXY'] -eq 'true' -and
        (Test-Path -LiteralPath (Join-Path $cpRoot '.env.runtime') -PathType Leaf))
    $nodeHost = if ($nodeSelected) { $NodeDomain } else { $nodeConfig['PUBLIC_DOMAIN'] }
    $cpHost = if ($cpSelected) { $ControlPlaneDomain } else { $cpConfig['PUBLIC_DOMAIN'] }
    if ($publishNode -and $publishCP -and $nodeHost -eq $cpHost) { throw 'Node and Control Plane cannot use the same public hostname.' }
    $entries = @()
    $routes = @()
    if ($publishNode) {
        $routes += Get-ComponentRoutes $nodeRoot '{$COMMUNITY_NODE_SITE_ADDRESS}' @{
            'gateway:8094' = 'sparrow-node-gateway:8094'
            'federation:8093' = 'sparrow-node-federation:8093'
            'mailbox:8092' = 'sparrow-node-mailbox:8092'
        } '/srv/community-node'
        $entries += "$(Check-Domain $nodeHost) {`n    import sparrow_community_routes`n}"
    }
    if ($publishCP) {
        $routes += Get-ComponentRoutes $cpRoot '{$CONTROL_PLANE_SITE_ADDRESS}' @{
            'node-registry:8090' = 'sparrow-control-registry:8090'
            'presence-directory:8091' = 'sparrow-control-presence:8091'
            'push:8095' = 'sparrow-control-push:8095'
        } '/srv/control-plane'
        $entries += "$(Check-Domain $cpHost) {`n    import sparrow_control_routes`n}"
    }
    if ($routes.Count -eq 0) { throw 'Cannot start public proxy without a hostname.' }
    $managedRoutes = Join-Path $root 'node-instances/routes'
    if ((Test-Path -LiteralPath $managedRoutes -PathType Container) -and @(Get-ChildItem -LiteralPath $managedRoutes -Filter '*.caddy' -File).Count -gt 0) {
        $entries += 'import /etc/caddy/node-routes/*.caddy'
    }
    $caddyfile = Join-Path $proxyRoot 'Caddyfile'
    $temp = "$caddyfile.pending"
    [System.IO.File]::WriteAllText($temp, (($routes + $entries) -join "`n"), [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temp -Destination $caddyfile -Force
}
function Check-PublicPortOwnership {
    # Do not automatically stop/rebind an existing separate public installation: its TLS data
    # and connection state require an explicit, backed-up cutover.
    $ports = & docker ps --format '{{.Names}} {{.Ports}}' 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Could not check currently published Docker ports.' }
    $ownProxyRunning = $false
    foreach ($line in $ports) {
        if ($line -match '^sparrow-public-proxy-' -and $line -match '(?:80|443)->') { $ownProxyRunning = $true }
        if ($line -match '(?:0\.0\.0\.0|\[::\]):(?:80|443)->' -and $line -notmatch '^sparrow-public-proxy-') {
            throw "A different container already binds public TCP 80/443: $line. Shared proxy migration requires an explicit protected cutover; no existing deployment was changed."
        }
    }
    if (-not $ownProxyRunning) {
        $listeners = @(Get-NetTCPConnection -LocalPort 80, 443 -State Listen -ErrorAction SilentlyContinue)
        if ($listeners.Count -gt 0) {
            throw 'Host TCP 80/443 is already occupied by another application. The existing listener was not interrupted.'
        }
    }
}


# A previous interrupted Public installation can leave sparrow.conf marked LAN
# even though the installed containers already use BOTH Public Compose layers.
# Reconcile host-side metadata only when the COMPLETE existing deployment can
# be proved Public. Never bootstrap, migrate, reset identities or delete volumes.
function Test-InstalledProjectHasCompletePublicLayers([string]$Project, [string[]]$Services) {
    foreach ($service in $Services) {
        $ids = @(& docker ps -a --filter "label=com.docker.compose.project=$Project" --filter "label=com.docker.compose.service=$service" --format '{{.ID}}' 2>$null)
        if ($LASTEXITCODE -ne 0 -or $ids.Count -ne 1 -or -not $ids[0]) {
            throw "Cannot verify the existing $Project/$service Public deployment. No configuration was changed."
        }
        $files = [string](Get-SparrowDockerLabel -Resource container -Id ([string]$ids[0]) -Key 'com.docker.compose.project.config_files')
        $names = @($files -split ',' | ForEach-Object { [System.IO.Path]::GetFileName(([string]$_).Trim()) })
        if (@('docker-compose.yml', 'docker-compose.release.yml', 'docker-compose.production.yml', 'docker-compose.shared-proxy.yml') |
            Where-Object { $_ -notin $names }) {
            throw "Existing $Project/$service does not have the complete Public Compose configuration. No configuration was changed."
        }
        if ('docker-compose.public-transition.yml' -in $names) {
            throw "Existing $Project/$service uses transition layers, not a completed fresh Public installation. No configuration was changed."
        }
    }
}
function Repair-InstalledPublicMetadataIfAlreadyPublic {
    if (-not $shared -or $Component -ne 'Combined' -or $ExistingDeployment -eq 'ReplaceTestData') { return }
    $cpConfPath = Join-Path $cpRoot 'sparrow.conf'
    $nodeConfPath = Join-Path $nodeRoot 'sparrow.conf'
    $cpRuntimePath = Join-Path $cpRoot '.env.runtime'
    $nodeRuntimePath = Join-Path $nodeRoot '.env.runtime'
    if (-not (Test-Path -LiteralPath $cpRuntimePath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $nodeRuntimePath -PathType Leaf)) { return }
    $cpConf = Read-Properties $cpConfPath
    $nodeConf = Read-Properties $nodeConfPath
    if ($cpConf['MODE'] -eq 'public' -and $nodeConf['MODE'] -eq 'public') { return }
    if ($cpConf['MODE'] -ne 'lan' -and $cpConf['MODE'] -ne 'public') { return }
    if ($nodeConf['MODE'] -ne 'lan' -and $nodeConf['MODE'] -ne 'public') { return }

    # Inspect before writing either configuration. If this is truly LAN or only
    # partially converted, leave it untouched: a fake Public metadata flag can
    # change Compose DB credential and signing-volume bindings.
    Assert-DeploymentsNotRunningElsewhere
    Test-InstalledProjectHasCompletePublicLayers 'sparrow-control-plane' @('caddy', 'node-registry', 'presence-directory', 'push')
    Test-InstalledProjectHasCompletePublicLayers 'sparrow-community-node' @('caddy', 'mailbox', 'federation', 'gateway')
    $proxyIds = @(& docker ps -a --filter 'label=com.docker.compose.project=sparrow-public-proxy' --filter 'label=com.docker.compose.service=caddy' --format '{{.ID}}' 2>$null)
    if ($LASTEXITCODE -ne 0 -or $proxyIds.Count -ne 1 -or -not $proxyIds[0]) {
        throw 'Cannot verify the installed shared Public Caddy proxy. No configuration was changed.'
    }
    $cpRuntime = Read-Properties $cpRuntimePath
    $nodeRuntime = Read-Properties $nodeRuntimePath
    $cpHost = Check-Domain ([string]$cpRuntime['CONTROL_PLANE_DOMAIN'])
    $nodeHost = Check-Domain ([string]$nodeRuntime['COMMUNITY_NODE_DOMAIN'])
    if ($cpHost -ne $ControlPlaneDomain -or $nodeHost -ne $NodeDomain -or $cpHost -eq $nodeHost) {
        throw "The existing Public hostnames ($cpHost, $nodeHost) differ from the selected hostnames. No configuration was changed."
    }
    foreach ($entry in @(@{ Config=$cpConf; Host=$cpHost; Role='Control Plane' },
                          @{ Config=$nodeConf; Host=$nodeHost; Role='Community Node' })) {
        if ($entry.Config['PUBLIC_DOMAIN'] -and $entry.Config['PUBLIC_DOMAIN'] -ne $entry.Host) {
            throw "Existing $($entry.Role) hostname metadata does not match its running Public deployment. No configuration was changed."
        }
        if ($entry.Config['PUBLIC_TRANSITION']) {
            throw "Existing $($entry.Role) contains an unfinished transition marker. No configuration was changed."
        }
    }
    if ($nodeRuntime['CLIENT_ENDPOINT'] -ne "wss://$nodeHost/v1/gateway" -or
        $nodeRuntime['MAILBOX_ENDPOINT'] -ne "https://$nodeHost") {
        throw 'Existing Community Node does not advertise the configured Public gateway/mailbox endpoints. No configuration was changed.'
    }
    $caddyFile = Join-Path $proxyRoot 'Caddyfile'
    if (-not (Test-Path -LiteralPath $caddyFile -PathType Leaf)) {
        throw 'Existing shared Public Caddy configuration is missing. No configuration was changed.'
    }
    $caddyText = [System.IO.File]::ReadAllText($caddyFile)
    if (-not $caddyText.Contains($cpHost) -or -not $caddyText.Contains($nodeHost)) {
        throw 'The shared Public Caddy configuration does not contain both installed hostnames. No configuration was changed.'
    }
    # Only repair the two inconsistent human-readable configuration files.
    # Write-Properties creates a timestamped backup. Runtime secrets, the
    # Compose files, Docker containers and persistent volumes are untouched.
    if ($cpConf['MODE'] -ne 'public' -or $cpConf['SHARED_PROXY'] -ne 'true' -or -not $cpConf['PUBLIC_DOMAIN']) {
        Write-Properties $cpConfPath @{ MODE='public'; PUBLIC_DOMAIN=$cpHost; SHARED_PROXY='true' }
    }
    if ($nodeConf['MODE'] -ne 'public' -or $nodeConf['SHARED_PROXY'] -ne 'true' -or -not $nodeConf['PUBLIC_DOMAIN']) {
        Write-Properties $nodeConfPath @{ MODE='public'; PUBLIC_DOMAIN=$nodeHost; SHARED_PROXY='true' }
    }
    Write-Host 'Existing Public Compose deployment verified. Corrected stale LAN metadata only; proceeding with normal Install / Start (no migration or reinstall).'
}

# An existing LAN installation is published through the shared Caddy without
# re-running either bootstrap, changing DB secrets or replacing server identities.
# Public-only overrides keep the ORIGINAL Compose layering and named volumes.
function Get-InstalledLanComposeFlavor([string]$Directory, [string]$Project) {
    $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$Project" --filter 'label=com.docker.compose.service=caddy')
    if ($LASTEXITCODE -ne 0 -or $ids.Count -ne 1) {
        throw "Cannot identify exactly one existing Caddy container for $Project. Preserve the LAN installation."
    }
    $configuration = Get-SparrowDockerLabel -Resource container -Id ([string]$ids[0]) -Key 'com.docker.compose.project.config_files'
    if ($configuration -notmatch 'docker-compose\.yml') {
        throw "Cannot prove the original Compose configuration for $Project. Refusing an identity-changing migration."
    }
    if ($configuration -match 'docker-compose\.production\.yml|docker-compose\.shared-proxy\.yml|docker-compose\.public-transition\.yml') {
        throw "$Project already uses public Compose overrides although sparrow.conf says LAN. Inspect the original deployment before migration."
    }
    foreach ($filename in ($configuration -split ',' | ForEach-Object { [System.IO.Path]::GetFileName(([string]$_).Trim()) })) {
        if ($filename -notin @('docker-compose.yml','docker-compose.release.yml','docker-compose.firebase.yml')) {
            throw "Unrecognized live Compose override $filename for $Project. Preserve original service/identity configuration."
        }
        if ($filename -eq 'docker-compose.firebase.yml' -and $Project -ne 'sparrow-control-plane') {
            throw "Unexpected Firebase override on $Project."
        }
    }
    return $(if ($configuration -match 'docker-compose\.release\.yml') { 'release' } else { 'base' })
}
# A previous failed public Install / Start may have written MODE=public but
# never supplied PUBLIC_DOMAIN or applied public Compose files. A missing
# hostname is a CONFIGURATION state, not evidence of an unsupported Docker
# network. Infer LAN only if *every live container* still uses the original
# LAN Compose layers and no prior migration marker or active public proxy
# exists. Anything ambiguous remains untouched and reports the actual state.
function Get-SafeInstalledPublicMode([string]$Directory, [string]$Project) {
    $config = Read-Properties (Join-Path $Directory 'sparrow.conf')
    $mode = ([string]$config['MODE']).Trim().ToLowerInvariant()
    $hostname = ([string]$config['PUBLIC_DOMAIN']).Trim()
    if ($mode -eq 'lan') { return 'lan' }
    if ($mode -eq 'public' -and $hostname) { return 'public' }
    $safeMode = $(if ($mode) { $mode } else { '(missing)' })
    if ($mode -notin @('', 'public') -or $hostname -or $config['PUBLIC_TRANSITION']) {
        throw "Existing $Project has MODE=$safeMode, PUBLIC_DOMAIN=$hostname, PUBLIC_TRANSITION=$($config['PUBLIC_TRANSITION']). Cannot infer LAN from that state. No Docker data was changed."
    }
    if ($Component -ne 'Combined' -or -not $nodeSelected -or -not $cpSelected) {
        throw "Existing $Project has incomplete MODE=$safeMode, PUBLIC_DOMAIN=(empty). Select Combined to inspect its existing LAN deployment before Public conversion. No state was changed."
    }
    # This checks the Caddy instance's actual Compose file labels, but also
    # check all other containers: a crashed migration may have left Caddy on
    # LAN and already converted backend containers. Never infer LAN then.
    $null = Get-InstalledLanComposeFlavor $Directory $Project
    $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$Project")
    if ($LASTEXITCODE -ne 0 -or $ids.Count -eq 0) {
        throw "Cannot inspect the existing $Project containers. No state was changed."
    }
    foreach ($id in $ids) {
        $files = [string](Get-SparrowDockerLabel -Resource container -Id ([string]$id) -Key 'com.docker.compose.project.config_files')
        if (-not $files -or $files -match 'docker-compose\.(production|shared-proxy|public-transition)\.yml') {
            throw "Existing $Project contains public or unknown Compose layers (container $id). Refusing automatic recovery; no state was changed."
        }
        foreach ($filename in ($files -split ',' | ForEach-Object { [System.IO.Path]::GetFileName(([string]$_).Trim()) })) {
            if ($filename -notin @('docker-compose.yml','docker-compose.release.yml','docker-compose.firebase.yml')) {
                throw "Existing $Project has unknown live Compose file $filename. Refusing automatic recovery; no state was changed."
            }
        }
    }
    $proxy = @(& docker ps --all --filter 'label=com.docker.compose.project=sparrow-public-proxy' --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0 -or $proxy.Count -gt 0) {
        throw "Existing $Project has incomplete Public configuration and existing shared-proxy containers (or Docker inspection failed). Do not overwrite a previous deployment; no state was changed."
    }
    Write-Host "Existing $Project has incomplete MODE=$safeMode / missing PUBLIC_DOMAIN, but all running Docker Compose layers are LAN. Eligible for ownership-checked, non-destructive Combined conversion."
    return 'lan'
}
function Get-TransitionComposeArguments([string]$Directory, [string]$Runtime, [string]$Flavor) {
    $result = @('compose', '--env-file', $Runtime, '-f', (Join-Path $Directory 'docker-compose.yml'))
    if ($Flavor -eq 'release') { $result += @('-f', (Join-Path $Directory 'docker-compose.release.yml')) }
    $result += @('-f', (Join-Path $Directory 'docker-compose.public-transition.yml'))
    if ($Directory -eq $cpRoot) {
        $existing = Read-Properties (Join-Path $cpRoot '.env.runtime')
        if ($existing['FIREBASE_ADMIN_CREDENTIALS'] -and
            (Test-Path -LiteralPath $existing['FIREBASE_ADMIN_CREDENTIALS'] -PathType Leaf)) {
            $result += @('-f', (Join-Path $cpRoot 'docker-compose.firebase.yml'))
        }
    }
    return ,$result
}
function Write-PreflightProperties([string]$Path, [hashtable]$Updates) {
    # Staged *copies* only: avoid writing permanent backup files containing
    # copied runtime credentials next to these transient Compose preflight files.
    $values = Read-Properties $Path
    foreach ($key in $Updates.Keys) { $values[$key] = [string]$Updates[$key] }
    $lines = @('# Temporary Sparrow migration preflight; not an installed runtime.')
    foreach ($key in ($values.Keys | Sort-Object)) { $lines += "$key=$($values[$key])" }
    [System.IO.File]::WriteAllLines($Path, $lines, [System.Text.UTF8Encoding]::new($false))
}
function Test-LocalLanDeploymentForMigration {
    if ($Component -ne 'Combined' -or $Mode -ne 'public') {
        throw 'In-place LAN to Public conversion currently requires Combined mode (Node + Control Plane).'
    }
    Assert-DeploymentsNotRunningElsewhere
    foreach ($entry in @(@{ Root=$cpRoot; Project='sparrow-control-plane'; Services=@('caddy','node-registry','presence-directory','push','node-registry-database','presence-redis','push-database') },
                        @{ Root=$nodeRoot; Project='sparrow-community-node'; Services=@('caddy','mailbox','federation','gateway','mailbox-database','federation-database') })) {
        $runtimePath = Join-Path $entry.Root '.env.runtime'
        $config = Read-Properties (Join-Path $entry.Root 'sparrow.conf')
        if (-not (Test-Path -LiteralPath $runtimePath -PathType Leaf) -or
            (Get-SafeInstalledPublicMode $entry.Root $entry.Project) -ne 'lan') {
            throw "Only two complete, installed LAN components can be converted. $($entry.Project) does not qualify."
        }
        if (-not (Test-Path -LiteralPath (Join-Path $entry.Root 'secrets') -PathType Container)) {
            throw "Missing existing secrets folder for $($entry.Project). No migration changes made."
        }
        $all = @(& docker ps -aq --filter "label=com.docker.compose.project=$($entry.Project)")
        if ($LASTEXITCODE -ne 0 -or $all.Count -lt $entry.Services.Count) {
            throw "Incomplete Docker deployment for $($entry.Project). No data was changed."
        }
        foreach ($service in $entry.Services) {
            $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$($entry.Project)" --filter "label=com.docker.compose.service=$service")
            if ($LASTEXITCODE -ne 0 -or $ids.Count -ne 1) {
                throw "Missing/duplicate $service container in $($entry.Project). No conversion was attempted."
            }
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $nodeRoot 'secrets/mailbox-database-password.txt') -PathType Leaf) -or
        -not (Test-Path -LiteralPath (Join-Path $nodeRoot 'secrets/federation-database-password.txt') -PathType Leaf)) {
        throw 'Existing node password files are missing. Refusing to change its Compose configuration.'
    }
    foreach ($entry in @(@{Root=$cpRoot; Name='control-plane'}, @{Root=$nodeRoot; Name='community-node'})) {
        if (-not (Test-Path -LiteralPath (Join-Path $entry.Root 'docker-compose.public-transition.yml') -PathType Leaf)) {
            throw "Missing protected Public transition override for $($entry.Name). No existing state changed."
        }
    }
    # An old public proxy from a different extracted bundle must never be
    # adopted or replaced just because its Compose project name matches.
    $proxyIds = @(& docker ps -aq --filter 'label=com.docker.compose.project=sparrow-public-proxy')
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the existing public proxy.' }
    foreach ($id in $proxyIds) {
        $workdir = Get-SparrowDockerLabel -Resource container -Id ([string]$id) -Key 'com.docker.compose.project.working_dir'
        if (-not $workdir -or -not (Normalize-ComposeWorkdir $workdir).Equals(
                (Normalize-ComposeWorkdir $proxyRoot), [StringComparison]::OrdinalIgnoreCase)) {
            throw 'A different Sparrow public proxy deployment already exists. Its TLS volumes and routes were not changed.'
        }
    }
    Check-PublicPortOwnership
}
function Invoke-LanToPublicMigration {
    Write-Host '==== Protected LAN -> Public conversion (existing server IDs and volumes retained) ===='
    Test-LocalLanDeploymentForMigration
    $cpFlavor = Get-InstalledLanComposeFlavor $cpRoot 'sparrow-control-plane'
    $nodeFlavor = Get-InstalledLanComposeFlavor $nodeRoot 'sparrow-community-node'
    if ($nodeFlavor -ne 'release') {
        throw 'The node does not use its existing release/secret-file configuration. Refusing to change DB credentials during public conversion.'
    }
    $cpRuntime = Read-Properties (Join-Path $cpRoot '.env.runtime')
    $nodeRuntime = Read-Properties (Join-Path $nodeRoot '.env.runtime')
    foreach ($key in @('CONTROL_PLANE_PROJECT_NAME','NODE_REGISTRY_DATABASE_PASSWORD','PRESENCE_REDIS_PASSWORD','PUSH_DATABASE_PASSWORD','PUSH_INTERNAL_API_TOKEN')) {
        if (-not $cpRuntime[$key]) { throw "Existing Control Plane lacks $key. No data changed." }
    }
    foreach ($key in @('COMMUNITY_NODE_PROJECT_NAME','CLIENT_ENDPOINT','FEDERATION_ENDPOINT','MAILBOX_ENDPOINT','CONTROL_PLANE_URL')) {
        if (-not $nodeRuntime[$key]) { throw "Existing Node lacks $key. No data changed." }
    }
    $candidateDirectory = ([string]$DirectoryUrl).Trim()
    if ($candidateDirectory) {
        $parsedDirectory = $null
        if (-not [uri]::TryCreate($candidateDirectory, [UriKind]::Absolute, [ref]$parsedDirectory) -or
            $parsedDirectory.Scheme -notin @('http','https') -or -not $parsedDirectory.Host -or
            $parsedDirectory.UserInfo -or $parsedDirectory.Fragment -or $parsedDirectory.Query) {
            throw 'Control Plane JSON directory must be a valid HTTPS URL in Public mode.'
        }
        if ($parsedDirectory.Scheme -ne 'https') {
            $oldNodeConfig = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
            $originalDirectory = [string]$oldNodeConfig['CONTROL_PLANE_DIRECTORY_URL']
            if ($candidateDirectory -ne $originalDirectory) {
                throw 'The manually entered Control Plane directory must use HTTPS in Public mode.'
            }
            Write-Host 'The prior LAN-only HTTP directory is preserved in the backup, but cannot be advertised in Public mode.'
            $candidateDirectory = ''
        }
    }
    $cpPublic = "https://$ControlPlaneDomain"
    $cpConfigured = @($cpPublic)
    $oldNodeConfig = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
    foreach ($url in (([string]$oldNodeConfig['CONTROL_PLANE_URLS']) -split '[,;]')) {
        $candidate = $url.Trim().TrimEnd('/')
        if ($candidate -match '^https://' -and $candidate -ne $cpPublic -and $candidate -notin $cpConfigured) {
            $cpConfigured += $candidate
        }
    }
    $cpUrls = $cpConfigured -join ','
    # Validate the prospective Compose model before writing configuration or
    # touching any running containers. This does not pull images or create data.
    Ensure-SharedNetwork
    $preflightFiles = @()
    try {
        $cpCandidate = Join-Path $cpRoot '.env.runtime.public-preflight'
        $nodeCandidate = Join-Path $nodeRoot '.env.runtime.public-preflight'
        foreach ($source in @(@{ Path=$cpCandidate; Original=(Join-Path $cpRoot '.env.runtime') },
                            @{ Path=$nodeCandidate; Original=(Join-Path $nodeRoot '.env.runtime') })) {
            if (Test-Path -LiteralPath $source.Path) { throw "Unexpected pending migration file: $($source.Path). Inspect before continuing." }
            Copy-Item -LiteralPath $source.Original -Destination $source.Path -ErrorAction Stop
            $preflightFiles += $source.Path
        }
        Write-PreflightProperties $cpCandidate @{ CONTROL_PLANE_DOMAIN=$ControlPlaneDomain }
        Write-PreflightProperties $nodeCandidate @{
            COMMUNITY_NODE_DOMAIN=$NodeDomain
            CLIENT_ENDPOINT="wss://$NodeDomain/v1/gateway"
            FEDERATION_ENDPOINT="https://$NodeDomain"
            MAILBOX_ENDPOINT="https://$NodeDomain"
            CONTROL_PLANE_URL='http://sparrow-control-edge:8080'
            CONTROL_PLANE_URLS=('http://sparrow-control-edge:8080' + $(if ($cpConfigured.Count -gt 1) { ',' + ($cpConfigured[1..($cpConfigured.Count-1)] -join ',') } else { '' }))
            ADVERTISED_CONTROL_PLANE_URLS=$cpUrls
        }
        $cpArgs = Get-TransitionComposeArguments $cpRoot $cpCandidate $cpFlavor
        $nodeArgs = Get-TransitionComposeArguments $nodeRoot $nodeCandidate $nodeFlavor
        Invoke-Docker -Arguments ($cpArgs + @('config','--quiet'))
        Invoke-Docker -Arguments ($nodeArgs + @('config','--quiet'))
        Invoke-Docker -Arguments @('compose','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'config','--quiet')
    } finally {
        foreach ($path in $preflightFiles) {
            Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        }
    }
    # Back up the FULL current configuration (including secret-bearing runtime).
    # These are recovery files, not a database snapshot. Database containers and
    # named volumes are never stopped, removed or re-created by this conversion.
    $backup = Join-Path $root ('migration-backups\lan-to-public-' + (Get-Date -Format yyyyMMdd-HHmmss) + '-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path (Join-Path $backup 'control-plane'), (Join-Path $backup 'community-node'), (Join-Path $backup 'public-proxy') -Force | Out-Null
    foreach ($entry in @(@{Root=$cpRoot; Name='control-plane'}, @{Root=$nodeRoot; Name='community-node'})) {
        foreach ($filename in @('sparrow.conf','.env.runtime')) {
            Copy-Item -LiteralPath (Join-Path $entry.Root $filename) -Destination (Join-Path (Join-Path $backup $entry.Name) $filename) -ErrorAction Stop
        }
    }
    $oldProxyFile = Join-Path $proxyRoot 'Caddyfile'
    if (Test-Path -LiteralPath $oldProxyFile -PathType Leaf) {
        Copy-Item -LiteralPath $oldProxyFile -Destination (Join-Path $backup 'public-proxy/Caddyfile') -ErrorAction Stop
    }
    Write-Host "Recovery snapshots of the original configuration were saved in: $backup"
    Write-Host 'No database volume, signing key, node identity or password is removed or regenerated.'
    # Only the host-side environment/Compose network/advertised endpoints change.
    # Complete the component transition without calling either bootstrap.
    try {
        Write-Properties (Join-Path $cpRoot 'sparrow.conf') @{
        MODE='public'; PUBLIC_DOMAIN=$ControlPlaneDomain; SHARED_PROXY='true'
        PUBLIC_TRANSITION='lan-in-place'; PUBLIC_TRANSITION_COMPOSE=$cpFlavor
    }
    Write-Properties (Join-Path $nodeRoot 'sparrow.conf') @{
        MODE='public'; PUBLIC_DOMAIN=$NodeDomain; SHARED_PROXY='true'
        PUBLIC_TRANSITION='lan-in-place'; PUBLIC_TRANSITION_COMPOSE=$nodeFlavor
        LOCAL_CONTROL_PLANE_DOMAIN=$ControlPlaneDomain
        CONTROL_PLANE_URLS=$cpUrls; CONTROL_PLANE_DIRECTORY_URL=$candidateDirectory
    }
    Write-Properties (Join-Path $cpRoot '.env.runtime') @{ CONTROL_PLANE_DOMAIN=$ControlPlaneDomain }
    Write-Properties (Join-Path $nodeRoot '.env.runtime') @{
        COMMUNITY_NODE_DOMAIN=$NodeDomain
        CLIENT_ENDPOINT="wss://$NodeDomain/v1/gateway"
        FEDERATION_ENDPOINT="https://$NodeDomain"
        MAILBOX_ENDPOINT="https://$NodeDomain"
        CONTROL_PLANE_URL='http://sparrow-control-edge:8080'
        CONTROL_PLANE_URLS=('http://sparrow-control-edge:8080' + $(if ($cpConfigured.Count -gt 1) { ',' + ($cpConfigured[1..($cpConfigured.Count-1)] -join ',') } else { '' }))
        ADVERTISED_CONTROL_PLANE_URLS=$cpUrls
    }
        Write-Host 'Applying existing Control Plane backend and Caddy networking without touching database/Redis containers ...'
        $cpArgs = Get-ComposeArguments $cpRoot
        Invoke-Docker -Arguments ($cpArgs + @('up','-d','--no-deps','--pull','never','node-registry','presence-directory','push','caddy'))
        Write-Host 'Applying existing Community Node backend and Caddy networking without touching database containers ...'
        $nodeArgs = Get-ComposeArguments $nodeRoot
        Invoke-Docker -Arguments ($nodeArgs + @('up','-d','--no-deps','--pull','never','mailbox','federation','gateway','caddy'))
        Write-ProxyConfig
        Invoke-Docker -Arguments @('compose','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'up','-d','--pull','never')
        Invoke-Docker -Arguments @('compose','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'exec','-T','caddy','caddy','validate','--config','/etc/caddy/Caddyfile')
        Write-Host 'Existing LAN deployment is now configured for shared-Caddy Public mode. External DNS, certificates and Android connectivity must still be tested.'
        Write-Host "Original configuration snapshots: $backup (not a database backup)."
    } catch {
        $migrationFailure = $_.Exception.Message
        Write-Host "Public conversion failed: $migrationFailure. Attempting non-destructive LAN configuration rollback."
        $rollbackFailures = [System.Collections.Generic.List[string]]::new()
        try {
            # Stop only the shared reverse proxy; no `down`, no `-v`, no pruning.
            # An existing LAN server never used this entry proxy for its routing.
            Invoke-Docker -Arguments @('compose','-f',(Join-Path $proxyRoot 'docker-compose.yml'),'stop','caddy')
        } catch { $rollbackFailures.Add("Public proxy stop: $($_.Exception.Message)") }
        foreach ($entry in @(@{Root=$cpRoot; Name='control-plane'}, @{Root=$nodeRoot; Name='community-node'})) {
            foreach ($filename in @('sparrow.conf','.env.runtime')) {
                try {
                    Copy-Item -LiteralPath (Join-Path (Join-Path $backup $entry.Name) $filename) -Destination (Join-Path $entry.Root $filename) -Force -ErrorAction Stop
                } catch { $rollbackFailures.Add("Restore $($entry.Name)/$filename : $($_.Exception.Message)") }
            }
        }
        if (Test-Path -LiteralPath (Join-Path $backup 'public-proxy/Caddyfile') -PathType Leaf) {
            try { Copy-Item -LiteralPath (Join-Path $backup 'public-proxy/Caddyfile') -Destination $oldProxyFile -Force -ErrorAction Stop }
            catch { $rollbackFailures.Add("Restore public proxy config: $($_.Exception.Message)") }
        } elseif (Test-Path -LiteralPath $oldProxyFile -PathType Leaf) {
            # Generated during this attempt, not part of the original LAN setup.
            try { Remove-Item -LiteralPath $oldProxyFile -Force -ErrorAction Stop }
            catch { $rollbackFailures.Add("Remove newly generated proxy config: $($_.Exception.Message)") }
        }
        # Reapply ONLY original application/Caddy services using restored LAN
        # env/Compose layers. Never recreate DB, Redis or identity volumes.
        try {
            $originalCp = @('compose','--env-file',(Join-Path $cpRoot '.env.runtime'),'-f',(Join-Path $cpRoot 'docker-compose.yml'))
            if ($cpFlavor -eq 'release') { $originalCp += @('-f',(Join-Path $cpRoot 'docker-compose.release.yml')) }
            Invoke-Docker -Arguments ($originalCp + @('up','-d','--no-deps','--pull','never','node-registry','presence-directory','push','caddy'))
        } catch { $rollbackFailures.Add("Restore LAN Control Plane: $($_.Exception.Message)") }
        try {
            $originalNode = @('compose','--env-file',(Join-Path $nodeRoot '.env.runtime'),'-f',(Join-Path $nodeRoot 'docker-compose.yml'),'-f',(Join-Path $nodeRoot 'docker-compose.release.yml'))
            Invoke-Docker -Arguments ($originalNode + @('up','-d','--no-deps','--pull','never','mailbox','federation','gateway','caddy'))
        } catch { $rollbackFailures.Add("Restore LAN Community Node: $($_.Exception.Message)") }
        if ($rollbackFailures.Count -gt 0) {
            throw "Public conversion failed: $migrationFailure. LAN rollback is INCOMPLETE: $($rollbackFailures -join '; '). Preserve every Docker volume; original config backups: $backup"
        }
        throw "Public conversion failed: $migrationFailure. Original LAN configuration and services restored without deleting Docker volumes. Config backups: $backup"
    }
}

# The Windows Forms manager may receive a null Process.ExitCode even after the
# child has terminated. Report the actual script result through a per-run,
# manager-created file instead of guessing based on the last log line.
function Write-TaskResult([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($ResultPath)) { return }
    try {
        [System.IO.File]::WriteAllText($ResultPath, $Value, [System.Text.UTF8Encoding]::new($false))
    } catch {
        [Console]::Error.WriteLine("Could not write Sparrow task result: $($_.Exception.Message)")
    }
}

try {
    Require-Docker
    if ($Action -eq 'Attach' -or (Test-Path -LiteralPath $attachmentFile -PathType Leaf)) {
        . (Join-Path $root 'Attached-SparrowDeployment.ps1')
        Attached-Invoke -Action $Action -Component $Component -PlanDirectory $PlanDirectory -ExpectedState $ExpectedState
        Write-TaskResult 'SUCCESS'
        exit 0
    }
    if ($Component -eq 'Proxy' -or $Action -eq 'Verify') { throw 'Proxy-only management and lifecycle verification require an explicitly attached Step 8f cutover. The fresh-install path was not run.' }
    if ($Action -eq 'Preflight') { Show-Preflight; Write-TaskResult 'SUCCESS'; exit 0 }
    if ($Action -eq 'Backup') {
        if (-not $BackupDirectory) { throw 'Choose a new backup destination.' }
        $backupParameters = @{ BackupDirectory = $BackupDirectory }
        if ($nodeSelected) {
            $backupParameters['NodeDirectory'] = if ($ExistingNodeDirectory) { $ExistingNodeDirectory } else { $nodeRoot }
        }
        if ($cpSelected) {
            $backupParameters['ControlPlaneDirectory'] = if ($ExistingControlPlaneDirectory) { $ExistingControlPlaneDirectory } else { $cpRoot }
        }
        $nodeCfg = if ($nodeSelected) { Read-Properties (Join-Path ([string]$backupParameters['NodeDirectory']) 'sparrow.conf') } else { @{} }
        $cpCfg = if ($cpSelected) { Read-Properties (Join-Path ([string]$backupParameters['ControlPlaneDirectory']) 'sparrow.conf') } else { @{} }
        if (($nodeCfg['MODE'] -eq 'public' -and $nodeCfg['SHARED_PROXY'] -eq 'true') -or
            ($cpCfg['MODE'] -eq 'public' -and $cpCfg['SHARED_PROXY'] -eq 'true')) {
            $backupParameters['IncludeSharedProxy'] = $true
            $backupParameters['ProxyDirectory'] = if ($ExistingProxyDirectory) { $ExistingProxyDirectory } else { $proxyRoot }
        }
        & (Join-Path $root 'Backup-SparrowDeployment.ps1') @backupParameters
        if (-not $?) { throw 'Offline backup command failed.' }
        Write-TaskResult 'SUCCESS'
        exit 0
    }
    # Public installer installs both services together. Component-specific lifecycle
    # remains an internal diagnostic capability, not a selectable install mode.
    if ($Action -eq 'Start' -and $Component -ne 'Combined') {
        throw 'Sparrow installation requires Combined (Control Plane + Community Node).'
    }
    if ($Action -eq 'Start') {
        if ($ImagePrefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$') { throw 'Invalid image prefix.' }
        if ($ImageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') { throw 'Invalid image tag.' }
        Set-CaddyAutomaticHostnames
        if ($shared) {
            if ($nodeSelected) { $NodeDomain = Check-Domain $NodeDomain }
            if ($cpSelected) { $ControlPlaneDomain = Check-Domain $ControlPlaneDomain }
            if ($nodeSelected -and $cpSelected -and $NodeDomain -eq $ControlPlaneDomain) { throw 'Node and Control Plane need distinct hostnames.' }
        }
        if ($ExistingDeployment -eq 'ReplaceTestData') {
            if ($Component -ne 'Combined' -or $ReplacementConfirmation -cne 'DELETE SPARROW SERVER DATA') {
                throw 'Fresh replacement is only available for Combined, with explicit DELETE SPARROW SERVER DATA confirmation.'
            }
            # Check prerequisites BEFORE deleting the old server. Replacing an
            # installed test server is destructive; no migration code is run.
            foreach ($required in @((Join-Path $root 'Reset-SparrowTestDeployment.ps1'),
                                   (Join-Path $cpRoot 'Bootstrap-ControlPlane.ps1'),
                                   (Join-Path $nodeRoot 'Bootstrap-CommunityNode.ps1'),
                                   (Join-Path $proxyRoot 'docker-compose.yml'))) {
                if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
                    throw "Installer asset missing: $required. Existing server was not changed."
                }
            }
            if ($shared) { Ensure-SharedNetwork }
            Write-Host 'EXPLICIT TEST REPLACEMENT: deleting existing Combined server identities, databases, queued messages, certificates and credentials; installing fresh Public configuration.'
            & (Join-Path $root 'Reset-SparrowTestDeployment.ps1') -InstallationDirectory $root -Component Combined -ReplaceInstalledTestData -DeleteDisposableData -Confirmation 'DELETE SPARROW SERVER DATA'
            if (-not $?) { throw 'The explicit test replacement failed. No new bootstrap was attempted.' }
        }
        $installedCp = Test-Path -LiteralPath (Join-Path $cpRoot '.env.runtime') -PathType Leaf
        $installedNode = Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf
        $cpBefore = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
        $nodeBefore = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
        if ($shared -and $Component -eq 'Combined' -and $installedCp -and $installedNode -and
            $ExistingDeployment -ne 'ReplaceTestData' -and
            ($cpBefore['MODE'] -eq 'lan' -or $nodeBefore['MODE'] -eq 'lan')) {
            # Only attempt metadata repair if Docker is already running the
            # Public CP layers. A genuine LAN install must NOT be converted.
            $cpCaddyIds = @(& docker ps -a --filter 'label=com.docker.compose.project=sparrow-control-plane' --filter 'label=com.docker.compose.service=caddy' --format '{{.ID}}' 2>$null)
            if ($LASTEXITCODE -ne 0) { throw 'Could not inspect existing Control Plane; no data was changed.' }
            if ($cpCaddyIds.Count -eq 1) {
                $cpFiles = [string](Get-SparrowDockerLabel -Resource container -Id ([string]$cpCaddyIds[0]) -Key 'com.docker.compose.project.config_files')
                if ($cpFiles -match 'docker-compose\.production\.yml|docker-compose\.shared-proxy\.yml') {
                    Repair-InstalledPublicMetadataIfAlreadyPublic
                    $cpBefore = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
                    $nodeBefore = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
                }
            }
        }
        # Read-only inference: a failed earlier public attempt may leave mode
        # metadata inconsistent with the *actual* LAN Compose deployment.
        $cpEffectiveMode = if ($shared -and $Component -eq 'Combined' -and $installedCp) { Get-SafeInstalledPublicMode $cpRoot 'sparrow-control-plane' } else { [string]$cpBefore['MODE'] }
        $nodeEffectiveMode = if ($shared -and $Component -eq 'Combined' -and $installedNode) { Get-SafeInstalledPublicMode $nodeRoot 'sparrow-community-node' } else { [string]$nodeBefore['MODE'] }
        if ($shared -and $Component -eq 'Combined' -and $installedCp -and $installedNode -and
            $cpEffectiveMode -eq 'lan' -and $nodeEffectiveMode -eq 'lan') {
            throw 'This is an installed LAN deployment, not an existing Public installation. Install / Start will not migrate or reinstall it. Use the separately confirmed fresh Public reinstall ONLY if the old TEST server data is disposable.'
        }
        if ($shared -and $Component -eq 'Combined' -and $installedCp -and $installedNode -and
            $cpEffectiveMode -ne $nodeEffectiveMode) {
            throw 'One component has an incomplete LAN-to-Public transition. Preserve all data and consult the migration-backups folder; do not run fresh-install cleanup.'
        }
        # Install / Start updates software; it must never silently reconfigure
        # the installed deployment's network mode or hostname.
        foreach ($directory in @($nodeRoot, $cpRoot)) {
            if (($directory -eq $nodeRoot -and -not $nodeSelected) -or
                ($directory -eq $cpRoot -and -not $cpSelected)) { continue }
            if (-not (Test-Path -LiteralPath (Join-Path $directory '.env.runtime') -PathType Leaf)) { continue }
            $installedConfig = Read-Properties (Join-Path $directory 'sparrow.conf')
            if ($installedConfig['MODE'] -ne $Mode) {
                throw "Already-installed $directory uses mode $($installedConfig['MODE']); image updates cannot change its mode."
            }
            if ($Mode -eq 'public') {
                $desiredDomain = if ($directory -eq $nodeRoot) { $NodeDomain } else { $ControlPlaneDomain }
                if ($installedConfig['PUBLIC_DOMAIN'] -ne $desiredDomain) {
                    throw "Already-installed $directory uses a different public hostname; a reviewed proxy/TLS reconfiguration is required."
                }
            }
        }
        # A Public -> LAN transition or hostname change must be planned with
        # a proxy route update, not silently leave an old public endpoint live.
        foreach ($directory in @($nodeRoot, $cpRoot)) {
            if (($directory -eq $nodeRoot -and -not $nodeSelected) -or
                ($directory -eq $cpRoot -and -not $cpSelected)) { continue }
            $before = Read-Properties (Join-Path $directory 'sparrow.conf')
            if ($before['MODE'] -eq 'public' -and
                (Test-Path -LiteralPath (Join-Path $directory '.env.runtime') -PathType Leaf)) {
                $newHost = if ($directory -eq $nodeRoot) { $NodeDomain } else { $ControlPlaneDomain }
                if ($Mode -ne 'public' -or ($before['PUBLIC_DOMAIN'] -and $before['PUBLIC_DOMAIN'] -ne $newHost)) {
                    throw 'Changing or removing an installed public hostname requires an explicit proxy/TLS cutover. No existing deployment was changed.'
                }
            }
        }
        if ($nodeSelected -and $DirectoryUrl) {
            $validatedDirectory = $null
            if (-not [uri]::TryCreate($DirectoryUrl, [UriKind]::Absolute, [ref]$validatedDirectory) -or
                $validatedDirectory.Scheme -notin @('https', 'http') -or
                ($shared -and $validatedDirectory.Scheme -ne 'https') -or
                $validatedDirectory.UserInfo -or $validatedDirectory.Fragment) {
                throw 'Control Plane directory URL must be a valid HTTP(S) URL without credentials or fragment (HTTPS in Public mode).'
            }
        }
        if ($nodeSelected) {
            $priorNode = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
            if (-not $DirectoryUrl) {
                $priorCp = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
                $DirectoryUrl = if ($priorNode['CONTROL_PLANE_DIRECTORY_URL']) { [string]$priorNode['CONTROL_PLANE_DIRECTORY_URL'] } else { [string]$priorCp['CONTROL_PLANE_DIRECTORY_URL'] }
            }
            if ($Component -eq 'Node' -and -not $DirectoryUrl) {
                throw 'Node-only installation requires a Control Plane directory URL.'
            }
            # A directory change is a normal Install/Start reconfiguration, not
            # a migration. Sync-InstalledNodeDiscovery updates only the node's
            # discovery environment and the gateway/federation/mailbox services.
        }
        # Check required bundle assets before touching any old TEST data.
        $freshRequired = @((Join-Path $root 'Reset-SparrowTestDeployment.ps1'))
        if ($nodeSelected) { $freshRequired += (Join-Path $nodeRoot 'Bootstrap-CommunityNode.ps1') }
        if ($cpSelected) { $freshRequired += (Join-Path $cpRoot 'Bootstrap-ControlPlane.ps1') }
        if ($shared) { $freshRequired += (Join-Path $proxyRoot 'docker-compose.yml') }
        foreach ($required in $freshRequired) {
            if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Installer bundle missing $required" }
        }
        # Create/check the shared network BEFORE the optional destructive
        # disposal of orphaned TEST Docker resources. A missing network must
        # never cause data deletion followed by an avoidable install failure.
        if ($shared) { Ensure-SharedNetwork }
        # Never erase orphaned Docker data merely because an env file is missing.
        # Only the separately confirmed Reinstall Public action may reset data.
        if ($ExistingDeployment -ne 'ReplaceTestData') {
            if ($nodeSelected) { Assert-NotOrphaned $nodeRoot 'sparrow-community-node' }
            if ($cpSelected) { Assert-NotOrphaned $cpRoot 'sparrow-control-plane' }
        }
        Assert-DeploymentsNotRunningElsewhere
        # Old standalone Public Caddy containers may have held 80/443. Check
        # port ownership only AFTER removing explicitly abandoned TEST projects.
        if ($shared) { Check-PublicPortOwnership }
        if ($cpSelected) { Assert-NotOrphaned $cpRoot 'sparrow-control-plane' }
        if ($nodeSelected) { Assert-NotOrphaned $nodeRoot 'sparrow-community-node' }
        if ($shared) {
            foreach ($directory in @($nodeRoot, $cpRoot)) {
                if (-not (Test-Path -LiteralPath (Join-Path $directory '.env.runtime') -PathType Leaf)) { continue }
                $existingConfig = Read-Properties (Join-Path $directory 'sparrow.conf')
                if ($existingConfig['MODE'] -eq 'public' -and $existingConfig['SHARED_PROXY'] -ne 'true') {
                    throw "Existing standalone Public mode detected in $directory. Automatic cutover may lose certificates; no changes made."
                }
            }
        }
        # Preflight all selected files before changing any persistent configuration.
        $requiredFiles = @()
        if ($nodeSelected) { $requiredFiles += (Join-Path $nodeRoot 'Bootstrap-CommunityNode.ps1') }
        if ($cpSelected) { $requiredFiles += (Join-Path $cpRoot 'Bootstrap-ControlPlane.ps1') }
        if ($shared) { $requiredFiles += (Join-Path $proxyRoot 'docker-compose.yml') }
        foreach ($required in $requiredFiles) {
            if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Installer bundle missing $required" }
        }
        if ($nodeSelected -and (Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf)) {
            # Refresh FIRST, even when GHCR is unavailable for an unrelated CP
            # image pull. This also corrects legacy localhost advertisements.
            Sync-InstalledNodeDiscovery $DirectoryUrl
        }
        if ($cpSelected) {
            $previous = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
            $cpId = if ($previous['CONTROL_PLANE_ID']) { $previous['CONTROL_PLANE_ID'] } else { [Guid]::NewGuid().ToString('N') }
            if (Test-Path -LiteralPath (Join-Path $cpRoot '.env.runtime') -PathType Leaf) {
                # Normal Install / Start also configures Firebase for an already
                # installed Control Plane. Previously only the fresh-install
                # branch imported credentials, leaving FCM disabled forever.
                # Do not change the existing Control Plane identity, databases,
                # public hostnames, or Docker volumes.
                if ($FirebaseMode -eq 'Enabled' -and
                    -not [string]::IsNullOrWhiteSpace($FirebaseCredentialsPath)) {
                    $credential = Get-PushCredential
                    if (-not $credential -or -not (Test-Path -LiteralPath $credential -PathType Leaf)) {
                        throw 'Firebase service-account import failed. Select a valid Firebase Admin service-account JSON; the installed Control Plane was not reset.'
                    }
                    # Persist the host path so Get-ComposeArguments includes the
                    # optional Firebase override on subsequent starts/updates.
                    Write-Properties (Join-Path $cpRoot '.env.runtime') @{
                        FIREBASE_ADMIN_CREDENTIALS = $credential.Replace('\', '/')
                    }
                    Write-Host 'Firebase Admin credentials configured for existing Control Plane (no server reset).'
                } elseif ($FirebaseMode -eq 'Disabled') {
                    # An unchecked UI option on a routine update must not
                    # silently strip an already working production FCM setup.
                    Write-Host 'Firebase import not requested; existing installed push configuration preserved.'
                }
                # Updating only the image must never reset CP identity, public
                # hostname, push mode, or existing configuration.
                Write-Properties (Join-Path $cpRoot 'sparrow.conf') @{
                    SPARROW_IMAGE_PREFIX = $ImagePrefix; SPARROW_IMAGE_TAG = $ImageTag
                    CONTROL_PLANE_DIRECTORY_URL = $DirectoryUrl
                }
            } else {
                Write-Properties (Join-Path $cpRoot 'sparrow.conf') @{
                    CONFIGURED = 'true'; CONTROL_PLANE_ID = $cpId; MODE = $Mode
                    PUBLIC_DOMAIN = $ControlPlaneDomain
                    SHARED_PROXY = $(if ($shared) { 'true' } else { 'false' })
                    CONTROL_PLANE_DIRECTORY_URL = $DirectoryUrl
                    SPARROW_IMAGE_PREFIX = $ImagePrefix; SPARROW_IMAGE_TAG = $ImageTag
                }
                if ($FirebaseMode -eq 'Disabled') {
                    $env:SPARROW_DISABLE_FCM = '1'
                    Remove-Item Env:FIREBASE_ADMIN_CREDENTIALS -ErrorAction SilentlyContinue
                    Write-Host 'FCM explicitly disabled by the operator.'
                } else {
                    Remove-Item Env:SPARROW_DISABLE_FCM -ErrorAction SilentlyContinue
                    $credential = Get-PushCredential
                    if ($credential) { $env:FIREBASE_ADMIN_CREDENTIALS = $credential }
                }
            }
            if ($shared) {
                # Create this non-secret, host-writable directory BEFORE Compose mounts it.
                # Otherwise Docker may create a root-owned directory that the
                # registration client cannot write to.
                New-Item -ItemType Directory -Path (Join-Path $cpRoot 'directory-registration-proofs') -Force | Out-Null
            }
            Start-OrUpdateComponent $cpRoot 'Bootstrap-ControlPlane.ps1' @('node-registry', 'presence-directory', 'push')
        }
        if ($nodeSelected) {
            if (Test-Path -LiteralPath (Join-Path $nodeRoot '.env.runtime') -PathType Leaf) {
                # Never erase an existing local CP, manual directory or public
                # routing when the operator selects Node-only for image updates.
                Write-Properties (Join-Path $nodeRoot 'sparrow.conf') @{
                    SPARROW_IMAGE_PREFIX = $ImagePrefix; SPARROW_IMAGE_TAG = $ImageTag
                }
            } else {
                $cpUrls = $(if ($Component -eq 'Combined') {
                    if ($shared) { "https://$ControlPlaneDomain" } else { 'http://localhost:8390' }
                } else { '' })
                Write-Properties (Join-Path $nodeRoot 'sparrow.conf') @{
                    CONFIGURED = 'true'; MODE = $Mode
                    PUBLIC_DOMAIN = $NodeDomain
                    SHARED_PROXY = $(if ($shared) { 'true' } else { 'false' })
                    LOCAL_CONTROL_PLANE_DOMAIN = $(if ($Component -eq 'Combined' -and $shared) { $ControlPlaneDomain } else { '' })
                    CONTROL_PLANE_DIRECTORY_URL = $DirectoryUrl
                    CONTROL_PLANE_URLS = $cpUrls
                    SPARROW_IMAGE_PREFIX = $ImagePrefix; SPARROW_IMAGE_TAG = $ImageTag
                }
            }
            Start-OrUpdateComponent $nodeRoot 'Bootstrap-CommunityNode.ps1' @('mailbox', 'federation', 'gateway')
        }
        if ($shared) {
            # The only proof directory mounted by the public proxy. Not secrets/.
            $proofDirectory = Join-Path $cpRoot 'directory-registration-proofs'
            if (-not (Test-Path -LiteralPath $proofDirectory -PathType Container)) {
                New-Item -ItemType Directory -Path $proofDirectory -Force | Out-Null
            }
            New-Item -ItemType Directory -Path (Join-Path $root 'node-instances/routes') -Force | Out-Null
            Write-ProxyConfig
            Invoke-Docker -Arguments @('compose', '-f', (Join-Path $proxyRoot 'docker-compose.yml'), 'up', '-d')
            Invoke-Docker -Arguments @('compose', '-f', (Join-Path $proxyRoot 'docker-compose.yml'), 'exec', '-T', 'caddy', 'caddy', 'reload', '--config', '/etc/caddy/Caddyfile')
            Write-Host 'Shared proxy started. DNS/TLS and external WebSocket reachability still need verification.'
        }
        if ($Component -eq 'Combined') { Assert-CombinedNodeDiscoverable }
        if ($shared) { Start-SparrowDirectoryBackgroundSync }
        if ($cpSelected) { Refresh-PublicControlPlaneDirectory }
        if ($shared -and $cpSelected) { Register-PublicControlPlaneInDirectory }
    } elseif ($Action -in @('Stop', 'Restart', 'Status', 'Logs')) {
        $directories = @()
        if ($cpSelected) { $directories += $cpRoot }
        if ($nodeSelected) { $directories += $nodeRoot }
        foreach ($directory in $directories) {
            if ($Action -eq 'Status') { Show-ComponentStatus $directory; continue }
            if (-not (Test-Path -LiteralPath (Join-Path $directory '.env.runtime') -PathType Leaf)) {
                Write-Host "$(Get-ComponentName $directory): not installed in this bundle."
                continue
            }
            $command = switch ($Action) {
                'Stop' { @('stop') }
                # `compose restart` does NOT start a previously stopped container.
                'Restart' { @('up', '-d', '--no-recreate') }
                'Logs' { @('logs', '--no-color', '--tail', '120') }
            }
            Write-Host "==== $(Get-ComponentName $directory): $Action ===="
            Invoke-ComponentCommand $directory $command
        }
        $proxyCompose = Join-Path $proxyRoot 'docker-compose.yml'
        if (Test-Path -LiteralPath (Join-Path $proxyRoot 'Caddyfile') -PathType Leaf) {
            if ($Action -eq 'Status') {
                Write-Host '==== Public entry proxy (independent component) ===='
                Invoke-Docker -Arguments @('compose', '-f', $proxyCompose, 'ps', '--all')
            } elseif ($Action -eq 'Logs') {
                Invoke-Docker -Arguments @('compose', '-f', $proxyCompose, 'logs', '--no-color', '--tail', '120')
            } elseif ($Action -eq 'Stop' -and $Component -eq 'Combined' -and (Test-Path -LiteralPath (Join-Path $root 'node-instances/routes') -PathType Container) -and @(Get-ChildItem -LiteralPath (Join-Path $root 'node-instances/routes') -Filter '*.caddy' -File).Count -gt 0) {
                Write-Host 'Shared public proxy remains online for separately managed Community Nodes.'
            } elseif ($Action -eq 'Stop' -and $Component -eq 'Combined') {
                # Only stop the shared proxy when stopping both components. A
                # single-component stop must not interrupt the other hostname.
                Invoke-Docker -Arguments @('compose', '-f', $proxyCompose, '--profile', 'directory', 'stop')
            } elseif ($Action -eq 'Restart' -and $Component -eq 'Combined') {
                # After Stop Combined, compose restart will not start stopped
                # services. Restore the public entry point without re-creating
                # certificates or touching persistent volumes.
                $nodeCfg = Read-Properties (Join-Path $nodeRoot 'sparrow.conf')
                $cpCfg = Read-Properties (Join-Path $cpRoot 'sparrow.conf')
                if (($nodeCfg['MODE'] -eq 'public' -and $nodeCfg['SHARED_PROXY'] -eq 'true') -or
                    ($cpCfg['MODE'] -eq 'public' -and $cpCfg['SHARED_PROXY'] -eq 'true')) {
                    Invoke-Docker -Arguments @('compose', '-f', $proxyCompose, 'up', '-d', '--no-recreate')
                    Start-SparrowDirectoryBackgroundSync
                }
            } elseif ($Action -eq 'Restart') {
                Write-Host 'Shared proxy is unchanged by single-component restart.'
            }
        }
    }
    if ($Action -in @('Start', 'Status')) { Show-PublicEndpoints }
    Write-Host "Completed: $Action ($Component)."
    Write-TaskResult 'SUCCESS'
    exit 0
} catch {
    $failureMessage = "Sparrow Server: $($_.Exception.Message)"
    # A redirected Windows PowerShell 5.1 stderr stream can contain CLIXML
    # mixed with native stderr. Keep the actual failure in a separate, plain
    # text file that is readable both by the manager and the operator.
    if ($FailurePath) {
        try {
            [System.IO.File]::WriteAllText($FailurePath, ($failureMessage + [Environment]::NewLine),
                [System.Text.UTF8Encoding]::new($false))
        } catch {
            [Console]::Error.WriteLine("Could not write Sparrow failure log: $($_.Exception.Message)")
        }
    }
    [Console]::Error.WriteLine($failureMessage)
    Write-TaskResult 'FAILED'
    exit 1
}
