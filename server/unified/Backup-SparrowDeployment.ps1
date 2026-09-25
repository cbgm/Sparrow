# Offline, non-destructive preparation for an existing Sparrow deployment migration.
# This tool NEVER stops services, recreates containers or changes a Docker volume.
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BackupDirectory,
    [string]$NodeDirectory = '',
    [string]$ControlPlaneDirectory = '',
    [string]$ProxyDirectory = '',
    [switch]$IncludeSharedProxy
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-SparrowDockerLabel.ps1')

function Invoke-DockerChecked([string[]]$Arguments) {
    $result = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed: docker $($Arguments[0]) $($Arguments[1]) : $($result -join ' ')" }
    return $result
}
function Read-Env([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') {
            $values[$Matches[1]] = $Matches[2].Trim().Trim([char]34).Trim([char]39)
        }
    }
    return $values
}
function Ensure-Stopped([string]$Project) {
    $running = @(Invoke-DockerChecked @('ps', '--filter', "label=com.docker.compose.project=$Project", '--format', '{{.ID}}'))
    if ($running.Count -gt 0) {
        throw "Project $Project is RUNNING. Stop it using its ORIGINAL installer/Compose directory and rerun the backup. No containers have been stopped by this tool."
    }
}
function Normalize-Workdir([string]$Value) {
    $clean = $Value.Trim().TrimEnd('/', '\')
    if ($clean -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
        $clean = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
    }
    return [System.IO.Path]::GetFullPath($clean).TrimEnd('\')
}
function Check-ProjectFolder([string]$Project, [string]$Folder) {
    $ids = @(Invoke-DockerChecked @('ps', '-a', '--filter', "label=com.docker.compose.project=$Project", '--format', '{{.ID}}'))
    if ($ids.Count -eq 0) { throw "Cannot verify the Compose project $Project: no containers found. Refusing to assume a volume belongs to this deployment." }
    foreach ($id in $ids) {
        $workdir = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir')
        if ($workdir.Count -ne 1 -or -not $workdir[0] -or $workdir[0] -eq '<no value>') {
            throw "Missing working-directory label on $Project container $id. Cannot verify project ownership."
        }
        $actual = Normalize-Workdir $workdir[0]
        $expected = Normalize-Workdir $Folder
        if (-not $actual.Equals($expected, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Project $Project belongs to $actual, not $expected. Specify the ORIGINAL installation directory."
        }
    }
}
function Assert-PrivateDirectory([string]$Directory) {
    New-Item -ItemType Directory -Path $Directory -ErrorAction Stop | Out-Null
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $Directory /inheritance:r /grant:r "*${sid}:(OI)(CI)(F)" '*S-1-5-18:(OI)(CI)(F)' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not restrict backup directory ACL: $Directory. No secrets have been copied." }
}
function Copy-SnapshotFile([string]$Source, [string]$Relative, [string]$Original) {
    if (-not (Test-Path -LiteralPath $Source -PathType Leaf)) { throw "Required file is missing: $Source" }
    $item = Get-Item -LiteralPath $Source -Force
    if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Refusing to follow a symbolic link for a secret or config file: $Source"
    }
    $before = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $destination = Join-Path $targetRoot $Relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $destination -ErrorAction Stop
    $after = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $copyHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
    if ($before -ne $after -or $before -ne $copyHash) { throw "File changed while backing up $Source. Backup incomplete." }
    $script:files += [PSCustomObject]@{ Original = $Original; Backup = $Relative.Replace('\', '/'); Sha256 = $copyHash }
}
function Copy-ComponentSnapshot([string]$Folder, [string]$Name, [hashtable]$Runtime) {
    $prefix = "components/$Name"
    $items = @('sparrow.conf', '.env.runtime', '.env', 'Caddyfile', 'index.html')
    foreach ($item in $items) {
        $source = Join-Path $Folder $item
        if (Test-Path -LiteralPath $source -PathType Leaf) { Copy-SnapshotFile $source "$prefix/$item" $source }
    }
    foreach ($source in @(Get-ChildItem -LiteralPath $Folder -File -Filter 'docker-compose*.yml' -ErrorAction Stop)) {
        Copy-SnapshotFile $source.FullName "$prefix/$($source.Name)" $source.FullName
    }
    $secrets = Join-Path $Folder 'secrets'
    if ((Test-Path -LiteralPath $secrets) -and ((Get-Item -LiteralPath $secrets -Force).Attributes -band [System.IO.FileAttributes]::ReparsePoint)) { throw "Refusing to follow a symbolic link for secrets: $secrets" }
    if (-not (Test-Path -LiteralPath $secrets -PathType Container)) { throw "Missing secrets directory: $secrets" }
    foreach ($item in @(Get-ChildItem -LiteralPath $secrets -File -Recurse -Force -ErrorAction Stop)) {
        $relative = $item.FullName.Substring($secrets.Length).TrimStart([char[]]@([char]92, [char]47))
        Copy-SnapshotFile $item.FullName "$prefix/secrets/$relative" $item.FullName
    }
    if ($Name -eq 'control-plane' -and $Runtime['FIREBASE_ADMIN_CREDENTIALS']) {
        $credential = $Runtime['FIREBASE_ADMIN_CREDENTIALS']
        if (-not [System.IO.Path]::IsPathRooted($credential)) { throw 'FCM credential path is not absolute; cannot reliably preserve the secret.' }
        Copy-SnapshotFile $credential 'external-secrets/control-plane/firebase-admin.json' $credential
    }
}

$targetRoot = [System.IO.Path]::GetFullPath($BackupDirectory)
if (Test-Path -LiteralPath $targetRoot) { throw "Backup destination must be a new, nonexistent directory: $targetRoot" }
if ($targetRoot.Contains(',')) { throw 'Choose a backup path without commas (Docker bind mount syntax).' }
if (-not $NodeDirectory -and -not $ControlPlaneDirectory) { throw 'Specify at least one ORIGINAL component installation directory.' }
$components = @()
if ($NodeDirectory) { $components += @{ Name='community-node'; Folder=[System.IO.Path]::GetFullPath($NodeDirectory); Project='sparrow-community-node'; Key='COMMUNITY_NODE_PROJECT_NAME' } }
if ($ControlPlaneDirectory) { $components += @{ Name='control-plane'; Folder=[System.IO.Path]::GetFullPath($ControlPlaneDirectory); Project='sparrow-control-plane'; Key='CONTROL_PLANE_PROJECT_NAME' } }
foreach ($component in $components) {
    if ($targetRoot.StartsWith(($component.Folder.TrimEnd([char[]]@([char]92, [char]47)) + [System.IO.Path]::DirectorySeparatorChar), [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Backup destination must be outside the original installation directory.'
    }
    foreach ($item in @('sparrow.conf', '.env.runtime')) {
        if (-not (Test-Path -LiteralPath (Join-Path $component.Folder $item) -PathType Leaf)) {
            throw "Missing $item in $($component.Folder). This is not a configured Sparrow deployment."
        }
    }
    $runtime = Read-Env (Join-Path $component.Folder '.env.runtime')
    if ($runtime[$component.Key] -ne $component.Project) { throw "Unexpected Compose project in $($component.Folder)." }
    $component['Runtime'] = $runtime
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is not available.' }
Invoke-DockerChecked @('info', '--format', '{{.ServerVersion}}') | Out-Null
$projects = @($components | ForEach-Object { $_.Project })
if ($IncludeSharedProxy) {
    if (-not $ProxyDirectory) { throw 'Supply the ORIGINAL shared proxy directory when -IncludeSharedProxy is set.' }
    $ProxyDirectory = [System.IO.Path]::GetFullPath($ProxyDirectory)
    foreach ($required in @('docker-compose.yml', 'Caddyfile')) {
        if (-not (Test-Path -LiteralPath (Join-Path $ProxyDirectory $required) -PathType Leaf)) {
            throw "Missing original shared proxy file: $required."
        }
    }
    $projects += 'sparrow-public-proxy'
}
foreach ($component in $components) { Check-ProjectFolder $component.Project $component.Folder }
if ($IncludeSharedProxy) { Check-ProjectFolder 'sparrow-public-proxy' $ProxyDirectory }
foreach ($component in $components) {
    $cfg = Read-Env (Join-Path $component.Folder 'sparrow.conf')
    if ($cfg['MODE'] -eq 'public' -and $cfg['SHARED_PROXY'] -eq 'true' -and -not $IncludeSharedProxy) {
        throw 'This installation uses the shared Public proxy. Include its ORIGINAL directory and stopped Docker project in the offline snapshot.'
    }
}
foreach ($project in $projects) { Ensure-Stopped $project }
# Verify independent containers have not mounted any selected volume before snapshotting.
$volumes = @()
foreach ($project in $projects) {
    $found = @(Invoke-DockerChecked @('volume', 'ls', '--quiet', '--filter', "label=com.docker.compose.project=$project"))
    if ($found.Count -eq 0) { throw "No labeled persistent volumes found for $project; cannot produce a complete offline snapshot." }
    # Do not call this snapshot complete if a stopped project container uses an
    # external/unlabelled Docker volume that the project-label scan would miss.
    $containerIds = @(Invoke-DockerChecked @('ps', '-a', '--filter', "label=com.docker.compose.project=$project", '--format', '{{.ID}}'))
    foreach ($containerId in $containerIds) {
        # JSON avoids the Windows PowerShell 5.1 native quoting problem with
        # Go templates containing string literals such as "volume".
        $mountJson = @(Invoke-DockerChecked @('inspect', '--format', '{{json .Mounts}}', $containerId))
        if ($mountJson.Count -ne 1) { throw "Cannot inspect mounts for $containerId." }
        try { $mountRecords = @(($mountJson[0] | ConvertFrom-Json -ErrorAction Stop)) }
        catch { throw "Docker returned invalid mount JSON for $containerId." }
        $mountedVolumes = @($mountRecords | Where-Object { $_.Type -eq 'volume' } | ForEach-Object { [string]$_.Name })
        foreach ($mounted in $mountedVolumes) {
            if ($mounted -and $mounted -notin $found) {
                throw "Project $project uses $mounted, which has no matching project volume label. Refusing an incomplete snapshot."
            }
        }
    }
    foreach ($volume in $found) {
        if ($volume -notmatch '^[a-zA-Z0-9][a-zA-Z0-9_.-]*$') { throw 'Unexpected Docker volume name.' }
        $driver = @(Invoke-DockerChecked @('volume', 'inspect', $volume, '--format', '{{.Driver}}'))
        if ($driver.Count -ne 1 -or $driver[0] -ne 'local') { throw "Unsupported Docker volume driver for $volume. No snapshot attempted." }
        $users = @(Invoke-DockerChecked @('ps', '--filter', "volume=$volume", '--format', '{{.ID}}'))
        if ($users.Count -gt 0) { throw "Volume $volume is mounted by a running container. Stop dependent services and retry." }
        $volumes += [PSCustomObject]@{ Project=$project; Name=$volume }
    }
}
Assert-PrivateDirectory $targetRoot
$script:files = @()
$script:volumeRecords = @()
$completeMarker = Join-Path $targetRoot 'INCOMPLETE-DO-NOT-RESTORE.txt'
[System.IO.File]::WriteAllText($completeMarker, 'Backup is in progress or failed. This directory is NOT a verified rollback point.')
try {
    foreach ($component in $components) { Copy-ComponentSnapshot $component.Folder $component.Name $component.Runtime }
    if ($IncludeSharedProxy) {
        foreach ($name in @('docker-compose.yml', 'Caddyfile')) {
            $source = Join-Path $ProxyDirectory $name
            Copy-SnapshotFile $source "shared-proxy/$name" $source
        }
    }
    foreach ($volume in $volumes) {
        foreach ($project in $projects) { Ensure-Stopped $project }
        $running = @(Invoke-DockerChecked @('ps', '--filter', "volume=$($volume.Name)", '--format', '{{.ID}}'))
        if ($running.Count -gt 0) { throw "Volume $($volume.Name) became active during snapshot." }
        $relative = "volumes/$($volume.Name).tar.gz"
        $path = Join-Path $targetRoot $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
        # GNU tar records ownership, permissions, ACLs and extended attributes.
        # The original Docker volume is mounted read-only; no network is needed.
        Invoke-DockerChecked @('run', '--rm', '--network', 'none', '--mount', "type=volume,source=$($volume.Name),target=/source,readonly", '--mount', "type=bind,source=$targetRoot,target=/backup", 'ubuntu:24.04', 'tar', '--acls', '--xattrs', '--numeric-owner', '-czf', "/backup/$relative", '-C', '/source', '.') | Out-Null
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -eq 0) { throw "Volume archive is absent/empty: $relative" }
        $script:volumeRecords += [PSCustomObject]@{ Project=$volume.Project; OriginalVolume=$volume.Name; Backup=$relative; Sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash }
        Write-Host "Captured $($volume.Name) offline."
    }
    foreach ($project in $projects) { Ensure-Stopped $project }
    $manifest = [PSCustomObject]@{
        FormatVersion=1; CreatedUtc=[DateTime]::UtcNow.ToString('o'); Status='CompleteOfflineSnapshot'
        RestorePolicy='Manual verification and restore required. The installer does not automatically replace existing deployments.'
        Components=@($components | ForEach-Object { [PSCustomObject]@{ Name=$_.Name; OriginalDirectory=$_.Folder; ComposeProject=$_.Project } }); SharedProxyDirectory=$(if ($IncludeSharedProxy) { $ProxyDirectory } else { $null })
        Files=@($script:files); Volumes=@($script:volumeRecords)
    }
    $manifestPath = Join-Path $targetRoot 'manifest.json'
    [System.IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 10), [System.Text.UTF8Encoding]::new($false))
    Remove-Item -LiteralPath $completeMarker -Force
    Write-Host "Offline snapshot complete: $targetRoot. All selected services remain stopped. Restore has NOT been performed or tested."
} catch {
    Write-Host 'Snapshot INCOMPLETE. Existing Docker volumes and containers were not modified by this tool.'
    throw
}
