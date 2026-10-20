# Pure local-image validation tests. No Docker engine or Pester installation required.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../app/SparrowLocalImages.ps1')

function Assert-Throws([scriptblock]$Action) {
    $failed = $false
    try { & $Action } catch { $failed = $true }
    if (-not $failed) { throw 'Expected the operation to reject invalid local-image input.' }
}

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('sparrow-local-images-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
$script:loadCalls = 0
$script:inspectedId = 'sha256:' + ('a' * 64)
function Invoke-Docker([string[]]$Arguments) {
    if ($Arguments[0] -ne 'load' -or $Arguments[1] -ne '--input') { throw 'Unexpected Docker operation.' }
    $script:loadCalls++
}
function docker {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    if ($Arguments[0] -ne 'image' -or $Arguments[1] -ne 'inspect') { throw 'Unexpected Docker operation.' }
    $global:LASTEXITCODE = 0
    return $script:inspectedId
}

try {
    if ($null -ne (Read-SparrowLocalImages $testRoot)) { throw 'A normal bundle should not select local images.' }
    $archive = Join-Path $testRoot 'local-images.tar'
    [IO.File]::WriteAllText($archive, 'test image archive')
    $images = @('node-registry', 'presence-directory', 'mailbox', 'federation', 'gateway', 'push') | ForEach-Object {
        @{ service = $_; name = "local/sparrow-${_}:test"; id = $script:inspectedId }
    }
    $manifest = @{
        version = 1; archive = 'local-images.tar'
        prefix = 'local/sparrow'; tag = 'test'; images = @($images)
        sha256 = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    }
    $manifestFile = Join-Path $testRoot 'local-images.json'
    function Write-TestManifest {
        [IO.File]::WriteAllText($manifestFile, ($manifest | ConvertTo-Json -Depth 5))
    }
    Write-TestManifest
    $parsed = Read-SparrowLocalImages $testRoot
    Import-SparrowLocalImages $testRoot $parsed
    if ($script:loadCalls -ne 1) { throw 'A valid archive should be loaded exactly once.' }

    [IO.File]::WriteAllText($archive, 'tampered image archive')
    Assert-Throws { Import-SparrowLocalImages $testRoot $parsed }
    if ($script:loadCalls -ne 1) { throw 'A tampered archive must be rejected before Docker load.' }
    [IO.File]::WriteAllText($archive, 'test image archive')

    $script:inspectedId = 'sha256:' + ('b' * 64)
    Assert-Throws { Import-SparrowLocalImages $testRoot $parsed }

    $manifest.archive = '../outside.tar'
    Write-TestManifest
    Assert-Throws { Read-SparrowLocalImages $testRoot }
    $manifest.archive = 'local-images.tar'

    $originalImages = $manifest.images
    $manifest.images = @($originalImages[0], $originalImages[0]) + @($originalImages[2..5])
    Write-TestManifest
    Assert-Throws { Read-SparrowLocalImages $testRoot }
    $manifest.images = $originalImages

    $manifest.images[0].name = 'other/image:test'
    Write-TestManifest
    Assert-Throws { Read-SparrowLocalImages $testRoot }
    $manifest.images[0].name = 'local/sparrow-node-registry:test'
    Write-TestManifest

    Remove-Item -LiteralPath $archive -Force
    Assert-Throws { Read-SparrowLocalImages $testRoot }
    Write-Host 'Local-image validation tests passed.'
} finally {
    Remove-Item -LiteralPath $testRoot -Recurse -Force
}
