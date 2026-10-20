[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('Release', 'Zip', 'Folder', 'Source')][string]$From,
    [string]$Path = '',
    [string]$InstallationDirectory = (Split-Path -Parent $PSScriptRoot),
    [string]$ResultPath = '',
    [string]$FailurePath = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# One entry point for every way of updating an installation:
#   Release  latest published GitHub release (checksum verified)
#   Zip      a sparrow-server.zip on disk (registry or local-image bundle)
#   Folder   an extracted bundle folder (used to migrate older installations)
#   Source   a Sparrow source checkout; its server images are built locally
# The bundle's own code installer copies the files, then the updated manager
# applies the new server images to the existing deployment.
$install = [IO.Path]::GetFullPath($InstallationDirectory).TrimEnd('\', '/')
$work = Join-Path ([IO.Path]::GetTempPath()) ('sparrow-update-' + [guid]::NewGuid().ToString('N'))
$powershell = (Get-Process -Id $PID).Path

# Child processes write progress to stderr. Under 'Stop', Windows PowerShell
# turns the first stderr line of a native command into a terminating error.
function Invoke-Child([string[]]$Arguments) {
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $powershell @Arguments 2>&1 | ForEach-Object { Write-Host ([string]$_) }
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
}
function Test-GeneratedSslipHostname([string]$Hostname, [string]$Prefix) {
    if ([string]::IsNullOrWhiteSpace($Hostname)) { return $false }
    return $Hostname.ToLowerInvariant() -match "^$([regex]::Escape($Prefix))-(\d{1,3}-){3}\d{1,3}\.sslip\.io$"
}
function Write-Result([string]$Value, [string]$Message = '') {
    if ($Message -and $FailurePath) { [IO.File]::WriteAllText($FailurePath, $Message, [Text.UTF8Encoding]::new($false)) }
    if ($ResultPath) { [IO.File]::WriteAllText($ResultPath, $Value, [Text.UTF8Encoding]::new($false)) }
}
function Read-Properties([string]$File) {
    $values = @{}
    if (Test-Path -LiteralPath $File -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $File) {
            if ($line -match '^([A-Z_][A-Z_0-9]*)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
        }
    }
    return $values
}
function Read-BundleInfo([string]$Root) {
    $file = Join-Path $Root 'app/bundle.json'
    if (Test-Path -LiteralPath $file -PathType Leaf) { return Get-Content -LiteralPath $file -Raw | ConvertFrom-Json }
    return $null
}
function Get-ReleaseRepository {
    $info = Read-BundleInfo $install
    if ($null -ne $info -and $info.repository) { return [string]$info.repository }
    $legacy = Join-Path $install 'bundle-source.json'
    if (Test-Path -LiteralPath $legacy -PathType Leaf) {
        $repository = [string](Get-Content -LiteralPath $legacy -Raw | ConvertFrom-Json).repository
        if ($repository) { return $repository }
    }
    if ($env:SPARROW_GITHUB_REPOSITORY) { return $env:SPARROW_GITHUB_REPOSITORY }
    return 'cbgm/sparrow'
}
function Get-LatestReleaseBundle([string]$Destination) {
    $repository = Get-ReleaseRepository
    if ($repository -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$' -or $repository.Contains('..')) {
        throw "Invalid GitHub repository '$repository'."
    }
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $headers = @{ 'User-Agent' = 'Sparrow-Server-Updater'; 'Accept' = 'application/vnd.github+json' }
    Write-Host "Looking up the latest Sparrow server release of $repository ..."
    try { $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$repository/releases?per_page=100" -Headers $headers }
    catch { throw "GitHub releases of $repository are unavailable: $($_.Exception.Message)" }
    # Windows PowerShell emits the JSON array as ONE pipeline object; unroll it.
    $releases = @($response | ForEach-Object { $_ })
    $suffix = if ($env:OS -eq 'Windows_NT') { '-windows.zip' } else { '-linux.zip' }
    $isBundle = {
        param($asset)
        ([string]$asset.name).StartsWith('sparrow-server-', [StringComparison]::OrdinalIgnoreCase) -and
            ([string]$asset.name).EndsWith($suffix, [StringComparison]::OrdinalIgnoreCase)
    }
    $release = @($releases | Where-Object {
        $candidate = $_
        -not $candidate.draft -and -not $candidate.prerelease -and
            ([string]$candidate.tag_name) -match '^(?:server-)?v[A-Za-z0-9_.-]+$' -and
            @($candidate.assets | Where-Object { & $isBundle $_ }).Count -eq 1 -and
            @($candidate.assets | Where-Object { $_.name -eq 'SHA256SUMS.txt' }).Count -eq 1
    } | Sort-Object -Property published_at -Descending | Select-Object -First 1)
    if ($release.Count -eq 0) { throw "No published release of $repository contains a *$suffix server bundle and SHA256SUMS.txt." }
    $release = $release[0]
    $asset = @($release.assets | Where-Object { & $isBundle $_ })[0]
    $checksums = @($release.assets | Where-Object { $_.name -eq 'SHA256SUMS.txt' })[0]
    foreach ($item in @($asset, $checksums)) {
        $uri = [uri][string]$item.browser_download_url
        if ($uri.Scheme -ne 'https' -or $uri.Host -ne 'github.com' -or
            -not $uri.AbsolutePath.StartsWith("/$repository/releases/download/", [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Release asset URL does not belong to the configured GitHub repository.'
        }
    }
    $zip = Join-Path $Destination ([string]$asset.name)
    $sumsFile = Join-Path $Destination 'SHA256SUMS.txt'
    Write-Host "Downloading $($asset.name) from release $($release.tag_name) ..."
    Invoke-WebRequest -Uri $asset.browser_download_url -Headers $headers -OutFile $zip -UseBasicParsing
    Invoke-WebRequest -Uri $checksums.browser_download_url -Headers $headers -OutFile $sumsFile -UseBasicParsing
    $expected = $null
    foreach ($line in Get-Content -LiteralPath $sumsFile) {
        if ($line -match '^([A-Fa-f0-9]{64})\s+\*?(.+)$' -and $Matches[2].Trim() -eq [string]$asset.name) {
            $expected = $Matches[1]
            break
        }
    }
    if (-not $expected) { throw 'SHA256SUMS.txt does not list the server bundle.' }
    $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
    if ($actual -ine $expected) { throw 'Downloaded server bundle failed checksum verification.' }
    Write-Host "Verified release $($release.tag_name) (SHA-256 $actual)."
    return $zip
}
function Get-Python {
    foreach ($candidate in @(@('python3'), @('python'), @('py', '-3'))) {
        $command = Get-Command $candidate[0] -ErrorAction SilentlyContinue
        if (-not $command) { continue }
        $arguments = @($candidate | Select-Object -Skip 1)
        # The Microsoft Store 'python' alias only prints an install hint on stderr.
        $previous = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & $command.Source @arguments -c 'import sys; sys.exit(0 if sys.version_info >= (3, 9) else 1)' 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) { return @($command.Source) + $arguments }
        } catch {
        } finally {
            $ErrorActionPreference = $previous
        }
    }
    throw 'Building from source needs Python 3.9 or newer (python.org). The installation was not changed.'
}
function Build-LocalBundle([string]$SourceDirectory, [string]$Destination) {
    $source = [IO.Path]::GetFullPath($SourceDirectory).TrimEnd('\', '/')
    $builder = Join-Path $source 'server/installer/build.py'
    if (-not (Test-Path -LiteralPath $builder -PathType Leaf)) {
        throw "$source is not a Sparrow source folder with the current server installer."
    }
    $python = @(Get-Python)
    Write-Host "Building server images from $source (this can take several minutes) ..."
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $pythonArguments = @($python | Select-Object -Skip 1) + @($builder, '--local-images', '--output', $Destination)
        & $python[0] @pythonArguments 2>&1 | ForEach-Object { Write-Host ([string]$_) }
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    if ($exitCode -ne 0) { throw "Local server build failed (exit $exitCode). The installation was not changed." }
    return (Join-Path $Destination 'sparrow-server.zip')
}
function Expand-Bundle([string]$Zip, [string]$Destination) {
    if (-not (Test-Path -LiteralPath $Zip -PathType Leaf)) { throw "Bundle not found: $Zip" }
    Write-Host 'Unpacking the server bundle ...'
    Expand-Archive -LiteralPath $Zip -DestinationPath $Destination -Force
    return $Destination
}

try {
    if (Test-Path -LiteralPath (Join-Path $install '.sparrow-attached.json') -PathType Leaf) {
        throw 'Attached deployments need a separate reviewed update procedure.'
    }
    $cp = Read-Properties (Join-Path $install 'control-plane/sparrow.conf')
    $node = Read-Properties (Join-Path $install 'community-node/sparrow.conf')
    if (-not $cp.Count -or -not $node.Count) { throw "No Sparrow server installation found at $install." }
    $installed = (Test-Path -LiteralPath (Join-Path $install 'control-plane/.env.runtime') -PathType Leaf) -and
        (Test-Path -LiteralPath (Join-Path $install 'community-node/.env.runtime') -PathType Leaf)
    if ($installed -and ($cp['MODE'] -notin @('lan', 'public') -or $cp['MODE'] -ne $node['MODE'])) {
        throw 'The installed components have inconsistent network modes.'
    }
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    $bundle = switch ($From) {
        'Release' { Expand-Bundle (Get-LatestReleaseBundle $work) (Join-Path $work 'bundle') }
        'Zip' { Expand-Bundle ([IO.Path]::GetFullPath($Path)) (Join-Path $work 'bundle') }
        'Source' { Expand-Bundle (Build-LocalBundle $Path (Join-Path $work 'build')) (Join-Path $work 'bundle') }
        'Folder' { [IO.Path]::GetFullPath($Path).TrimEnd('\', '/') }
    }
    $codeInstaller = Join-Path $bundle 'app/Update-SparrowServerBundle.ps1'
    $bundleInfo = Read-BundleInfo $bundle
    if ($null -eq $bundleInfo -or -not (Test-Path -LiteralPath $codeInstaller -PathType Leaf)) {
        throw 'This bundle uses an older layout and cannot be applied by this manager.'
    }
    & $codeInstaller -BundleDirectory $bundle -InstallationDirectory $install
    if ($From -eq 'Folder') {
        # Moving to a new manager must not change what the server runs.
        Write-Host 'Manager updated. The running server was not changed; use the Updates card for new server versions.'
        Write-Result 'SUCCESS'
        exit 0
    }
    if (-not $installed) {
        Write-Host 'Manager code updated. The server is not installed yet: click Install.'
        Write-Result 'SUCCESS'
        exit 0
    }

    # A new process runs the updated manager code against the existing data.
    $automatic = $cp['PUBLIC_HOSTNAME_MODE'] -eq 'CaddyAutomatic' -or
        ((Test-GeneratedSslipHostname ([string]$cp['PUBLIC_DOMAIN']) 'control') -and
            (Test-GeneratedSslipHostname ([string]$node['PUBLIC_DOMAIN']) 'node'))
    $hostnameMode = if ($cp['MODE'] -eq 'public' -and $automatic) { 'CaddyAutomatic' } else { 'Manual' }
    $directoryUrl = if ($cp['CONTROL_PLANE_DIRECTORY_URL']) { $cp['CONTROL_PLANE_DIRECTORY_URL'] } else { [string]$node['CONTROL_PLANE_DIRECTORY_URL'] }
    $engineResult = Join-Path $work 'engine-result.txt'
    $engineFailure = Join-Path $work 'engine-failure.txt'
    $arguments = @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
        '-File', (Join-Path $install 'app/Invoke-SparrowServer.ps1'),
        '-Action', 'Start', '-Component', 'Combined', '-Mode', $cp['MODE'],
        '-PublicHostnameMode', $hostnameMode, '-ResultPath', $engineResult, '-FailurePath', $engineFailure)
    # Windows PowerShell drops empty native arguments; omit unset values.
    $optional = [ordered]@{
        NodeDomain = [string]$node['PUBLIC_DOMAIN']; ControlPlaneDomain = [string]$cp['PUBLIC_DOMAIN']
        DirectoryUrl = $directoryUrl
        ImagePrefix = [string]$bundleInfo.imagePrefix; ImageTag = [string]$bundleInfo.imageTag
    }
    foreach ($entry in $optional.GetEnumerator()) {
        if (-not [string]::IsNullOrWhiteSpace($entry.Value)) { $arguments += @(('-' + $entry.Key), $entry.Value) }
    }
    Write-Host "Applying server images $($bundleInfo.imagePrefix)-*:$($bundleInfo.imageTag) to the existing installation ..."
    $exitCode = Invoke-Child $arguments
    if ($exitCode -ne 0 -or -not (Test-Path -LiteralPath $engineResult -PathType Leaf) -or
        ([IO.File]::ReadAllText($engineResult)).Trim() -ne 'SUCCESS') {
        $detail = if (Test-Path -LiteralPath $engineFailure -PathType Leaf) { ([IO.File]::ReadAllText($engineFailure)).Trim() } else { "exit $exitCode" }
        throw "The manager was updated, but applying the server images failed: $($detail.TrimEnd(".")). Existing data was not reset."
    }
    Write-Host 'Update complete.'
    Write-Result 'SUCCESS'
} catch {
    $message = $_.Exception.Message
    Write-Result 'FAILED' $message
    [Console]::Error.WriteLine($message)
    exit 1
} finally {
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
