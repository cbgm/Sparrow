$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$tokens = $null
$errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'Start-SparrowDirectory.ps1'), [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) { throw ($errors | Out-String) }
$function = $ast.Find({ param($node)
    $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Preserve-InstalledProxyRoute'
}, $true)
if ($null -eq $function) { throw 'Proxy compatibility function is missing.' }
# Load only the patch function: no GUI, containers, volumes, or keys are touched.
. ([scriptblock]::Create($function.Extent.Text))

function Test-LineEnding([string]$Newline) {
    $folder = Join-Path ([IO.Path]::GetTempPath()) ('sparrow-proxy-test-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    try {
        $manager = Join-Path $folder 'Invoke-SparrowServer.ps1'
        $fixture = @'
function Write-ProxyConfig {
    # Intentionally differs from the historical four-line text template.
    $output = Join-Path $proxyRoot 'Caddyfile'
    [IO.File]::WriteAllText($output, 'node.example.org { }')
}
'@
        [IO.File]::WriteAllText($manager, ($fixture.Replace("`r`n", "`n").Replace("`n", $Newline)))
        Preserve-InstalledProxyRoute $folder
        $patched = [IO.File]::ReadAllText($manager)
        Preserve-InstalledProxyRoute $folder
        if ([IO.File]::ReadAllText($manager) -cne $patched) { throw 'A second patch must be a no-op.' }
        . ([scriptblock]::Create($patched))
        $proxyRoot = $folder
        $route = "# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE`ndirectory.example.org { }`n# END SPARROW INDEPENDENT DIRECTORY ROUTE"
        $caddy = Join-Path $folder 'Caddyfile'
        [IO.File]::WriteAllText($caddy, "previous.example.org { }`n$route")
        Write-ProxyConfig
        $result = [IO.File]::ReadAllText($caddy)
        if (-not $result.Contains('node.example.org { }') -or -not $result.Contains($route)) {
            throw 'Regeneration must retain both updated server routes and the independent Directory site.'
        }
        $bad = '# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE'
        [IO.File]::WriteAllText($caddy, $bad)
        $failed = $false
        try { Write-ProxyConfig } catch { $failed = $true }
        if (-not $failed -or [IO.File]::ReadAllText($caddy) -cne $bad) {
            throw 'Incomplete markers must fail without changing the original Caddyfile.'
        }
        [IO.File]::WriteAllText($manager, 'function Different-Manager { }')
        $failed = $false
        try { Preserve-InstalledProxyRoute $folder } catch { $failed = $true }
        if (-not $failed -or [IO.File]::ReadAllText($manager) -cne 'function Different-Manager { }') {
            throw 'Unrecognized managers must be rejected without changing their source.'
        }
    } finally { Remove-Item -LiteralPath $folder -Recurse -Force }
}

Test-LineEnding "`n"
Test-LineEnding "`r`n"
Write-Host 'Directory proxy compatibility checks passed.'
