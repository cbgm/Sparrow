# Read Docker labels as JSON rather than passing quoted Go-template keys through
# Windows PowerShell 5.1 native argument marshalling. Docker otherwise receives
# {{index .Labels com.docker.compose.project}} and reports function "com" not defined.
# Inspection only: no Docker object or deployment file is changed.
function Get-SparrowDockerLabel {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory=$true)][ValidateSet('container','volume')][string]$Resource,
        [Parameter(Mandatory=$true)][string]$Id,
        [Parameter(Mandatory=$true)][string]$Key
    )
    if ($Id -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]*$') {
        throw 'Invalid Docker resource identifier for label inspection.'
    }
    $template = if ($Resource -eq 'container') { '{{json .Config.Labels}}' } else { '{{json .Labels}}' }
    $raw = @(& docker $Resource inspect $Id --format $template 2>$null)
    if ($LASTEXITCODE -ne 0 -or $raw.Count -ne 1 -or [string]::IsNullOrWhiteSpace([string]$raw[0])) {
        throw "Could not inspect Docker $Resource labels for $Id; refusing to assume ownership."
    }
    try { $labels = ([string]$raw[0]) | ConvertFrom-Json -ErrorAction Stop }
    catch { throw "Invalid Docker $Resource label JSON for $Id; refusing to assume ownership." }
    if ($null -eq $labels) { return '' }
    $property = $labels.PSObject.Properties[$Key]
    if ($null -eq $property -or $null -eq $property.Value) { return '' }
    return [string]$property.Value
}
