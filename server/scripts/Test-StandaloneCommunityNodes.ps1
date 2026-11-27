[CmdletBinding()]
param(
    [switch]$BuildImages,
    [switch]$KeepRunning,
    [ValidateRange(30, 900)]
    [int]$TimeoutSeconds = 300
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$serverDirectory = Split-Path -Parent $PSScriptRoot
$workDirectory = Join-Path $serverDirectory ".standalone-smoke"
$controlCompose = Join-Path $serverDirectory "control-plane/docker-compose.yml"
$nodeCompose = Join-Path $serverDirectory "community-node/docker-compose.yml"
$controlEnvironment = Join-Path $workDirectory "control-plane.env"
$nodeAEnvironment = Join-Path $workDirectory "node-a.env"
$nodeBEnvironment = Join-Path $workDirectory "node-b.env"

$controlProject = "sparrow-control-plane-smoke"
$nodeAProject = "sparrow-community-node-a-smoke"
$nodeBProject = "sparrow-community-node-b-smoke"

function Write-Utf8File {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$Lines
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, $Lines, $encoding)
}

function Invoke-Docker {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

function Get-DockerOutput {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
    return (($output | Out-String).Trim())
}

function Get-ComposeArguments {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile
    )

    return @(
        "compose",
        "--project-name", $Project,
        "--env-file", $EnvironmentFile,
        "-f", $ComposeFile
    )
}

function Invoke-Compose {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $composeArguments = Get-ComposeArguments $Project $EnvironmentFile $ComposeFile
    Invoke-Docker -Arguments ($composeArguments + $Arguments)
}

function Get-ComposeOutput {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $composeArguments = Get-ComposeArguments $Project $EnvironmentFile $ComposeFile
    return Get-DockerOutput -Arguments ($composeArguments + $Arguments)
}

function Convert-KeyValueOutput {
    param([Parameter(Mandatory = $true)][string]$Output)

    $values = @{}
    foreach ($line in @($Output -split "`r?`n")) {
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) {
            $values[$parts[0]] = $parts[1]
        }
    }
    return $values
}

function Get-NodeAuthentication {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile,
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Body = ""
    )

    $cliArguments = @(
        "exec", "-T", "federation",
        "java", "-cp", "/app/lib/*",
        "com.cbgm.sparrow.server.security.NodeRequestSignatureCli",
        "/data/node.identity", $Method, $Path
    )
    if (-not [string]::IsNullOrEmpty($Body)) {
        $cliArguments += $Body
    }
    $output =
        Get-ComposeOutput `
            -Project $Project `
            -EnvironmentFile $EnvironmentFile `
            -ComposeFile $ComposeFile `
            -Arguments $cliArguments
    $authentication = Convert-KeyValueOutput -Output $output
    foreach ($key in @("nodeId", "timestamp", "nonce", "signature")) {
        if ([string]::IsNullOrWhiteSpace([string]$authentication[$key])) {
            throw "Node authentication CLI did not return '$key': $output"
        }
    }
    return $authentication
}

function Get-CommunityNodeId {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile
    )

    $authentication =
        Get-NodeAuthentication `
            -Project $Project `
            -EnvironmentFile $EnvironmentFile `
            -ComposeFile $ComposeFile `
            -Method "GET" `
            -Path "/standalone-smoke/identity"
    return [string]$authentication["nodeId"]
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Pattern
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastResult = ""
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            $lastResult = ([string]$response.Content).Trim()
            if ($response.StatusCode -eq 200 -and $lastResult -match $Pattern) {
                Write-Host "PASS $Name`: $lastResult"
                return $lastResult
            }
        } catch {
            $lastResult = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }

    throw "Health endpoint did not become ready: $Name $Url ($lastResult)"
}

function Wait-ForRegistryNodes {
    param([Parameter(Mandatory = $true)][int]$Expected)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastResult = ""
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8391/health" -UseBasicParsing -TimeoutSec 5
            $lastResult = ([string]$response.Content).Trim()
            if (
                $response.StatusCode -eq 200 -and
                $lastResult -match '^ok persistence=postgresql nodes=(\d+)$' -and
                [int]$Matches[1] -eq $Expected
            ) {
                Write-Host "PASS registry contains $Expected independent nodes."
                return
            }
        } catch {
            $lastResult = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }

    throw "Registry did not reach nodes=$Expected within $TimeoutSeconds seconds. Last response: $lastResult"
}

function Get-RegisteredNodeIds {
    $response = Invoke-RestMethod -Uri "http://localhost:8390/v1/nodes" -TimeoutSec 10
    return @($response.directory.nodes | ForEach-Object { [string]$_.nodeId } | Sort-Object)
}

function Get-ProjectNetworks {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile
    )

    $containerIds = Get-ComposeOutput $Project $EnvironmentFile $ComposeFile @("ps", "-q")
    $networks = New-Object System.Collections.Generic.HashSet[string]
    foreach ($containerId in @($containerIds -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($containerId)) {
            continue
        }
        $inspect = Get-DockerOutput -Arguments @("inspect", $containerId) | ConvertFrom-Json
        foreach ($networkName in $inspect[0].NetworkSettings.Networks.PSObject.Properties.Name) {
            [void]$networks.Add($networkName)
        }
    }
    return @($networks)
}

function Get-ProjectVolumes {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile
    )

    $containerIds = Get-ComposeOutput $Project $EnvironmentFile $ComposeFile @("ps", "-q")
    $volumes = New-Object System.Collections.Generic.HashSet[string]
    foreach ($containerId in @($containerIds -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($containerId)) {
            continue
        }
        $inspect = Get-DockerOutput -Arguments @("inspect", $containerId) | ConvertFrom-Json
        foreach ($mount in @($inspect[0].Mounts)) {
            if ($mount.Type -eq "volume") {
                [void]$volumes.Add([string]$mount.Name)
            }
        }
    }
    return @($volumes)
}

function Assert-NoSharedNetworks {
    param(
        [Parameter(Mandatory = $true)][string[]]$First,
        [Parameter(Mandatory = $true)][string[]]$Second,
        [Parameter(Mandatory = $true)][string]$Description
    )

    $shared = @($First | Where-Object { $Second -contains $_ })
    if ($shared.Count -gt 0) {
        throw "$Description unexpectedly share Docker networks: $($shared -join ', ')"
    }
    Write-Host "PASS $Description share no Docker network."
}

function Assert-NoSharedVolumes {
    param(
        [Parameter(Mandatory = $true)][string[]]$First,
        [Parameter(Mandatory = $true)][string[]]$Second,
        [Parameter(Mandatory = $true)][string]$Description
    )

    $shared = @($First | Where-Object { $Second -contains $_ })
    if ($shared.Count -gt 0) {
        throw "$Description unexpectedly share Docker volumes: $($shared -join ', ')"
    }
    Write-Host "PASS $Description share no Docker volume."
}

function Stop-Project {
    param(
        [Parameter(Mandatory = $true)][string]$Project,
        [Parameter(Mandatory = $true)][string]$EnvironmentFile,
        [Parameter(Mandatory = $true)][string]$ComposeFile
    )

    $composeArguments = @(Get-ComposeArguments $Project $EnvironmentFile $ComposeFile)
    $dockerArguments = $composeArguments + @("down", "-v", "--remove-orphans")
    & docker @dockerArguments | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Could not clean up isolated smoke project '$Project'."
    }
}

New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null

Write-Utf8File -Path $controlEnvironment -Lines @(
    "CONTROL_PLANE_PROJECT_NAME=$controlProject",
    "CONTROL_PLANE_BIND_ADDRESS=0.0.0.0",
    "CONTROL_PLANE_HTTP_PORT=8390",
    "CONTROL_PLANE_SITE_ADDRESS=:80",
    "NODE_REGISTRY_DIAGNOSTIC_PORT=8391",
    "PRESENCE_DIRECTORY_DIAGNOSTIC_PORT=8392",
    "NODE_REGISTRY_DATABASE_PORT=5537",
    "PRESENCE_REDIS_PORT=6480",
    "NODE_REGISTRY_DATABASE_PASSWORD=standalone-smoke-registry",
    "PRESENCE_REDIS_PASSWORD=standalone-smoke-presence"
)

Write-Utf8File -Path $nodeAEnvironment -Lines @(
    "COMMUNITY_NODE_PROJECT_NAME=$nodeAProject",
    "COMMUNITY_NODE_DIRECTORY_CACHE_VOLUME=$nodeAProject-directory-cache",
    "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
    "COMMUNITY_NODE_HTTP_PORT=8490",
    "COMMUNITY_NODE_SITE_ADDRESS=:80",
    "CONTROL_PLANE_URL=http://host.docker.internal:8390",
    "CONTROL_PLANE_URLS=http://host.docker.internal:8390",
    "ADVERTISED_CONTROL_PLANE_URLS=http://localhost:8390",
    "CLIENT_ENDPOINT=ws://host.docker.internal:8490/v1/gateway",
    "MAILBOX_ENDPOINT=http://host.docker.internal:8490",
    "MAILBOX_DIAGNOSTIC_PORT=8492",
    "FEDERATION_DIAGNOSTIC_PORT=8493",
    "GATEWAY_DIAGNOSTIC_PORT=8494",
    "MAILBOX_DATABASE_PORT=5636",
    "MAILBOX_DATABASE_PASSWORD=standalone-smoke-mailbox-a"
)

Write-Utf8File -Path $nodeBEnvironment -Lines @(
    "COMMUNITY_NODE_PROJECT_NAME=$nodeBProject",
    "COMMUNITY_NODE_DIRECTORY_CACHE_VOLUME=$nodeBProject-directory-cache",
    "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
    "COMMUNITY_NODE_HTTP_PORT=8590",
    "COMMUNITY_NODE_SITE_ADDRESS=:80",
    "CONTROL_PLANE_URL=http://host.docker.internal:8390",
    "CONTROL_PLANE_URLS=http://host.docker.internal:8390",
    "ADVERTISED_CONTROL_PLANE_URLS=http://localhost:8390",
    "CLIENT_ENDPOINT=ws://host.docker.internal:8590/v1/gateway",
    "MAILBOX_ENDPOINT=http://host.docker.internal:8590",
    "MAILBOX_DIAGNOSTIC_PORT=8592",
    "FEDERATION_DIAGNOSTIC_PORT=8593",
    "GATEWAY_DIAGNOSTIC_PORT=8594",
    "MAILBOX_DATABASE_PORT=5736",
    "MAILBOX_DATABASE_PASSWORD=standalone-smoke-mailbox-b"
)

try {
    Stop-Project $nodeBProject $nodeBEnvironment $nodeCompose
    Stop-Project $nodeAProject $nodeAEnvironment $nodeCompose
    Stop-Project $controlProject $controlEnvironment $controlCompose

    $upArguments = @("up", "-d", "--remove-orphans")
    if ($BuildImages) {
        $upArguments += "--build"
    }

    Invoke-Compose $controlProject $controlEnvironment $controlCompose $upArguments
    Wait-ForHealth `
        -Name "control-plane registry" `
        -Url "http://localhost:8391/health" `
        -Pattern '^ok persistence=postgresql nodes=\d+$'
    Wait-ForHealth `
        -Name "control-plane presence" `
        -Url "http://localhost:8392/health" `
        -Pattern '^ok persistence=redis$'

    Invoke-Compose $nodeAProject $nodeAEnvironment $nodeCompose $upArguments
    $nodeBUpArguments = @("up", "-d", "--remove-orphans")
    Invoke-Compose $nodeBProject $nodeBEnvironment $nodeCompose $nodeBUpArguments

    Wait-ForHealth `
        -Name "node A mailbox" `
        -Url "http://localhost:8492/health" `
        -Pattern '^ok persistence=postgresql mailboxes=\d+$'
    Wait-ForHealth `
        -Name "node A federation" `
        -Url "http://localhost:8493/health" `
        -Pattern '^ok$'
    Wait-ForHealth `
        -Name "node A gateway" `
        -Url "http://localhost:8494/health" `
        -Pattern '^ok$'
    Wait-ForHealth `
        -Name "node B mailbox" `
        -Url "http://localhost:8592/health" `
        -Pattern '^ok persistence=postgresql mailboxes=\d+$'
    Wait-ForHealth `
        -Name "node B federation" `
        -Url "http://localhost:8593/health" `
        -Pattern '^ok$'
    Wait-ForHealth `
        -Name "node B gateway" `
        -Url "http://localhost:8594/health" `
        -Pattern '^ok$'

    $advertisedControlPlanes =
        Invoke-RestMethod `
            -Uri "http://localhost:8490/v1/control-planes" `
            -Method Get `
            -TimeoutSec 10
    if ($advertisedControlPlanes.controlPlanes -notcontains "http://localhost:8390") {
        throw "Community node did not advertise its external control-plane address."
    }
    Write-Host "PASS node A advertises client-usable control-plane addresses."

    Wait-ForRegistryNodes -Expected 2

    $nodeAId =
        Get-CommunityNodeId `
            -Project $nodeAProject `
            -EnvironmentFile $nodeAEnvironment `
            -ComposeFile $nodeCompose
    $nodeBId =
        Get-CommunityNodeId `
            -Project $nodeBProject `
            -EnvironmentFile $nodeBEnvironment `
            -ComposeFile $nodeCompose
    if ($nodeAId -eq $nodeBId) {
        throw "Community nodes unexpectedly share the same node identity: $nodeAId"
    }

    $initialNodeIds = Get-RegisteredNodeIds
    $expectedNodeIds = @($nodeAId, $nodeBId) | Sort-Object
    if (($initialNodeIds -join "|") -ne ($expectedNodeIds -join "|")) {
        throw "Registry identities differ from the running nodes: $($initialNodeIds -join ', ')"
    }
    Write-Host "PASS registry contains the two independent node identities."

    $controlNetworks = Get-ProjectNetworks $controlProject $controlEnvironment $controlCompose
    $nodeANetworks = Get-ProjectNetworks $nodeAProject $nodeAEnvironment $nodeCompose
    $nodeBNetworks = Get-ProjectNetworks $nodeBProject $nodeBEnvironment $nodeCompose
    Assert-NoSharedNetworks $controlNetworks $nodeANetworks "Control plane and node A"
    Assert-NoSharedNetworks $controlNetworks $nodeBNetworks "Control plane and node B"
    Assert-NoSharedNetworks $nodeANetworks $nodeBNetworks "Node A and node B"

    $controlVolumes = Get-ProjectVolumes $controlProject $controlEnvironment $controlCompose
    $nodeAVolumes = Get-ProjectVolumes $nodeAProject $nodeAEnvironment $nodeCompose
    $nodeBVolumes = Get-ProjectVolumes $nodeBProject $nodeBEnvironment $nodeCompose
    Assert-NoSharedVolumes $controlVolumes $nodeAVolumes "Control plane and node A"
    Assert-NoSharedVolumes $controlVolumes $nodeBVolumes "Control plane and node B"
    Assert-NoSharedVolumes $nodeAVolumes $nodeBVolumes "Node A and node B"

    Invoke-Compose `
        -Project $nodeAProject `
        -EnvironmentFile $nodeAEnvironment `
        -ComposeFile $nodeCompose `
        -Arguments @("restart", "mailbox", "federation", "gateway")
    Wait-ForHealth `
        -Name "restarted node A federation" `
        -Url "http://localhost:8493/health" `
        -Pattern '^ok$'
    Wait-ForRegistryNodes -Expected 2
    $restartedNodeAId =
        Get-CommunityNodeId `
            -Project $nodeAProject `
            -EnvironmentFile $nodeAEnvironment `
            -ComposeFile $nodeCompose
    if ($restartedNodeAId -ne $nodeAId) {
        throw "Node A identity changed after restart. Before=$nodeAId After=$restartedNodeAId"
    }
    Write-Host "PASS node identity persisted across a full node-service restart."

    Write-Host "Standalone community-node smoke test passed."
} catch {
    Write-Warning "Standalone community-node smoke test failed: $_"
    foreach ($projectSpec in @(
        @{ Name = $controlProject; Env = $controlEnvironment; Compose = $controlCompose },
        @{ Name = $nodeAProject; Env = $nodeAEnvironment; Compose = $nodeCompose },
        @{ Name = $nodeBProject; Env = $nodeBEnvironment; Compose = $nodeCompose }
    )) {
        $composeArgs = @(Get-ComposeArguments $projectSpec.Name $projectSpec.Env $projectSpec.Compose)
        Write-Warning "Container status for $($projectSpec.Name):"
        try {
            $psArguments = $composeArgs + @("ps", "--all")
            & docker @psArguments | Out-Host
            Write-Warning "Recent container logs for $($projectSpec.Name):"
            $logArguments = $composeArgs + @("logs", "--no-color", "--tail", "60")
            & docker @logArguments 2>&1 | Out-Host
        } catch {
            Write-Warning "Could not retrieve diagnostics for $($projectSpec.Name): $_"
        }
    }
    throw
} finally {
    if (-not $KeepRunning) {
        foreach ($projectSpec in @(
            @{ Name = $nodeBProject; Env = $nodeBEnvironment; Compose = $nodeCompose },
            @{ Name = $nodeAProject; Env = $nodeAEnvironment; Compose = $nodeCompose },
            @{ Name = $controlProject; Env = $controlEnvironment; Compose = $controlCompose }
        )) {
            try {
                Stop-Project $projectSpec.Name $projectSpec.Env $projectSpec.Compose
            } catch {
                Write-Warning "Cleanup failed for $($projectSpec.Name): $_"
            }
        }
    } else {
        Write-Host "Compose projects remain running because -KeepRunning was supplied."
    }
}
