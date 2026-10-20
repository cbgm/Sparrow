Set-StrictMode -Version Latest

function Read-SparrowLocalImages([string]$Directory) {
    $path = Join-Path $Directory 'local-images.json'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { return $null }
    $manifest = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    if ($manifest.version -ne 1 -or $manifest.archive -cne 'local-images.tar' -or
        $manifest.sha256 -notmatch '^[a-fA-F0-9]{64}$' -or
        $manifest.prefix -notmatch '^[a-z0-9.-]+(?:/[a-z0-9._-]+)+$' -or
        $manifest.tag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
        throw 'Invalid bundled local-image manifest.'
    }
    $expected = @('node-registry', 'presence-directory', 'mailbox', 'federation', 'gateway', 'push')
    $images = @($manifest.images)
    if ($images.Count -ne $expected.Count -or
        @($images.service | Sort-Object -Unique).Count -ne $expected.Count) {
        throw 'The local bundle must contain all six Sparrow server images.'
    }
    foreach ($image in $images) {
        $name = "$($manifest.prefix)-$($image.service):$($manifest.tag)"
        if ($image.service -notin $expected -or $image.name -cne $name -or
            $image.id -notmatch '^sha256:[a-f0-9]{64}$') {
            throw 'Invalid bundled local-image entry.'
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $Directory $manifest.archive) -PathType Leaf)) {
        throw 'Bundled local-images.tar is missing. No registry fallback will be used.'
    }
    return $manifest
}

function Import-SparrowLocalImages([string]$Directory, $Manifest) {
    $archive = Join-Path $Directory $Manifest.archive
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ine $Manifest.sha256) {
        throw 'Bundled image archive checksum mismatch. No server images were applied.'
    }
    Write-Host 'Loading the locally built Sparrow server images ...'
    Invoke-Docker -Arguments @('load', '--input', $archive)
    foreach ($image in $Manifest.images) {
        $id = @(& docker image inspect --format '{{.Id}}' $image.name)
        if ($LASTEXITCODE -ne 0 -or $id.Count -ne 1 -or $id[0] -cne $image.id) {
            throw "Loaded image does not match the bundle: $($image.name)"
        }
    }
}
