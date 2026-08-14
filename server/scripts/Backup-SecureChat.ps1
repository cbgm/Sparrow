[CmdletBinding()]
param(
    [string]$OutputDirectory,

    [ValidateRange(0, 3650)]
    [int]$RetentionDays = 0,

    [switch]$MultiNode,

    [switch]$Production
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$serverDirectory = Split-Path -Parent $PSScriptRoot
$baseComposeFile = Join-Path $serverDirectory "docker-compose.yml"
$multiNodeComposeFile = Join-Path $serverDirectory "docker-compose.multinode.yml"
$productionComposeFile = Join-Path $serverDirectory "docker-compose.production.yml"
$productionEnvironmentFile = Join-Path $serverDirectory ".env.production"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $serverDirectory "backups"
} else {
    $OutputDirectory = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath(
        $OutputDirectory
    )
}

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

function Get-ComposeContainerId {
    param([Parameter(Mandatory = $true)][string]$Service)

    $containerId = Get-DockerOutput -CommandArguments ($composeArguments + @("ps", "-q", $Service))
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "Compose service is not running: $Service"
    }
    if ($containerId.Contains([Environment]::NewLine)) {
        throw "Compose returned more than one container for service: $Service"
    }
    return $containerId
}

function Assert-ContainerRunning {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerId,
        [Parameter(Mandatory = $true)][string]$Service
    )

    $inspect = @(Get-DockerOutput -CommandArguments @("inspect", $ContainerId) | ConvertFrom-Json)
    if ($inspect.Count -ne 1 -or -not $inspect[0].State.Running) {
        throw "Compose service is not running: $Service"
    }
}

function Get-LowercaseSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

$databaseDefinitions = @(
    [pscustomobject]@{
        Service = "node-registry-database"
        Database = "securechat_registry"
        User = "securechat_registry"
        File = "databases/node-registry.dump"
    },
    [pscustomobject]@{
        Service = "mailbox-database"
        Database = "securechat_mailbox"
        User = "securechat_mailbox"
        File = "databases/mailbox.dump"
    },
    [pscustomobject]@{
        Service = "push-database"
        Database = "securechat_push"
        User = "securechat_push"
        File = "databases/push.dump"
    },
    [pscustomobject]@{
        Service = "federation-database"
        Database = "securechat_federation"
        User = "securechat_federation"
        File = "databases/federation.dump"
    }
)

$identityDefinitions = @(
    [pscustomobject]@{
        Service = "node-registry"
        ContainerPath = "/data/registry.identity"
        File = "identities/registry.identity"
    },
    [pscustomobject]@{
        Service = "federation"
        ContainerPath = "/data/node.identity"
        File = "identities/node-a.identity"
    }
)

if ($MultiNode) {
    $databaseDefinitions += @(
        [pscustomobject]@{
            Service = "mailbox-b-database"
            Database = "securechat_mailbox_b"
            User = "securechat_mailbox_b"
            File = "databases/mailbox-b.dump"
        },
        [pscustomobject]@{
            Service = "federation-b-database"
            Database = "securechat_federation_b"
            User = "securechat_federation_b"
            File = "databases/federation-b.dump"
        }
    )
    $identityDefinitions += @(
        [pscustomobject]@{
            Service = "federation-b"
            ContainerPath = "/data/node.identity"
            File = "identities/node-b.identity"
        }
    )
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$backupTimestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssfffZ")
$archivePath = Join-Path $OutputDirectory "SecureChat-backup-$backupTimestamp.zip"
$stagingDirectory = Join-Path ([IO.Path]::GetTempPath()) "securechat-backup-$([Guid]::NewGuid())"
$databaseManifest = @()
$identityManifest = @()

New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $stagingDirectory "databases") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $stagingDirectory "identities") -Force | Out-Null

try {
    foreach ($database in $databaseDefinitions) {
        Write-Host "Backing up PostgreSQL service: $($database.Service)"
        $containerId = Get-ComposeContainerId -Service $database.Service
        Assert-ContainerRunning -ContainerId $containerId -Service $database.Service

        $containerDumpPath = "/tmp/securechat-backup-$([Guid]::NewGuid()).dump"
        $destinationPath = Join-Path $stagingDirectory $database.File
        try {
            Invoke-DockerCommand -CommandArguments @(
                "exec",
                $containerId,
                "pg_dump",
                "--format=custom",
                "--no-owner",
                "--no-acl",
                "--username=$($database.User)",
                "--dbname=$($database.Database)",
                "--file=$containerDumpPath"
            )
            & docker exec $containerId pg_restore --list $containerDumpPath 1>$null
            if ($LASTEXITCODE -ne 0) {
                throw "PostgreSQL produced an unreadable dump: $($database.Service)"
            }
            Invoke-DockerCommand -CommandArguments @(
                "cp",
                "${containerId}:$containerDumpPath",
                $destinationPath
            )
        } finally {
            & docker exec $containerId rm -f $containerDumpPath 2>$null
        }

        if (-not (Test-Path -LiteralPath $destinationPath -PathType Leaf) -or
            (Get-Item -LiteralPath $destinationPath).Length -eq 0) {
            throw "PostgreSQL backup is empty: $($database.Service)"
        }

        $databaseManifest += [ordered]@{
            service = $database.Service
            database = $database.Database
            user = $database.User
            file = $database.File
            sha256 = Get-LowercaseSha256 -Path $destinationPath
            postgresVersion = Get-DockerOutput -CommandArguments @(
                "exec",
                $containerId,
                "pg_dump",
                "--version"
            )
        }
    }

    foreach ($identity in $identityDefinitions) {
        Write-Host "Backing up identity: $($identity.Service)"
        $containerId = Get-ComposeContainerId -Service $identity.Service
        Assert-ContainerRunning -ContainerId $containerId -Service $identity.Service

        $destinationPath = Join-Path $stagingDirectory $identity.File
        Invoke-DockerCommand -CommandArguments @(
            "cp",
            "${containerId}:$($identity.ContainerPath)",
            $destinationPath
        )

        if (-not (Test-Path -LiteralPath $destinationPath -PathType Leaf) -or
            (Get-Item -LiteralPath $destinationPath).Length -eq 0) {
            throw "Identity backup is empty: $($identity.Service)"
        }

        $identityManifest += [ordered]@{
            service = $identity.Service
            containerPath = $identity.ContainerPath
            file = $identity.File
            sha256 = Get-LowercaseSha256 -Path $destinationPath
        }
    }

    $manifest = [ordered]@{
        formatVersion = 1
        createdAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        topology = if ($MultiNode) { "multi-node" } else { "single-node" }
        production = [bool]$Production
        databases = $databaseManifest
        identities = $identityManifest
        intentionallyExcluded = @(
            "Firebase Admin credentials",
            "Compose secret files and environment files",
            "Redis presence routes because clients refresh these short-lived routes"
        )
    }
    $manifestPath = Join-Path $stagingDirectory "manifest.json"
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

    Compress-Archive -Path (Join-Path $stagingDirectory "*") -DestinationPath $archivePath

    if ($RetentionDays -gt 0) {
        $retentionCutoff = (Get-Date).ToUniversalTime().AddDays(-$RetentionDays)
        Get-ChildItem -LiteralPath $OutputDirectory -Filter "SecureChat-backup-*.zip" -File |
            Where-Object {
                $_.FullName -ne $archivePath -and $_.LastWriteTimeUtc -lt $retentionCutoff
            } |
            ForEach-Object {
                Write-Host "Removing expired backup: $($_.FullName)"
                Remove-Item -LiteralPath $_.FullName -Force
            }
    }

    Write-Host "Backup created: $archivePath"
    Write-Host "SHA-256: $(Get-LowercaseSha256 -Path $archivePath)"
    Write-Host "Store the archive encrypted and separately back up server/secrets and Firebase credentials."
} finally {
    if (Test-Path -LiteralPath $stagingDirectory) {
        Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
    }
}
