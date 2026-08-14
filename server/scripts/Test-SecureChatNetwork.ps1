[CmdletBinding()]
param(
    [switch]$MultiNode,

    [switch]$Start,

    [switch]$BuildImages,

    [switch]$RequireFcm,

    [ValidateRange(1, 600)]
    [int]$TimeoutSeconds = 180,

    [ValidateRange(-1, 2147483647)]
    [int]$ExpectedNodes = -1,

    [ValidateRange(-1, 2147483647)]
    [int]$ExpectedMailboxCountA = -1,

    [ValidateRange(-1, 2147483647)]
    [int]$ExpectedMailboxCountB = -1,

    [ValidateRange(-1, 2147483647)]
    [int]$ExpectedPushDevices = -1
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$serverDirectory = Split-Path -Parent $PSScriptRoot
$baseComposeFile = Join-Path $serverDirectory "docker-compose.yml"
$multiNodeComposeFile = Join-Path $serverDirectory "docker-compose.multinode.yml"

$composeArguments = @("compose", "-f", $baseComposeFile)
if ($MultiNode) {
    $composeArguments += @("-f", $multiNodeComposeFile)
}

function Invoke-DockerCommand {
    param([Parameter(Mandatory = $true)][string[]]$CommandArguments)

    & docker @CommandArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($CommandArguments -join ' ')"
    }
}

function Get-DockerOutput {
    param([Parameter(Mandatory = $true)][string[]]$CommandArguments)

    $output = & docker @CommandArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($CommandArguments -join ' ')"
    }
    return (($output | Out-String).Trim())
}

function Invoke-ComposeCommand {
    param([Parameter(Mandatory = $true)][string[]]$CommandArguments)

    Invoke-DockerCommand -CommandArguments ($composeArguments + $CommandArguments)
}

function Get-ComposeContainerId {
    param([Parameter(Mandatory = $true)][string]$Service)

    return Get-DockerOutput -CommandArguments (
        $composeArguments + @("ps", "--all", "--quiet", $Service)
    )
}

function Write-ServiceDiagnostics {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [string]$ContainerId
    )

    if (-not [string]::IsNullOrWhiteSpace($ContainerId)) {
        $inspectOutput = (& docker inspect $ContainerId 2>$null | Out-String).Trim()
        if (-not [string]::IsNullOrWhiteSpace($inspectOutput)) {
            $inspect = @($inspectOutput | ConvertFrom-Json)
            if ($inspect.Count -eq 1 -and $null -ne $inspect[0].State.Health) {
                foreach ($entry in @($inspect[0].State.Health.Log | Select-Object -Last 3)) {
                    Write-Host "Health probe exit=$($entry.ExitCode): $(([string]$entry.Output).Trim())"
                }
            }
        }
    }
    & docker @($composeArguments + @("logs", "--tail", "80", $Service))
}

function Wait-ForHealthyService {
    param([Parameter(Mandatory = $true)][string]$Service)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastContainerId = ""
    while ((Get-Date) -lt $deadline) {
        $lastContainerId = Get-ComposeContainerId -Service $Service
        if (-not [string]::IsNullOrWhiteSpace($lastContainerId)) {
            $inspect = @(Get-DockerOutput -CommandArguments @("inspect", $lastContainerId) |
                ConvertFrom-Json)
            if ($inspect.Count -eq 1 -and $inspect[0].State.Running) {
                $health = $inspect[0].State.Health
                if ($null -eq $health -or $health.Status -eq "healthy") {
                    return
                }
                if ($health.Status -eq "unhealthy") {
                    Write-ServiceDiagnostics -Service $Service -ContainerId $lastContainerId
                    throw "Compose service is unhealthy: $Service"
                }
            }
        }
        Start-Sleep -Seconds 2
    }

    Write-ServiceDiagnostics -Service $Service -ContainerId $lastContainerId
    throw "Timed out waiting for healthy Compose service: $Service"
}

function Get-HealthText {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = $null
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                return ([string]$response.Content).Trim()
            }
            $lastError = "HTTP $($response.StatusCode)"
        } catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Health endpoint '$Name' did not become ready: $Url ($lastError)"
}

function Assert-Pattern {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value,
        [Parameter(Mandatory = $true)][string]$Pattern
    )

    if ($Value -notmatch $Pattern) {
        throw "Unexpected $Name response: $Value"
    }
}

function Assert-Count {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value,
        [Parameter(Mandatory = $true)][string]$Field,
        [Parameter(Mandatory = $true)][int]$Expected
    )

    if ($Expected -lt 0) {
        return
    }
    $pattern = "(?:^|\s)$([Regex]::Escape($Field))=(\d+)(?:\s|$)"
    if ($Value -notmatch $pattern) {
        throw "Response for '$Name' has no numeric '$Field' field: $Value"
    }
    $actual = [int]$Matches[1]
    if ($actual -ne $Expected) {
        throw "Unexpected $Field for '$Name': expected=$Expected actual=$actual"
    }
}

function Wait-ForExpectedCount {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Field,
        [Parameter(Mandatory = $true)][int]$Expected
    )

    if ($Expected -lt 0) {
        return Get-HealthText -Name $Name -Url $Url
    }

    $pattern = "(?:^|\s)$([Regex]::Escape($Field))=(\d+)(?:\s|$)"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastValue = ""
    while ((Get-Date) -lt $deadline) {
        $lastValue = Get-HealthText -Name $Name -Url $Url
        if ($lastValue -match $pattern -and [int]$Matches[1] -eq $Expected) {
            return $lastValue
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $Field=$Expected from '$Name'. Last response: $lastValue"
}

if ($BuildImages -and -not $Start) {
    throw "-BuildImages requires -Start."
}

if ($ExpectedNodes -lt 0) {
    $ExpectedNodes = if ($MultiNode) { 2 } else { 1 }
}

$services = @(
    "node-registry-database",
    "node-registry",
    "presence-redis",
    "presence-directory",
    "mailbox-database",
    "mailbox",
    "push-database",
    "push",
    "federation-database",
    "federation",
    "gateway"
)
if ($MultiNode) {
    $services += @(
        "mailbox-b-database",
        "mailbox-b",
        "federation-b-database",
        "federation-b",
        "gateway-b"
    )
}

if ($Start) {
    $upArguments = @("up", "-d", "--remove-orphans")
    if ($BuildImages) {
        $upArguments += "--build"
    }
    Invoke-ComposeCommand -CommandArguments $upArguments
}

Write-Host "Waiting for SecureChat Compose services."
foreach ($service in $services) {
    Wait-ForHealthyService -Service $service
}

$checks = @(
    [pscustomobject]@{
        Name = "node-registry"
        Url = "http://localhost:8090/health"
        Pattern = '^ok persistence=postgresql nodes=\d+$'
    },
    [pscustomobject]@{
        Name = "presence-directory"
        Url = "http://localhost:8091/health"
        Pattern = '^ok persistence=redis routes=\d+$'
    },
    [pscustomobject]@{
        Name = "mailbox"
        Url = "http://localhost:8092/health"
        Pattern = '^ok persistence=postgresql mailboxes=\d+$'
    },
    [pscustomobject]@{
        Name = "federation"
        Url = "http://localhost:8093/health"
        Pattern = '^ok persistence=postgresql pending=\d+$'
    },
    [pscustomobject]@{
        Name = "gateway"
        Url = "http://localhost:8094/health"
        Pattern = '^ok connections=\d+$'
    },
    [pscustomobject]@{
        Name = "push"
        Url = "http://localhost:8095/health"
        Pattern = '^ok fcmEnabled=(true|false) persistence=postgresql devices=\d+ pendingEnvelopes=\d+$'
    }
)
if ($MultiNode) {
    $checks += @(
        [pscustomobject]@{
            Name = "mailbox-b"
            Url = "http://localhost:8192/health"
            Pattern = '^ok persistence=postgresql mailboxes=\d+$'
        },
        [pscustomobject]@{
            Name = "federation-b"
            Url = "http://localhost:8193/health"
            Pattern = '^ok persistence=postgresql pending=\d+$'
        },
        [pscustomobject]@{
            Name = "gateway-b"
            Url = "http://localhost:8294/health"
            Pattern = '^ok connections=\d+$'
        }
    )
}

$results = @{}
foreach ($check in $checks) {
    $healthText = Get-HealthText -Name $check.Name -Url $check.Url
    Assert-Pattern -Name $check.Name -Value $healthText -Pattern $check.Pattern
    $results[$check.Name] = $healthText
    Write-Host "PASS $($check.Name): $healthText"
}

$results["node-registry"] = Wait-ForExpectedCount `
    -Name "node-registry" `
    -Url "http://localhost:8090/health" `
    -Field "nodes" `
    -Expected $ExpectedNodes
Write-Host "PASS expected registry count: nodes=$ExpectedNodes"
Assert-Count `
    -Name "mailbox" `
    -Value $results["mailbox"] `
    -Field "mailboxes" `
    -Expected $ExpectedMailboxCountA
if ($MultiNode) {
    Assert-Count `
        -Name "mailbox-b" `
        -Value $results["mailbox-b"] `
        -Field "mailboxes" `
        -Expected $ExpectedMailboxCountB
}
Assert-Count `
    -Name "push" `
    -Value $results["push"] `
    -Field "devices" `
    -Expected $ExpectedPushDevices

if ($RequireFcm -and $results["push"] -notmatch '(?:^|\s)fcmEnabled=true(?:\s|$)') {
    throw "Push service is healthy but Firebase Cloud Messaging is disabled: $($results['push'])"
}

$requestId = "securechat-smoke-$([Guid]::NewGuid().ToString('N'))"
$liveResponse = Invoke-WebRequest `
    -Uri "http://localhost:8094/health/live" `
    -Headers @{"X-Request-ID" = $requestId} `
    -UseBasicParsing `
    -TimeoutSec 5
if ($liveResponse.StatusCode -ne 200 -or $liveResponse.Headers["X-Request-ID"] -ne $requestId) {
    throw "Gateway did not preserve X-Request-ID."
}
Write-Host "PASS request ID propagation: $requestId"

$metricsResponse = Invoke-WebRequest `
    -Uri "http://localhost:8094/metrics" `
    -UseBasicParsing `
    -TimeoutSec 5
if ($metricsResponse.StatusCode -ne 200 -or $metricsResponse.Content -notmatch 'service="gateway"') {
    throw "Gateway metrics do not contain the expected service tag."
}
Write-Host "PASS gateway Prometheus metrics"

Write-Host "SecureChat network smoke test passed for $($services.Count) Compose services."
