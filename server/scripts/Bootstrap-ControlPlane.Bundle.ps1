[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$networkConfigPath = Join-Path $deploymentDirectory "securechat.conf"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$releaseComposePath = Join-Path $deploymentDirectory "docker-compose.release.yml"
$productionComposePath = Join-Path $deploymentDirectory "docker-compose.production.yml"
$logPath = Join-Path $deploymentDirectory "bootstrap-control-plane.log"
$controlPlanePort = 8390

$script:Docker = $null
$script:ComposeFileArguments = @()

$form = New-Object System.Windows.Forms.Form
$form.Text = "SecureChat Control Plane"
$form.Size = New-Object System.Drawing.Size(560, 190)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(505, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting SecureChat control plane"
$form.Controls.Add($title)

$status = New-Object System.Windows.Forms.Label
$status.Location = New-Object System.Drawing.Point(24, 58)
$status.Size = New-Object System.Drawing.Size(505, 38)
$status.Text = "Preparing..."
$form.Controls.Add($status)

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(24, 108)
$progress.Size = New-Object System.Drawing.Size(505, 20)
$progress.Style = [System.Windows.Forms.ProgressBarStyle]::Marquee
$progress.MarqueeAnimationSpeed = 25
$form.Controls.Add($progress)

$form.Show()
[System.Windows.Forms.Application]::DoEvents()

function Write-Log {
    param([string]$Message)

    Add-Content `
        -LiteralPath $logPath `
        -Value "[$(Get-Date -Format o)] $Message" `
        -Encoding UTF8
}

function Set-Status {
    param([string]$Message)

    $status.Text = $Message
    Write-Log $Message
    [System.Windows.Forms.Application]::DoEvents()
}

function Fail {
    param([string]$Message)

    Write-Log "FAILED: $Message"

    try {
        Push-Location $deploymentDirectory
        try {
            Write-Log "----- docker compose ps -----"
            $composeFileArguments = $script:ComposeFileArguments
            (& $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                @composeFileArguments `
                ps --all 2>&1) | ForEach-Object { Write-Log $_.ToString() }

            Write-Log "----- docker compose logs -----"
            (& $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                @composeFileArguments `
                logs --tail 150 --no-color 2>&1) | ForEach-Object { Write-Log $_.ToString() }
        } finally {
            Pop-Location
        }
    } catch {
        Write-Log "Could not collect Docker diagnostics: $($_.Exception.Message)"
    }

    [System.Windows.Forms.MessageBox]::Show(
        "$Message`n`nDiagnostic log:`n$logPath",
        "SecureChat Control Plane",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null

    $form.Close()
    exit 1
}

function Read-EnvironmentFile {
    param([string]$Path)

    $values = @{}

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()

        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")

        if ($separator -gt 0) {
            $values[$trimmed.Substring(0, $separator).Trim()] =
                $trimmed.Substring($separator + 1).Trim()
        }
    }

    return $values
}

function Get-NetworkMode {
    param([hashtable]$Config)

    $mode = if ($Config.ContainsKey("MODE")) { $Config["MODE"].Trim().ToLowerInvariant() } else { "lan" }

    if ($mode -notin @("lan", "public")) {
        throw "securechat.conf MODE must be lan or public."
    }

    return $mode
}

function Get-PublicIpv4Address {
    foreach ($url in @("https://api.ipify.org", "https://checkip.amazonaws.com")) {
        try {
            $value = (Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5).ToString().Trim()
            $parsed = $null

            if (
                [System.Net.IPAddress]::TryParse($value, [ref]$parsed) -and
                $parsed.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork
            ) {
                return $value
            }
        } catch {
            Write-Log "Public IPv4 lookup failed at ${url}: $($_.Exception.Message)"
        }
    }

    throw "Could not detect the public IPv4 address. Set PUBLIC_DOMAIN in securechat.conf."
}

function Resolve-PublicDomain {
    param([hashtable]$Config)

    if ($Config.ContainsKey("PUBLIC_DOMAIN") -and -not [string]::IsNullOrWhiteSpace($Config["PUBLIC_DOMAIN"])) {
        return $Config["PUBLIC_DOMAIN"].Trim().TrimEnd(".")
    }

    $publicAddress = Get-PublicIpv4Address
    return "$($publicAddress.Replace('.', '-')).sslip.io"
}

function New-Secret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }

    return [Convert]::ToBase64String($bytes)
}

function Ensure-Secret {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-Secret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function Find-Docker {
    $command = Get-Command docker -ErrorAction SilentlyContinue

    if ($null -ne $command) {
        return $command.Source
    }

    $candidate = Join-Path $env:ProgramFiles "Docker\Docker\resources\bin\docker.exe"

    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
    }

    throw "Docker Desktop is not installed."
}

function Ensure-Docker {
    $docker = Find-Docker

    & $docker info *> $null

    if ($LASTEXITCODE -eq 0) {
        return $docker
    }

    $desktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"

    if (-not (Test-Path -LiteralPath $desktop -PathType Leaf)) {
        throw "Docker Desktop could not be found."
    }

    Start-Process -FilePath $desktop | Out-Null

    $deadline = [DateTime]::UtcNow.AddMinutes(5)

    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        [System.Windows.Forms.Application]::DoEvents()

        & $docker info *> $null

        if ($LASTEXITCODE -eq 0) {
            return $docker
        }
    }

    throw "Docker Desktop did not become ready."
}

function Find-FirebaseCredentials {
    $candidate = Join-Path $secretsDirectory "firebase-admin.json"

    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
    }

    throw "firebase-admin.json is missing from the secrets folder."
}

function Invoke-Compose {
    param([string[]]$Arguments)

    $composeFileArguments = $script:ComposeFileArguments
    & $script:Docker compose `
        --env-file $runtimeEnvironmentPath `
        @composeFileArguments `
        @Arguments

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose command failed: $($Arguments -join ' ')"
    }
}

function Wait-ForContainerRunning {
    param(
        [string]$Service,
        [int]$TimeoutSeconds = 60
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

    while ([DateTime]::UtcNow -lt $deadline) {
        $composeFileArguments = $script:ComposeFileArguments
        $containerId = (
            & $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                @composeFileArguments `
                ps -q $Service 2>$null |
                Select-Object -First 1
        )

        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $state = (
                & $script:Docker inspect `
                    --format "{{.State.Status}}" `
                    $containerId 2>$null |
                    Out-String
            ).Trim()

            if ($state -eq "running") {
                return
            }
        }

        Start-Sleep -Seconds 1
        [System.Windows.Forms.Application]::DoEvents()
    }

    throw "$Service did not start."
}

function Escape-SqlLiteral {
    param([string]$Value)

    return $Value.Replace("'", "''")
}

function Synchronize-PostgresPassword {
    param(
        [string]$Service,
        [string]$DatabaseUser,
        [string]$DatabaseName,
        [string]$Password
    )

    Set-Status "Synchronizing $Service credentials..."

    $escapedPassword = Escape-SqlLiteral -Value $Password
    $sql = "ALTER ROLE `"$DatabaseUser`" WITH PASSWORD '$escapedPassword';"

    $composeFileArguments = $script:ComposeFileArguments
    $output = & $script:Docker compose `
        --env-file $runtimeEnvironmentPath `
        @composeFileArguments `
        exec -T `
        $Service `
        psql `
        -v ON_ERROR_STOP=1 `
        -U $DatabaseUser `
        -d $DatabaseName `
        -c $sql 2>&1

    $exitCode = $LASTEXITCODE

    foreach ($line in $output) {
        Write-Log "$Service password sync: $line"
    }

    if ($exitCode -ne 0) {
        throw "Could not synchronize the password in $Service."
    }
}

function Test-HttpReady {
    param(
        [string]$Url,
        [int]$TimeoutMilliseconds = 1500
    )

    try {
        $request = [System.Net.HttpWebRequest]::Create($Url)
        $request.Method = "GET"
        $request.Timeout = $TimeoutMilliseconds
        $request.ReadWriteTimeout = $TimeoutMilliseconds
        $request.AllowAutoRedirect = $false

        $response = $request.GetResponse()

        try {
            return [int]$response.StatusCode -ge 200 -and
                [int]$response.StatusCode -lt 300
        } finally {
            $response.Close()
        }
    } catch {
        return $false
    }
}

function Wait-ForEndpoint {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds = 90
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

    while ([DateTime]::UtcNow -lt $deadline) {
        Set-Status "Waiting for $Name..."

        if (Test-HttpReady -Url $Url) {
            Write-Log "$Name ready: $Url"
            return
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name did not become ready: $Url"
}

try {
    Set-Content -LiteralPath $logPath -Value "" -Encoding UTF8

    foreach ($requiredFile in @(
        $networkConfigPath,
        $composePath,
        $releaseComposePath,
        $productionComposePath
    )) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "The deployment bundle is incomplete: $([System.IO.Path]::GetFileName($requiredFile)) is missing."
        }
    }

    $config = Read-EnvironmentFile -Path $networkConfigPath

    foreach ($requiredValue in @("SECURECHAT_IMAGE_PREFIX", "SECURECHAT_IMAGE_TAG")) {
        if (-not $config.ContainsKey($requiredValue) -or [string]::IsNullOrWhiteSpace($config[$requiredValue])) {
            throw "securechat.conf is missing $requiredValue."
        }
    }

    $mode = Get-NetworkMode -Config $config
    $publicDomain = if ($mode -eq "public") { Resolve-PublicDomain -Config $config } else { $null }
    $script:ComposeFileArguments = @("-f", $composePath, "-f", $releaseComposePath)

    if ($mode -eq "public") {
        $script:ComposeFileArguments += @("-f", $productionComposePath)
    }

    Set-Status "Starting Docker Desktop..."
    $script:Docker = Ensure-Docker

    Set-Status "Preparing SecureChat secrets..."
    New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null

    $registryPasswordPath = Join-Path $secretsDirectory "node-registry-database-password.txt"
    $presencePasswordPath = Join-Path $secretsDirectory "presence-redis-password.txt"
    $pushPasswordPath = Join-Path $secretsDirectory "push-database-password.txt"
    $pushTokenPath = Join-Path $secretsDirectory "push-internal-api-token.txt"

    Ensure-Secret $registryPasswordPath
    Ensure-Secret $presencePasswordPath
    Ensure-Secret $pushPasswordPath
    Ensure-Secret $pushTokenPath

    $registryPassword = (Get-Content $registryPasswordPath -Raw).Trim()
    $presencePassword = (Get-Content $presencePasswordPath -Raw).Trim()
    $pushPassword = (Get-Content $pushPasswordPath -Raw).Trim()
    $pushToken = (Get-Content $pushTokenPath -Raw).Trim()

    $firebaseCredentials = Find-FirebaseCredentials

    $siteAddress = if ($mode -eq "public") { $publicDomain } else { ":80" }

    $runtime = @(
        "CONTROL_PLANE_PROJECT_NAME=securechat-control-plane",
        "CONTROL_PLANE_BIND_ADDRESS=0.0.0.0",
        "CONTROL_PLANE_HTTP_PORT=$controlPlanePort",
        "CONTROL_PLANE_SITE_ADDRESS=$siteAddress",
        "CONTROL_PLANE_DOMAIN=$publicDomain",
        "FIREBASE_ADMIN_CREDENTIALS=$($firebaseCredentials.Replace('\','/'))",
        "NODE_REGISTRY_DATABASE_PASSWORD=$registryPassword",
        "PRESENCE_REDIS_PASSWORD=$presencePassword",
        "PUSH_DATABASE_PASSWORD=$pushPassword",
        "PUSH_INTERNAL_API_TOKEN=$pushToken",
        "NODE_REGISTRY_DATABASE_PASSWORD_FILE=./secrets/node-registry-database-password.txt",
        "PRESENCE_REDIS_PASSWORD_FILE=./secrets/presence-redis-password.txt",
        "PUSH_DATABASE_PASSWORD_FILE=./secrets/push-database-password.txt",
        "PUSH_INTERNAL_API_TOKEN_FILE=./secrets/push-internal-api-token.txt",
        "SECURECHAT_IMAGE_PREFIX=$($config['SECURECHAT_IMAGE_PREFIX'])",
        "SECURECHAT_IMAGE_TAG=$($config['SECURECHAT_IMAGE_TAG'])"
    )

    [System.IO.File]::WriteAllLines(
        $runtimeEnvironmentPath,
        $runtime,
        [System.Text.UTF8Encoding]::new($false)
    )

    Push-Location $deploymentDirectory

    try {
        Set-Status "Pulling SecureChat images..."
        Invoke-Compose -Arguments @("pull")

        # Start stateful dependencies first. PostgreSQL keeps its original
        # role passwords in existing volumes, so those roles are synchronized
        # below. Redis reads requirepass only when the Redis process starts,
        # therefore recreate only that container so it always consumes the
        # current presence-redis-password secret while preserving its volume.
        Set-Status "Starting SecureChat databases..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "node-registry-database",
            "push-database"
        )

        Set-Status "Synchronizing presence Redis credentials..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--force-recreate",
            "presence-redis"
        )

        Wait-ForContainerRunning -Service "node-registry-database"
        Wait-ForContainerRunning -Service "presence-redis"
        Wait-ForContainerRunning -Service "push-database"

        Start-Sleep -Seconds 3

        Synchronize-PostgresPassword `
            -Service "node-registry-database" `
            -DatabaseUser "securechat_registry" `
            -DatabaseName "securechat_registry" `
            -Password $registryPassword

        Synchronize-PostgresPassword `
            -Service "push-database" `
            -DatabaseUser "securechat_push" `
            -DatabaseName "securechat_push" `
            -Password $pushPassword

        Set-Status "Starting SecureChat services..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--remove-orphans"
        )

        Set-Status "Reloading control-plane routing..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--force-recreate",
            "caddy"
        )
    } finally {
        Pop-Location
    }

    Wait-ForEndpoint `
        -Name "control-plane registry route" `
        -Url "http://127.0.0.1:8390/health/registry"

    Wait-ForEndpoint `
        -Name "control-plane presence route" `
        -Url "http://127.0.0.1:8390/health/presence"

    Wait-ForEndpoint `
        -Name "control-plane push route" `
        -Url "http://127.0.0.1:8390/health/push"

    $address = @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4DefaultGateway -and
                $null -ne $_.IPv4Address
            } |
            ForEach-Object { $_.IPv4Address.IPAddress }
    ) | Select-Object -First 1

    if ([string]::IsNullOrWhiteSpace($address)) {
        $address = "localhost"
    }

    $url = if ($mode -eq "public") { "https://$publicDomain" } else { "http://$address`:$controlPlanePort" }

    $progress.Style = [System.Windows.Forms.ProgressBarStyle]::Blocks
    $progress.Value = 100
    $title.Text = "SecureChat control plane is running"
    $status.Text = $url
    Write-Log "SUCCESS: $url"

    [System.Windows.Forms.Application]::DoEvents()
    Start-Sleep -Seconds 3

    $form.Close()
    exit 0
} catch {
    Fail $_.Exception.Message
}
