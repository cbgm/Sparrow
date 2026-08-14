[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$deploymentDirectory = $PSScriptRoot
$serverDirectory = Split-Path -Parent $deploymentDirectory
$projectRoot = Split-Path -Parent $serverDirectory
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$controlPlanePort = 8390
$script:DockerExecutable = $null
$script:DockerContext = $null

function Show-Result {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [Parameter(Mandatory = $true)]
        [bool]$IsError
    )

    try {
        Add-Type -AssemblyName PresentationFramework -ErrorAction Stop
        $icon =
            if ($IsError) {
                [System.Windows.MessageBoxImage]::Error
            } else {
                [System.Windows.MessageBoxImage]::Information
            }

        [System.Windows.MessageBox]::Show(
            $Message,
            "SecureChat Control Plane",
            [System.Windows.MessageBoxButton]::OK,
            $icon
        ) | Out-Null
    } catch {
        if ($IsError) {
            Write-Error $Message
        } else {
            Write-Host $Message
        }
    }
}

function Resolve-DockerExecutable {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\resources\bin\docker.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker\resources\bin\docker.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw "Docker Desktop is not installed."
}

function Reset-DockerEnvironment {
    Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_TLS_VERIFY -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_CERT_PATH -ErrorAction SilentlyContinue
}

function Get-AvailableDockerContexts {
    $output = & $script:DockerExecutable context ls --format "{{.Name}}" 2>$null
    if ($LASTEXITCODE -ne 0) {
        return @()
    }

    return @(
        $output |
            ForEach-Object { $_.Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    )
}

function Select-DockerContext {
    $contexts = Get-AvailableDockerContexts

    if ($contexts -contains "desktop-linux") {
        $script:DockerContext = "desktop-linux"
        return
    }

    if ($contexts -contains "default") {
        $script:DockerContext = "default"
        return
    }

    $script:DockerContext = $null
}

function Invoke-DockerRaw {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$IgnoreExitCode
    )

    $allArguments = @()
    if (-not [string]::IsNullOrWhiteSpace($script:DockerContext)) {
        $allArguments += @("--context", $script:DockerContext)
    }
    $allArguments += $Arguments

    $output = & $script:DockerExecutable @allArguments 2>&1
    $exitCode = $LASTEXITCODE

    if (-not $IgnoreExitCode -and $exitCode -ne 0) {
        $message = ($output | Out-String).Trim()
        if ([string]::IsNullOrWhiteSpace($message)) {
            $message = "Docker command failed."
        }
        throw $message
    }

    return [PSCustomObject]@{
        ExitCode = $exitCode
        Output = $output
    }
}

function Test-DockerEngine {
    $result = Invoke-DockerRaw `
        -Arguments @("version", "--format", "{{.Server.Version}}") `
        -IgnoreExitCode

    if ($result.ExitCode -ne 0) {
        return $false
    }

    $version = ($result.Output | Out-String).Trim()
    return -not [string]::IsNullOrWhiteSpace($version)
}

function Start-DockerDesktopApplication {
    $desktopCandidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker Desktop.exe")
    )

    $desktop = $desktopCandidates |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1

    if ($null -eq $desktop) {
        throw "Docker Desktop is installed incompletely or cannot be found."
    }

    $existing = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
    if ($null -eq $existing) {
        Start-Process -FilePath $desktop | Out-Null
    }
}

function Try-StartDockerWindowsService {
    $service = Get-Service -Name "com.docker.service" -ErrorAction SilentlyContinue
    if ($null -eq $service -or $service.Status -eq "Running") {
        return
    }

    try {
        Start-Service -Name "com.docker.service" -ErrorAction Stop
    } catch {
        # Docker Desktop can still start its backend without us manually
        # starting the Windows service, depending on the installation mode.
    }
}

function Ensure-DockerEngine {
    Reset-DockerEnvironment
    $script:DockerExecutable = Resolve-DockerExecutable
    Select-DockerContext

    if (Test-DockerEngine) {
        return
    }

    Try-StartDockerWindowsService
    Start-DockerDesktopApplication

    $deadline = [DateTime]::UtcNow.AddMinutes(5)

    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2

        # Docker Desktop may create the desktop-linux context only after
        # its backend has started, so refresh the context while waiting.
        Select-DockerContext

        if (Test-DockerEngine) {
            return
        }
    }

    $diagnostic = Invoke-DockerRaw `
        -Arguments @("version") `
        -IgnoreExitCode

    $details = ($diagnostic.Output | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($details)) {
        $details = "Docker Desktop did not expose a usable Docker API."
    }

    throw "Docker Desktop started, but its Docker engine did not become ready within 5 minutes.`n`n$details"
}

function Assert-ComposeVersion {
    $result = Invoke-DockerRaw `
        -Arguments @("compose", "version", "--short") `
        -IgnoreExitCode

    $versionOutput = ($result.Output | Out-String).Trim()

    if ($result.ExitCode -ne 0 -or $versionOutput -notmatch '(\d+\.\d+\.\d+)') {
        throw "Docker Compose is not available."
    }

    if ([Version]$Matches[1] -lt [Version]"2.24.4") {
        throw "Docker Compose 2.24.4 or newer is required."
    }
}

function Read-LocalProperty {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $path = Join-Path $projectRoot "local.properties"
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -le 0) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        if ($key -ne $Name) {
            continue
        }

        $value = $trimmed.Substring($separator + 1).Trim()
        $value = $value.Replace("\:", ":")
        $value = $value.Replace("\\", "\")
        return $value
    }

    return $null
}

function Resolve-FirebaseCredentials {
    $configured = Read-LocalProperty -Name "securechat.firebase.adminCredentials"
    if (-not [string]::IsNullOrWhiteSpace($configured)) {
        $expanded = [Environment]::ExpandEnvironmentVariables($configured)
        if (Test-Path -LiteralPath $expanded -PathType Leaf) {
            return (Resolve-Path -LiteralPath $expanded).Path
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($env:FIREBASE_ADMIN_CREDENTIALS)) {
        if (Test-Path -LiteralPath $env:FIREBASE_ADMIN_CREDENTIALS -PathType Leaf) {
            return (Resolve-Path -LiteralPath $env:FIREBASE_ADMIN_CREDENTIALS).Path
        }
    }

    $bundled = Join-Path $secretsDirectory "firebase-admin.json"
    if (Test-Path -LiteralPath $bundled -PathType Leaf) {
        return (Resolve-Path -LiteralPath $bundled).Path
    }

    $matches = @(
        Get-ChildItem `
            -LiteralPath $projectRoot `
            -Recurse `
            -File `
            -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Extension -eq ".json" -and
                (
                    $_.Name -match "(?i)firebase.*admin" -or
                    $_.Name -match "(?i)service[-_ ]?account"
                ) -and
                $_.FullName -notmatch "[\\/](build|\.gradle|\.git)[\\/]"
            }
    )

    if ($matches.Count -eq 1) {
        return $matches[0].FullName
    }

    throw "Firebase Admin credentials were not found. Keep securechat.firebase.adminCredentials in local.properties or place firebase-admin.json in server/control-plane/secrets."
}

function New-RandomSecret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Ensure-SecretFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-RandomSecret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function Read-SecretFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return (Get-Content -LiteralPath $Path -Raw).Trim()
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $result = Invoke-DockerRaw -Arguments $Arguments
    return $result.Output
}

function Get-LanIpv4Address {
    $addresses = @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4DefaultGateway -and
                $null -ne $_.IPv4Address
            } |
            ForEach-Object { $_.IPv4Address.IPAddress } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )

    if ($addresses.Count -gt 0) {
        return $addresses[0]
    }

    $fallback = @(
        Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
            Where-Object {
                $_.IPAddress -notlike "127.*" -and
                $_.IPAddress -notlike "169.254.*"
            } |
            Select-Object -ExpandProperty IPAddress
    )

    if ($fallback.Count -gt 0) {
        return $fallback[0]
    }

    return "127.0.0.1"
}

try {
    if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) {
        throw "server/control-plane/docker-compose.yml is missing."
    }

    Ensure-DockerEngine
    Assert-ComposeVersion

    New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null

    $secretsIgnore = Join-Path $secretsDirectory ".gitignore"
    if (-not (Test-Path -LiteralPath $secretsIgnore -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $secretsIgnore,
            "*`n!.gitignore`n",
            [System.Text.UTF8Encoding]::new($false)
        )
    }

    $registryPasswordPath = Join-Path $secretsDirectory "node-registry-database-password.txt"
    $presencePasswordPath = Join-Path $secretsDirectory "presence-redis-password.txt"
    $pushPasswordPath = Join-Path $secretsDirectory "push-database-password.txt"
    $pushTokenPath = Join-Path $secretsDirectory "push-internal-api-token.txt"

    @(
        $registryPasswordPath,
        $presencePasswordPath,
        $pushPasswordPath,
        $pushTokenPath
    ) | ForEach-Object { Ensure-SecretFile -Path $_ }

    $firebasePath = Resolve-FirebaseCredentials
    $firebaseComposePath = $firebasePath.Replace("\", "/")

    $runtimeEnvironment = @(
        "CONTROL_PLANE_PROJECT_NAME=securechat-control-plane",
        "CONTROL_PLANE_BIND_ADDRESS=0.0.0.0",
        "CONTROL_PLANE_HTTP_PORT=$controlPlanePort",
        "CONTROL_PLANE_SITE_ADDRESS=:80",
        "FIREBASE_ADMIN_CREDENTIALS=$firebaseComposePath",
        "NODE_REGISTRY_DATABASE_PASSWORD=$(Read-SecretFile -Path $registryPasswordPath)",
        "PRESENCE_REDIS_PASSWORD=$(Read-SecretFile -Path $presencePasswordPath)",
        "PUSH_DATABASE_PASSWORD=$(Read-SecretFile -Path $pushPasswordPath)",
        "PUSH_INTERNAL_API_TOKEN=$(Read-SecretFile -Path $pushTokenPath)"
    )

    [System.IO.File]::WriteAllLines(
        $runtimeEnvironmentPath,
        $runtimeEnvironment,
        [System.Text.UTF8Encoding]::new($false)
    )

    $composeArguments = @(
        "compose",
        "--env-file",
        $runtimeEnvironmentPath,
        "-f",
        $composePath
    )

    Push-Location $deploymentDirectory
    try {
        Invoke-Docker -Arguments ($composeArguments + @("config", "--quiet")) | Out-Null
        Invoke-Docker -Arguments (
            $composeArguments + @(
                "up",
                "-d",
                "--remove-orphans",
                "--build",
                "--wait",
                "--wait-timeout",
                "300"
            )
        ) | Out-Null
    } finally {
        Pop-Location
    }

    $lanAddress = Get-LanIpv4Address
    $controlPlaneUrl = "http://$lanAddress`:$controlPlanePort"

    [System.IO.File]::WriteAllText(
        (Join-Path $deploymentDirectory "control-plane.url"),
        $controlPlaneUrl,
        [System.Text.UTF8Encoding]::new($false)
    )

    Show-Result `
        -Message "SecureChat control plane is running.`n`n$controlPlaneUrl" `
        -IsError $false

    exit 0
} catch {
    Show-Result -Message $_.Exception.Message -IsError $true
    exit 1
}
