# Stage legacy standalone Public Caddy certificate storage for the shared proxy.
# Only NEW, Compose-named shared-proxy volumes are written. Original deployments,
# Docker volumes, host ports and the verified offline snapshot are never changed.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$BackupDirectory,
    [Parameter(Mandatory=$true)][string]$StageDirectory
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-SparrowDockerLabel.ps1')

$backup = [System.IO.Path]::GetFullPath($BackupDirectory)
$stage = [System.IO.Path]::GetFullPath($StageDirectory)
if ($backup.Contains(',') -or $stage.Contains(',')) { throw 'Docker bind mount paths cannot contain commas.' }
if ((Test-Path -LiteralPath $stage) -or $stage.Equals($backup, [StringComparison]::OrdinalIgnoreCase) -or
    $stage.StartsWith(($backup.TrimEnd([char[]]@([char]92,[char]47)) + [System.IO.Path]::DirectorySeparatorChar), [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Choose a NEW, separate staging directory outside the verified offline snapshot.'
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is required.' }
& (Join-Path $PSScriptRoot 'Test-SparrowPublicCutover.ps1') -BackupDirectory $backup
if (-not $?) { throw 'Legacy Public backup preflight failed; nothing was staged.' }
$manifest = Get-Content -LiteralPath (Join-Path $backup 'manifest.json') -Raw | ConvertFrom-Json

$legacy = @()
$hostnames = @()
foreach ($component in @($manifest.Components)) {
    $config = @{}
    foreach ($line in (Get-Content -LiteralPath (Join-Path $backup "components/$($component.Name)/sparrow.conf"))) {
        if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') { $config[$Matches[1]] = $Matches[2].Trim().Trim([char]34).Trim([char]39) }
    }
    if ($config['MODE'] -ne 'public') { continue }
    if ($config['SHARED_PROXY'] -eq 'true') { throw 'Shared-proxy snapshots require a separate recovery path.' }
    $sourceVolume = "$($component.ComposeProject)_caddy-data"
    $entry = @($manifest.Volumes | Where-Object { $_.OriginalVolume -eq $sourceVolume })
    if ($entry.Count -ne 1 -or $entry[0].Backup -notmatch '^volumes/[A-Za-z0-9_.-]+\.tar\.gz$') {
        throw "Missing or invalid Caddy data archive for $sourceVolume."
    }
    $legacy += $entry[0]
    $hostnames += [string]$config['PUBLIC_DOMAIN']
}
if ($legacy.Count -eq 0) { throw 'No standalone Public certificate volume in the snapshot.' }

$volumeData = 'sparrow-public-proxy_caddy-data'
$volumeConfig = 'sparrow-public-proxy_caddy-config'
foreach ($name in @($volumeData,$volumeConfig)) {
    $existing = @(& docker volume inspect $name --format '{{.Name}}' 2>$null)
    if ($LASTEXITCODE -eq 0) {
        throw "Shared proxy volume $name ALREADY EXISTS. Refusing to overwrite active or staged TLS state. No resources were changed."
    }
}
$created = New-Object 'System.Collections.Generic.List[string]'
$complete = $false
try {
    New-Item -ItemType Directory -Path $stage -ErrorAction Stop | Out-Null
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $stage /inheritance:r /grant:r "*${sid}:(OI)(CI)(F)" '*S-1-5-18:(OI)(CI)(F)' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not protect staging directory ACL. No volumes were created.' }
    [System.IO.File]::WriteAllText((Join-Path $stage 'INCOMPLETE-DO-NOT-CUTOVER.txt'),
        'TLS import is in progress or failed. Do NOT point a public proxy at these volumes.')
    foreach ($name in @($volumeData,$volumeConfig)) {
        $result = @(& docker volume create --label 'com.docker.compose.project=sparrow-public-proxy' `
            --label "com.docker.compose.volume=$(if ($name -eq $volumeData) { 'caddy-data' } else { 'caddy-config' })" `
            --label 'sparrow.tls.stage=offline-import' $name 2>&1)
        if ($LASTEXITCODE -ne 0 -or $result.Count -ne 1 -or $result[0] -ne $name) {
            throw "Could not create staged shared-proxy volume $name."
        }
        $created.Add($name)
    }
    $mergeScript = @'
set -euo pipefail
mkdir -p /incoming
# Fail closed on symlinks, devices and other unusual archive entries. The
# saved Caddy store is expected to contain only ordinary files/directories.
tar --acls --xattrs --numeric-owner -xzf "$1" -C /incoming
if [ -n "$(find /incoming -mindepth 1 ! -type f ! -type d -print -quit)" ]; then
    echo 'Unexpected non-file/non-directory entry in backed-up Caddy data' >&2; exit 1
fi
while IFS= read -r -d '' item; do
    rel="${item#/incoming/}"
    if [ -e "/target/$rel" ] && [ ! -d "/target/$rel" ]; then
        echo "Caddy data directory collides with a file: $rel" >&2; exit 1
    fi
    mkdir -p "/target/$rel"
done < <(find /incoming -mindepth 1 -type d -print0)
while IFS= read -r -d '' item; do
    rel="${item#/incoming/}"
    dest="/target/$rel"
    if [ -e "$dest" ]; then
        if [ ! -f "$dest" ] || ! cmp -s -- "$item" "$dest"; then
            echo "Conflicting TLS store file: $rel; refusing to overwrite either certificate store" >&2
            exit 1
        fi
    else
        mkdir -p "$(dirname "$dest")"
        cp -p -- "$item" "$dest"
    fi
done < <(find /incoming -mindepth 1 -type f -print0)
'@
    foreach ($entry in $legacy) {
        $verifiedHash = (Get-FileHash -LiteralPath (Join-Path $backup $entry.Backup) -Algorithm SHA256).Hash
        if ($verifiedHash -ne $entry.Sha256) { throw "Caddy archive changed after preflight: $($entry.Backup)." }
        $archive = "/backup/$($entry.Backup)"
        $result = @(& docker run --rm --network none `
            --mount "type=bind,source=$backup,target=/backup,readonly" `
            --mount "type=volume,source=$volumeData,target=/target" `
            ubuntu:24.04 bash -euo pipefail -c $mergeScript sparrow-tls-stage $archive 2>&1)
        if ($LASTEXITCODE -ne 0) { throw "TLS merge failed for $($entry.OriginalVolume): $($result -join ' ')" }
        if ((Get-FileHash -LiteralPath (Join-Path $backup $entry.Backup) -Algorithm SHA256).Hash -ne $entry.Sha256) {
            throw "Caddy archive changed during import: $($entry.Backup)."
        }
        Write-Host "Imported offline Caddy TLS store from $($entry.OriginalVolume); original volume was not mounted or changed."
    }
    # Ensure every backed-up hostname still has certificate files in the merged
    # volume. The archive preflight checked path existence, not certificate expiry.
    foreach ($hostname in $hostnames) {
        if ($hostname -notmatch '^[a-zA-Z0-9.-]+$') { throw 'Unexpected hostname in backed-up settings.' }
        $result = @(& docker run --rm --network none --mount "type=volume,source=$volumeData,target=/data,readonly" `
            ubuntu:24.04 bash -euo pipefail -c `
            'find /data/caddy/certificates -type f -path "*/$1/*" -name "$1.crt" -print -quit | grep -q .' `
            sparrow-tls-stage $hostname 2>&1)
        if ($LASTEXITCODE -ne 0) { throw "Staged data does not contain a readable Caddy certificate file for $hostname." }
    }
    $record = [PSCustomObject]@{
        FormatVersion=1
        State='TLS_STAGED_NOT_CUT_OVER'
        CreatedUtc=[DateTime]::UtcNow.ToString('o')
        SourceBackup=$backup
        SourceManifestSha256=(Get-FileHash -LiteralPath (Join-Path $backup 'manifest.json') -Algorithm SHA256).Hash
        SourceCaddyVolumes=@($legacy | ForEach-Object { $_.OriginalVolume })
        PublicHostnames=@($hostnames)
        SharedProxyProject='sparrow-public-proxy'
        StagedDataVolume=$volumeData
        StagedConfigVolume=$volumeConfig
        Notes='Staged data is NOT an active proxy; original Compose projects, data, /config and published ports are unchanged. TLS expiration and external HTTPS have not been verified.'
    }
    [System.IO.File]::WriteAllText((Join-Path $stage 'tls-stage.json'), ($record | ConvertTo-Json -Depth 5),
        [System.Text.UTF8Encoding]::new($false))
    Remove-Item -LiteralPath (Join-Path $stage 'INCOMPLETE-DO-NOT-CUTOVER.txt') -Force
    $complete = $true
    Write-Host "Offline TLS staging COMPLETE at $stage. Original projects, host ports and original volumes were not changed. THIS IS NOT A CUTOVER."
} finally {
    if (-not $complete) {
        foreach ($name in $created) {
            $ownershipProven = $false
            try {
                $existing = @(Get-SparrowDockerLabel -Resource volume -Id $name -Key 'sparrow.tls.stage')
                $ownershipProven = ($existing.Count -eq 1 -and $existing[0] -eq 'offline-import')
            } catch { $ownershipProven = $false }
            if (-not $ownershipProven) {
                Write-Warning "Cannot prove ownership of staged volume $name; will not automatically delete it."
                continue
            }
            $null = & docker volume rm $name 2>&1
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Failed to remove incomplete staged volume $name. Do not use it for cutover; it may contain private TLS keys."
            }
        }
    }
}
