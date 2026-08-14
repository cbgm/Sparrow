[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Archive,

    [switch]$MultiNode,

    [switch]$Production,

    [switch]$RecreateVolumes,

    [switch]$BuildImages,

    [switch]$ValidateOnly,

    [switch]$ConfirmDataLoss
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not $ValidateOnly -and -not $ConfirmDataLoss) {
    throw "Restore replaces SecureChat databases and identities. Run again with -ConfirmDataLoss."
}

$archivePath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Archive)
if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
    throw "Backup archive does not exist: $archivePath"
}

$serverDirectory = Split-Path -Parent $PSScriptRoot
$baseComposeFile = Join-Path $serverDirectory "docker-compose.yml"
$multiNodeComposeFile = Join-Path $serverDirectory "docker-compose.multinode.yml"
$productionComposeFile = Join-Path $serverDirectory "docker-compose.production.yml"
$productionEnvironmentFile = Join-Path $serverDirectory ".env.production"

if ($Production -and -not (Test-Path -LiteralPath $productionEnvironmentFile -PathType Leaf)) {
    throw "Production environment file does not exist: $productionEnvironmentFile"
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

$composeArguments = @("compose")
if ($Production) {
    $composeArguments += @("--env-file", $productionEnvironmentFile)
}
$composeArguments += @("-f", $baseComposeFile)
if ($MultiNode) {
    $composeArguments += @("-f", $multiNodeComposeFile)
}
if ($Production) {
    $composeArguments += @("-f", $productionComposeFile)
}

function Invoke-ComposeCommand {
    param([Parameter(Mandatory = $true)][string[]]$CommandArguments)

    Invoke-DockerCommand -CommandArguments ($composeArguments + $CommandArguments)
}

function Get-ComposeContainerId {
    param([Parameter(Mandatory = $true)][string]$Service)

    $containerId = Get-DockerOutput -CommandArguments (
        $composeArguments + @("ps", "--all", "--quiet", $Service)
    )
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "Compose container does not exist: $Service"
    }
    if ($containerId.Contains([Environment]::NewLine)) {
        throw "Compose returned more than one container for service: $Service"
    }
    return $containerId
}

function Wait-ForHealthyService {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [ValidateRange(1, 600)][int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $containerId = Get-DockerOutput -CommandArguments (
            $composeArguments + @("ps", "--quiet", $Service)
        )
        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $inspect = @(Get-DockerOutput -CommandArguments @("inspect", $containerId) | ConvertFrom-Json)
            if ($inspect.Count -eq 1 -and $inspect[0].State.Running) {
                $health = $inspect[0].State.Health
                if ($null -eq $health -or $health.Status -eq "healthy") {
                    return
                }
            }
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for healthy Compose service: $Service"
}

function Write-ComposeFailureDiagnostics {
    param([Parameter(Mandatory = $true)][string[]]$Services)

    Write-Warning "SecureChat service startup failed. Compose state and failing health checks follow."
    & docker @($composeArguments + @("ps", "--all"))

    foreach ($service in $Services) {
        $containerId = (& docker @($composeArguments + @("ps", "--all", "--quiet", $service)) |
            Out-String).Trim()
        if ([string]::IsNullOrWhiteSpace($containerId)) {
            continue
        }

        $inspectOutput = (& docker inspect $containerId 2>$null | Out-String).Trim()
        if ([string]::IsNullOrWhiteSpace($inspectOutput)) {
            continue
        }

        $inspect = @($inspectOutput | ConvertFrom-Json)
        if ($inspect.Count -ne 1) {
            continue
        }

        $state = $inspect[0].State
        $healthStatus = if ($null -eq $state.Health) { "none" } else { $state.Health.Status }
        if ($state.Running -and $healthStatus -in @("none", "healthy")) {
            continue
        }

        Write-Warning "Service '$service': status=$($state.Status), health=$healthStatus"
        if ($null -ne $state.Health) {
            foreach ($entry in @($state.Health.Log | Select-Object -Last 3)) {
                $probeOutput = ([string]$entry.Output).Trim()
                Write-Host "Health probe exit=$($entry.ExitCode): $probeOutput"
            }
        }
        & docker @($composeArguments + @("logs", "--tail", "80", $service))
    }

    Write-Warning (
        "If a probe reports that ReadinessProbe cannot be loaded, rebuild that service image " +
        "and recreate the service. You can also run this restore with -BuildImages."
    )
}

function Get-LowercaseSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Resolve-ArchiveEntry {
    param(
        [Parameter(Mandatory = $true)][string]$StagingDirectory,
        [Parameter(Mandatory = $true)][string]$RelativePath
    )

    $root = [IO.Path]::GetFullPath($StagingDirectory).TrimEnd([IO.Path]::DirectorySeparatorChar)
    $resolved = [IO.Path]::GetFullPath((Join-Path $root $RelativePath))
    $requiredPrefix = $root + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($requiredPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Backup manifest contains an invalid path: $RelativePath"
    }
    if (-not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
        throw "Backup archive entry is missing: $RelativePath"
    }
    return $resolved
}

$databaseDefinitions = @(
    [pscustomobject]@{
        Service = "node-registry-database"
        Database = "securechat_registry"
        User = "securechat_registry"
    },
    [pscustomobject]@{
        Service = "mailbox-database"
        Database = "securechat_mailbox"
        User = "securechat_mailbox"
    },
    [pscustomobject]@{
        Service = "push-database"
        Database = "securechat_push"
        User = "securechat_push"
    },
    [pscustomobject]@{
        Service = "federation-database"
        Database = "securechat_federation"
        User = "securechat_federation"
    }
)

$identityDefinitions = @(
    [pscustomobject]@{
        Service = "node-registry"
        ContainerPath = "/data/registry.identity"
    },
    [pscustomobject]@{
        Service = "federation"
        ContainerPath = "/data/node.identity"
    }
)

$applicationServices = @(
    "node-registry",
    "presence-directory",
    "mailbox",
    "push",
    "federation",
    "gateway"
)

if ($MultiNode) {
    $databaseDefinitions += @(
        [pscustomobject]@{
            Service = "mailbox-b-database"
            Database = "securechat_mailbox_b"
            User = "securechat_mailbox_b"
        },
        [pscustomobject]@{
            Service = "federation-b-database"
            Database = "securechat_federation_b"
            User = "securechat_federation_b"
        }
    )
    $identityDefinitions += @(
        [pscustomobject]@{
            Service = "federation-b"
            ContainerPath = "/data/node.identity"
        }
    )
    $applicationServices += @("mailbox-b", "federation-b", "gateway-b")
}

$stagingDirectory = Join-Path ([IO.Path]::GetTempPath()) "securechat-restore-$([Guid]::NewGuid())"
New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null

try {
    Expand-Archive -LiteralPath $archivePath -DestinationPath $stagingDirectory
    $manifestPath = Join-Path $stagingDirectory "manifest.json"
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Backup archive does not contain manifest.json"
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.formatVersion -ne 1) {
        throw "Unsupported backup format version: $($manifest.formatVersion)"
    }

    $requiredTopology = if ($MultiNode) { "multi-node" } else { "single-node" }
    if ($manifest.topology -ne $requiredTopology) {
        throw "Backup topology is '$($manifest.topology)'; restore requested '$requiredTopology'."
    }

    foreach ($entry in @($manifest.databases) + @($manifest.identities)) {
        $entryPath = Resolve-ArchiveEntry -StagingDirectory $stagingDirectory -RelativePath $entry.file
        $actualHash = Get-LowercaseSha256 -Path $entryPath
        if ($actualHash -ne $entry.sha256) {
            throw "Backup checksum mismatch: $($entry.file)"
        }
    }

    $manifestDatabaseServices = @($manifest.databases | ForEach-Object { $_.service } | Sort-Object)
    $requiredDatabaseServices = @($databaseDefinitions | ForEach-Object { $_.Service } | Sort-Object)
    if (($manifestDatabaseServices -join "|") -ne ($requiredDatabaseServices -join "|")) {
        throw "Backup database set does not match the requested topology."
    }

    $manifestIdentityServices = @($manifest.identities | ForEach-Object { $_.service } | Sort-Object)
    $requiredIdentityServices = @($identityDefinitions | ForEach-Object { $_.Service } | Sort-Object)
    if (($manifestIdentityServices -join "|") -ne ($requiredIdentityServices -join "|")) {
        throw "Backup identity set does not match the requested topology."
    }

    if ($ValidateOnly) {
        Write-Host "Backup archive is valid: $archivePath"
        Write-Host "Topology: $($manifest.topology)"
        Write-Host "Created: $($manifest.createdAtUtc)"
        return
    }

    if ($RecreateVolumes) {
        Write-Host "Removing the current Compose containers and volumes."
        Invoke-ComposeCommand -CommandArguments @("down", "--volumes", "--remove-orphans")
    } else {
        Write-Host "Stopping SecureChat application services."
        Invoke-ComposeCommand -CommandArguments (@("stop") + $applicationServices)
    }

    $databaseServices = @($databaseDefinitions | ForEach-Object { $_.Service })
    Invoke-ComposeCommand -CommandArguments (@("up", "-d") + $databaseServices)
    foreach ($service in $databaseServices) {
        Wait-ForHealthyService -Service $service
    }

    foreach ($database in $databaseDefinitions) {
        Write-Host "Restoring PostgreSQL service: $($database.Service)"
        $manifestEntry = @($manifest.databases | Where-Object { $_.service -eq $database.Service })
        if ($manifestEntry.Count -ne 1) {
            throw "Backup has no unique database entry for: $($database.Service)"
        }

        $sourcePath = Resolve-ArchiveEntry `
            -StagingDirectory $stagingDirectory `
            -RelativePath $manifestEntry[0].file
        $containerId = Get-ComposeContainerId -Service $database.Service
        $containerDumpPath = "/tmp/securechat-restore-$([Guid]::NewGuid()).dump"
        try {
            Invoke-DockerCommand -CommandArguments @(
                "cp",
                $sourcePath,
                "${containerId}:$containerDumpPath"
            )
            Invoke-DockerCommand -CommandArguments @(
                "exec",
                $containerId,
                "dropdb",
                "--username=$($database.User)",
                "--force",
                "--if-exists",
                $database.Database
            )
            Invoke-DockerCommand -CommandArguments @(
                "exec",
                $containerId,
                "createdb",
                "--username=$($database.User)",
                "--owner=$($database.User)",
                $database.Database
            )
            Invoke-DockerCommand -CommandArguments @(
                "exec",
                $containerId,
                "pg_restore",
                "--username=$($database.User)",
                "--dbname=$($database.Database)",
                "--no-owner",
                "--no-acl",
                "--exit-on-error",
                $containerDumpPath
            )
        } finally {
            & docker exec $containerId rm -f $containerDumpPath 2>$null
        }
    }

    $identityServices = @($identityDefinitions | ForEach-Object { $_.Service } | Select-Object -Unique)
    Invoke-ComposeCommand -CommandArguments (@("create") + $identityServices)

    foreach ($identity in $identityDefinitions) {
        Write-Host "Restoring identity: $($identity.Service)"
        $manifestEntry = @($manifest.identities | Where-Object { $_.service -eq $identity.Service })
        if ($manifestEntry.Count -ne 1) {
            throw "Backup has no unique identity entry for: $($identity.Service)"
        }
        if ($manifestEntry[0].containerPath -ne $identity.ContainerPath) {
            throw "Backup identity destination is invalid for: $($identity.Service)"
        }

        $sourcePath = Resolve-ArchiveEntry `
            -StagingDirectory $stagingDirectory `
            -RelativePath $manifestEntry[0].file
        $containerId = Get-ComposeContainerId -Service $identity.Service
        Invoke-DockerCommand -CommandArguments @(
            "cp",
            $sourcePath,
            "${containerId}:$($identity.ContainerPath)"
        )

        $verificationPath = Join-Path $stagingDirectory "verify-$([Guid]::NewGuid()).identity"
        Invoke-DockerCommand -CommandArguments @(
            "cp",
            "${containerId}:$($identity.ContainerPath)",
            $verificationPath
        )
        if ((Get-LowercaseSha256 -Path $verificationPath) -ne $manifestEntry[0].sha256) {
            throw "Restored identity checksum mismatch: $($identity.Service)"
        }
        Remove-Item -LiteralPath $verificationPath -Force
    }

    if ($BuildImages) {
        Write-Host "Building SecureChat application images before startup."
        Invoke-ComposeCommand -CommandArguments (@("build") + $applicationServices)
    }

    try {
        Invoke-ComposeCommand -CommandArguments @("up", "-d")
        foreach ($service in $applicationServices) {
            Wait-ForHealthyService -Service $service
        }
    } catch {
        Write-ComposeFailureDiagnostics -Services $applicationServices
        throw
    }

    Write-Host "Restore completed and every application service is healthy."
    Write-Host "Redis presence routes were intentionally not restored; connected clients republish them."
} finally {
    if (Test-Path -LiteralPath $stagingDirectory) {
        Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
    }
}
