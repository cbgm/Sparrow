[CmdletBinding()]
param(
    [string]$InstallationDirectory = $PSScriptRoot,
    [string]$Repository = '',
    [ValidateSet('Combined','Node','ControlPlane')][string]$Component = 'Combined'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$install = [IO.Path]::GetFullPath($InstallationDirectory).TrimEnd('\','/')
$stage = Join-Path ([IO.Path]::GetTempPath()) ('sparrow-github-update-' + [guid]::NewGuid().ToString('N'))

try {
    if (-not (Test-Path -LiteralPath (Join-Path $install 'Update-SparrowServerBundle.ps1') -PathType Leaf)) {
        throw 'The updater is missing from this installation. Install the latest manager patch in the ORIGINAL folder once.'
    }
    if (Test-Path -LiteralPath (Join-Path $install '.sparrow-attached.json') -PathType Leaf) {
        throw 'An attached legacy cutover cannot be updated by the fresh-install updater; the original deployment folders need a separate reviewed upgrade.'
    }
    # Fail before downloading when this is a newly extracted bundle pointed at
    # Docker volumes whose original credentials live somewhere else.
    $selected = if ($Component -eq 'Combined') { @('control-plane','community-node') } elseif ($Component -eq 'Node') { @('community-node') } else { @('control-plane') }
    foreach ($part in $selected) {
        if (-not (Test-Path -LiteralPath (Join-Path $install "$part/.env.runtime") -PathType Leaf) -or
            -not (Test-Path -LiteralPath (Join-Path $install "$part/sparrow.conf") -PathType Leaf)) {
            throw "No configured $part in this original folder. Update cannot regenerate missing runtime secrets or migrate an installation from another folder."
        }
    }
    $sourceFile = Join-Path $install 'bundle-source.json'
    if (-not $Repository -and (Test-Path -LiteralPath $sourceFile -PathType Leaf)) {
        $source = Get-Content -LiteralPath $sourceFile -Raw | ConvertFrom-Json
        $Repository = [string]$source.repository
    }
    if (-not $Repository -and $env:SPARROW_GITHUB_REPOSITORY) { $Repository = $env:SPARROW_GITHUB_REPOSITORY }
    # Historical pre-metadata Windows bundles used this GHCR namespace.
    if (-not $Repository) { $Repository = 'cbgm/sparrow' }
    if ($Repository -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$' -or $Repository.Contains('..')) {
        throw 'Invalid GitHub repository. Supply owner/repo through bundle-source.json or SPARROW_GITHUB_REPOSITORY.'
    }

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $headers = @{ 'User-Agent' = 'Sparrow-Server-Updater'; 'Accept' = 'application/vnd.github+json' }
    Write-Host "Checking the latest published unified server release in GitHub repository $Repository ..."
    try { $allReleases = @(Invoke-RestMethod -Uri "https://api.github.com/repos/$Repository/releases?per_page=100" -Headers $headers -ErrorAction Stop) }
    catch { throw "GitHub releases unavailable for $Repository. Publish a unified RELEASE (the old Actions artifact alone is not a public update URL). No local files or containers changed. $($_.Exception.Message)" }
    $serverReleases = @($allReleases | Where-Object {
        -not $_.draft -and -not $_.prerelease -and ([string]$_.tag_name) -match '^server-v[A-Za-z0-9_.-]+$' -and
        @($_.assets | Where-Object { $_.name -eq 'sparrow-server.zip' }).Count -eq 1 -and
        @($_.assets | Where-Object { $_.name -eq 'sparrow-server.zip.sha256' }).Count -eq 1
    } | Sort-Object -Property published_at -Descending)
    if ($serverReleases.Count -eq 0) { throw "No published server-v* release with unified ZIP and checksum in $Repository. No changes made." }
    $release = $serverReleases[0]
    $asset = @($release.assets | Where-Object { $_.name -eq 'sparrow-server.zip' })
    $checksumAsset = @($release.assets | Where-Object { $_.name -eq 'sparrow-server.zip.sha256' })
    if ($asset.Count -ne 1 -or $checksumAsset.Count -ne 1) { throw 'Latest release is missing the unified ZIP or its SHA-256 checksum. No changes made.' }
    foreach ($item in @($asset[0], $checksumAsset[0])) {
        $uri = [uri][string]$item.browser_download_url
        if ($uri.Scheme -ne 'https' -or $uri.Host -ne 'github.com' -or
            -not $uri.AbsolutePath.StartsWith("/$Repository/releases/download/", [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Release asset URL does not belong to the configured GitHub repository. No changes made.'
        }
    }
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    $zip = Join-Path $stage 'sparrow-server.zip'
    $shaFile = "$zip.sha256"
    Write-Host "Downloading unified release $($release.tag_name) directly from GitHub ..."
    Invoke-WebRequest -Uri $asset[0].browser_download_url -Headers $headers -OutFile $zip -UseBasicParsing -ErrorAction Stop
    Invoke-WebRequest -Uri $checksumAsset[0].browser_download_url -Headers $headers -OutFile $shaFile -UseBasicParsing -ErrorAction Stop
    $expected = (Get-Content -LiteralPath $shaFile -Raw).Trim()
    if ($expected -notmatch '^[A-Fa-f0-9]{64}$') { throw 'Malformed release checksum. No changes made.' }
    $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
    if ($actual -ine $expected) { throw 'Unified bundle checksum verification failed. No changes made.' }
    Write-Host "Verified GitHub release $($release.tag_name) / SHA-256 $actual."
    & (Join-Path $install 'Update-SparrowServerBundle.ps1') -BundleZip $zip -InstallationDirectory $install -ApplyImages -Component $Component
    if (-not $?) { throw 'In-place update failed. Check prior output; original data was not deliberately removed.' }
    Write-Host 'GitHub unified update completed. Reopen the existing manager in its ORIGINAL folder; verify Status and messaging.'
} catch {
    [Console]::Error.WriteLine("Sparrow GitHub update failed: $($_.Exception.Message)")
    exit 1
} finally {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue }
}
