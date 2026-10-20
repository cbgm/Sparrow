$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$source = Join-Path $PSScriptRoot '../app/Invoke-SparrowServer.ps1'
$tokens = $null
$errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile($source, [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) { throw ($errors | Out-String) }
$definition = $ast.Find({ param($node)
    $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Get-PreservedDirectoryRoute'
}, $true)
if ($null -eq $definition) { throw 'Directory route preservation function is missing.' }
# Load only the pure function, never run the deployment manager or Docker.
. ([scriptblock]::Create($definition.Extent.Text))

function Assert-Throws([scriptblock]$Action) {
    $failed = $false
    try { & $Action } catch { $failed = $true }
    if (-not $failed) { throw 'Expected ambiguous or incomplete route markers to be rejected.' }
}

$start = '# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE'
$end = '# END SPARROW INDEPENDENT DIRECTORY ROUTE'
$route = @"
$start
directory.example.org {
    reverse_proxy sparrow-central-directory:9080
}
$end
"@
if ((Get-PreservedDirectoryRoute "control.example.org { }`n$route`nnode.example.org { }") -cne $route) {
    throw 'The independent Directory site was not preserved verbatim.'
}
if ((Get-PreservedDirectoryRoute 'control.example.org { }') -cne '') {
    throw 'No Directory site should be invented for an installation without one.'
}
Assert-Throws { Get-PreservedDirectoryRoute $start }
Assert-Throws { Get-PreservedDirectoryRoute $end }
Assert-Throws { Get-PreservedDirectoryRoute "$end`n$start" }
Assert-Throws { Get-PreservedDirectoryRoute "$route`n$route" }
Write-Host 'Directory route preservation checks passed.'
