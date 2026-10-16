[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BundleZip,
    [Parameter(Mandatory = $true)][string]$InstallationDirectory,
    [Parameter(Mandatory = $true)][string]$ResultPath,
    [Parameter(Mandatory = $true)][string]$FailurePath
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Read-InstalledSettings([string]$Path) {
    $values = @{}
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -match '^([A-Z_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    return $values
}

try {
    $install = [IO.Path]::GetFullPath($InstallationDirectory)
    if (Test-Path -LiteralPath (Join-Path $install '.sparrow-attached.json') -PathType Leaf) {
        throw 'ZIP updates require the original combined installation, not an attached deployment.'
    }
    foreach ($part in @('control-plane', 'community-node')) {
        foreach ($file in @('sparrow.conf', '.env.runtime')) {
            if (-not (Test-Path -LiteralPath (Join-Path $install "$part/$file") -PathType Leaf)) {
                throw 'Select the existing combined server installation folder. No files were changed.'
            }
        }
    }
    $cp = Read-InstalledSettings (Join-Path $install 'control-plane/sparrow.conf')
    $node = Read-InstalledSettings (Join-Path $install 'community-node/sparrow.conf')
    if ($cp['MODE'] -notin @('lan', 'public') -or $cp['MODE'] -ne $node['MODE']) {
        throw 'The installed components have inconsistent network modes. No files were changed.'
    }
    & (Join-Path $PSScriptRoot 'Update-SparrowServerBundle.ps1') -BundleZip $BundleZip -InstallationDirectory $install
    if (-not $?) { throw 'The installer files could not be updated. Server images were not applied.' }

    # A separate process loads the updated manager; it cannot continue running old code.
    $hostnameMode = if ($cp['PUBLIC_HOSTNAME_MODE'] -eq 'CaddyAutomatic') { 'CaddyAutomatic' } else { 'Manual' }
    $directoryUrl = if ($cp['CONTROL_PLANE_DIRECTORY_URL']) { $cp['CONTROL_PLANE_DIRECTORY_URL'] } else { [string]$node['CONTROL_PLANE_DIRECTORY_URL'] }
    Write-Host 'Applying the update to the existing server ...'
    $startArguments = @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
        '-File', (Join-Path $install 'Invoke-SparrowServer.ps1'),
        '-Action', 'Start', '-Component', 'Combined', '-Mode', $cp['MODE'],
        '-PublicHostnameMode', $hostnameMode, '-ResultPath', $ResultPath, '-FailurePath', $FailurePath)
    $optionalArguments = @{
        NodeDomain = [string]$node['PUBLIC_DOMAIN']; ControlPlaneDomain = [string]$cp['PUBLIC_DOMAIN']
        DirectoryUrl = $directoryUrl
        ImagePrefix = [string]$cp['SPARROW_IMAGE_PREFIX']; ImageTag = [string]$cp['SPARROW_IMAGE_TAG']
    }
    # Windows PowerShell native invocation drops empty arguments; omit optional
    # values instead of leaving a switch without its value on LAN installations.
    foreach ($entry in $optionalArguments.GetEnumerator()) {
        if (-not [string]::IsNullOrWhiteSpace($entry.Value)) {
            $startArguments += @(('-' + $entry.Key), $entry.Value)
        }
    }
    & powershell.exe @startArguments
    if ($LASTEXITCODE -ne 0) { throw "Server update failed (exit $LASTEXITCODE). See the operation log; existing data was not reset." }
    [IO.File]::WriteAllText($ResultPath, 'SUCCESS', [Text.UTF8Encoding]::new($false))
} catch {
    $message = $_.Exception.Message
    [IO.File]::WriteAllText($FailurePath, $message, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText($ResultPath, 'FAILED', [Text.UTF8Encoding]::new($false))
    [Console]::Error.WriteLine($message)
    exit 1
}
