# Public signed-directory client. A directory URL is never itself a trust anchor.
# Python 3 + cryptography are optional for local-only combined installation, but
# required for authenticated federation discovery on this manager revision.
function Get-SparrowVerifiedDirectoryUrls {
    param(
        [Parameter(Mandatory = $true)][string]$DirectoryUrl,
        [Parameter(Mandatory = $true)][string]$PinnedPublicKey,
        [Parameter(Mandatory = $true)][string]$CacheFile,
        [Parameter(Mandatory = $true)][string]$ClientScript
    )
    if (-not (Test-Path -LiteralPath $ClientScript -PathType Leaf)) {
        throw 'Bundled signed-directory client is missing.'
    }
    $python = Get-Command python3 -ErrorAction SilentlyContinue
    if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
    if (-not $python) {
        throw 'Python 3 with cryptography is required for signed directory discovery on Windows.'
    }
    # The helper verifies the pinned key, signature, version, revocations and
    # persistent snapshot BEFORE yielding any addresses. No unsigned fallback.
    $result = & $python.Source $ClientScript --url $DirectoryUrl --public-key $PinnedPublicKey --cache $CacheFile 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $result) {
        throw 'Directory refresh failed and no usable verified directory snapshot is available.'
    }
    $parsed = (($result -join "`n") | ConvertFrom-Json -ErrorAction Stop)
    if ($null -eq $parsed.controlPlanes -or $parsed.controlPlanes -isnot [array]) {
        throw 'Signed-directory helper returned invalid output.'
    }
    return @($parsed.controlPlanes)
}
