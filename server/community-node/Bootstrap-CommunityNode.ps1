[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$networkConfigPath = Join-Path $deploymentDirectory "securechat.conf"
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$releaseComposePath = Join-Path $deploymentDirectory "docker-compose.release.yml"
$productionComposePath = Join-Path $deploymentDirectory "docker-compose.production.yml"
$logPath = Join-Path $deploymentDirectory "bootstrap-community-node.log"

$publicPort = 8490
$script:Docker = $null
$script:ComposeFileArguments = @()

$form = New-Object System.Windows.Forms.Form
$form.Text = "SecureChat Community Node"
$form.Size = New-Object System.Drawing.Size(760, 430)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(705, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting SecureChat community node"
$form.Controls.Add($title)

$status = New-Object System.Windows.Forms.Label
$status.Location = New-Object System.Drawing.Point(24, 58)
$status.Size = New-Object System.Drawing.Size(705, 52)
$status.Text = "Preparing..."
$form.Controls.Add($status)

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(24, 116)
$progress.Size = New-Object System.Drawing.Size(705, 20)
$progress.Style = [System.Windows.Forms.ProgressBarStyle]::Marquee
$progress.MarqueeAnimationSpeed = 25
$form.Controls.Add($progress)

$details = New-Object System.Windows.Forms.TextBox
$details.Location = New-Object System.Drawing.Point(24, 154)
$details.Size = New-Object System.Drawing.Size(705, 220)
$details.Multiline = $true
$details.ReadOnly = $true
$details.ScrollBars = [System.Windows.Forms.ScrollBars]::Vertical
$details.WordWrap = $false
$details.Font = New-Object System.Drawing.Font("Consolas", 9)
$form.Controls.Add($details)

$form.Show()
[System.Windows.Forms.Application]::DoEvents()

function Write-Log {
    param([Parameter(Mandatory = $true)][string]$Message)

    Add-Content `
        -LiteralPath $logPath `
        -Value "[$(Get-Date -Format o)] $Message" `
        -Encoding UTF8
}

function Write-Detail {
    param([Parameter(Mandatory = $true)][string]$Message)

    if ([string]::IsNullOrWhiteSpace($Message)) {
        return
    }

    $details.AppendText("[$(Get-Date -Format HH:mm:ss)] $Message`r`n")
    $details.SelectionStart = $details.Text.Length
    $details.ScrollToCaret()
    [System.Windows.Forms.Application]::DoEvents()
}

function Set-LiveStatus {
    param([Parameter(Mandatory = $true)][string]$Message)

    $status.Text = $Message
    [System.Windows.Forms.Application]::DoEvents()
}

function Set-ProgressValue {
    param([Parameter(Mandatory = $true)][int]$Value)

    $progress.MarqueeAnimationSpeed = 0
    $progress.Style = [System.Windows.Forms.ProgressBarStyle]::Blocks
    $progress.Value = [Math]::Max(0, [Math]::Min(100, $Value))
    [System.Windows.Forms.Application]::DoEvents()
}

function Start-ProgressActivity {
    $progress.Value = 0
    $progress.Style = [System.Windows.Forms.ProgressBarStyle]::Marquee
    $progress.MarqueeAnimationSpeed = 20
    [System.Windows.Forms.Application]::DoEvents()
}

function Set-Status {
    param([Parameter(Mandatory = $true)][string]$Message)

    $status.Text = $Message
    Write-Log $Message
    Write-Detail $Message
    [System.Windows.Forms.Application]::DoEvents()
}

function Read-EnvironmentFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $values = @{}

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()

        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")

        if ($separator -gt 0) {
            $name = $trimmed.Substring(0, $separator).Trim()
            $value = $trimmed.Substring($separator + 1).Trim()
            $values[$name] = $value
        }
    }

    return $values
}

function Find-Docker {
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

function Test-DockerEngine {
    & $script:Docker info *> $null
    return $LASTEXITCODE -eq 0
}

function Ensure-Docker {
    $script:Docker = Find-Docker

    Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_TLS_VERIFY -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_CERT_PATH -ErrorAction SilentlyContinue
    Remove-Item Env:DOCKER_CONTEXT -ErrorAction SilentlyContinue

    if (Test-DockerEngine) {
        return
    }

    $desktopCandidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
        (Join-Path $env:LOCALAPPDATA "Docker\Docker Desktop.exe")
    )

    $desktop = $desktopCandidates |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1

    if ($null -eq $desktop) {
        throw "Docker Desktop could not be found."
    }

    if ($null -eq (Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath $desktop | Out-Null
    }

    $deadline = [DateTime]::UtcNow.AddMinutes(5)

    while ([DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 2
        [System.Windows.Forms.Application]::DoEvents()

        if (Test-DockerEngine) {
            return
        }
    }

    throw "Docker Desktop did not become ready."
}

function Assert-ComposeVersion {
    $versionOutput = (& $script:Docker compose version --short | Out-String).Trim()

    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch '(\d+\.\d+\.\d+)') {
        throw "Docker Compose is not available."
    }

    if ([Version]$Matches[1] -lt [Version]"2.24.4") {
        throw "Docker Compose 2.24.4 or newer is required."
    }
}

function Get-LocalIpv4Addresses {
    return @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4Address
            } |
            ForEach-Object {
                $_.IPv4Address.IPAddress
            } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )
}

function Get-PrimaryIpv4Address {
    $addresses = @(
        Get-NetIPConfiguration |
            Where-Object {
                $null -ne $_.IPv4DefaultGateway -and
                $null -ne $_.IPv4Address
            } |
            ForEach-Object {
                $_.IPv4Address.IPAddress
            } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*"
            }
    )

    if ($addresses.Count -gt 0) {
        return $addresses[0]
    }

    $fallback = Get-LocalIpv4Addresses

    if ($fallback.Count -gt 0) {
        return $fallback[0]
    }

    throw "No usable IPv4 address was found."
}

function Test-IsLocalHostAddress {
    param([Parameter(Mandatory = $true)][string]$HostName)

    if ($HostName -eq "localhost" -or $HostName -eq "127.0.0.1") {
        return $true
    }

    return (Get-LocalIpv4Addresses) -contains $HostName
}

function Test-ControlPlane {
    param([Parameter(Mandatory = $true)][string]$Url)

    try {
        $base = $Url.TrimEnd("/")
        $response = Invoke-RestMethod `
            -Uri "$base/v1/nodes" `
            -Method Get `
            -TimeoutSec 3

        return $null -ne $response
    } catch {
        return $false
    }
}

function Get-NetworkMode {
    param([Parameter(Mandatory = $true)][hashtable]$Config)

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
    param([Parameter(Mandatory = $true)][hashtable]$Config)

    if ($Config.ContainsKey("PUBLIC_DOMAIN") -and -not [string]::IsNullOrWhiteSpace($Config["PUBLIC_DOMAIN"])) {
        return $Config["PUBLIC_DOMAIN"].Trim().TrimEnd(".")
    }

    $publicAddress = Get-PublicIpv4Address
    return "$($publicAddress.Replace('.', '-')).sslip.io"
}

function Normalize-ControlPlaneUrl {
    param(
        [Parameter(Mandatory = $true)][string]$Value,
        [Parameter(Mandatory = $true)][string]$Mode
    )

    $candidate = $Value.Trim()

    if ([string]::IsNullOrWhiteSpace($candidate)) {
        return $null
    }

    if ($candidate -notmatch '^[A-Za-z][A-Za-z0-9+.-]*://') {
        $scheme = if ($Mode -eq "public") { "https" } else { "http" }
        $candidate = "${scheme}://$candidate"
    }

    $uri = $null
    if (-not [Uri]::TryCreate($candidate, [UriKind]::Absolute, [ref]$uri)) {
        throw "Invalid control-plane address in securechat.conf: $Value"
    }

    if ($uri.Scheme -notin @("http", "https")) {
        throw "Control-plane addresses must use HTTP or HTTPS: $Value"
    }

    if ($Mode -eq "public" -and $uri.Scheme -ne "https") {
        throw "Public mode requires HTTPS control-plane addresses: $Value"
    }

    return $candidate.TrimEnd("/")
}

function Resolve-ControlPlaneUrl {
    param([Parameter(Mandatory = $true)][string]$ConfiguredUrl)

    $uri = [Uri]$ConfiguredUrl
    $port = if ($uri.IsDefaultPort) { if ($uri.Scheme -eq "https") { 443 } else { 80 } } else { $uri.Port }

    if (Test-IsLocalHostAddress -HostName $uri.Host) {
        $hostProbeUrl = "$($uri.Scheme)://127.0.0.1`:$port"

        if (-not (Test-ControlPlane -Url $hostProbeUrl)) {
            throw "The local SecureChat control plane is not reachable at $hostProbeUrl."
        }

        $containerUrl = if ($uri.Scheme -eq "http") { "http://host.docker.internal`:$port" } else { $ConfiguredUrl }

        return [PSCustomObject]@{
            HostProbeUrl = $hostProbeUrl
            ContainerUrl = $containerUrl.TrimEnd("/")
        }
    }

    if (-not (Test-ControlPlane -Url $ConfiguredUrl)) {
        throw "The SecureChat control plane is not reachable at $ConfiguredUrl."
    }

    return [PSCustomObject]@{
        HostProbeUrl = $ConfiguredUrl.TrimEnd("/")
        ContainerUrl = $ConfiguredUrl.TrimEnd("/")
    }
}

function Resolve-ControlPlaneCandidates {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Config,
        [Parameter(Mandatory = $true)][string]$Mode
    )

    if (
        -not $Config.ContainsKey("CONTROL_PLANE_URLS") -or
        [string]::IsNullOrWhiteSpace($Config["CONTROL_PLANE_URLS"])
    ) {
        throw "securechat.conf is missing CONTROL_PLANE_URLS."
    }

    $failures = @()

    foreach ($rawCandidate in ($Config["CONTROL_PLANE_URLS"] -split '[,;]')) {
        $candidate = Normalize-ControlPlaneUrl -Value $rawCandidate -Mode $Mode
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }

        try {
            Set-Status "Checking control plane $candidate..."
            return Resolve-ControlPlaneUrl -ConfiguredUrl $candidate
        } catch {
            $failures += "$candidate - $($_.Exception.Message)"
            Write-Log "Control-plane candidate failed: $candidate"
        }
    }

    if ($failures.Count -eq 0) {
        throw "securechat.conf CONTROL_PLANE_URLS contains no usable addresses."
    }

    throw "No configured SecureChat control plane is reachable.`n$($failures -join "`n")"
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
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [System.IO.File]::WriteAllText(
            $Path,
            (New-RandomSecret),
            [System.Text.UTF8Encoding]::new($false)
        )
    }
}

function ConvertTo-ProcessArgument {
    param([Parameter(Mandatory = $true)][string]$Value)

    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    return '"' + $Value.Replace('"', '\"') + '"'
}

function Publish-ComposeOutput {
    param([string]$Line)

    if ([string]::IsNullOrWhiteSpace($Line)) {
        return $null
    }

    $clean = [regex]::Replace($Line, '\x1B\[[0-?]*[ -/]*[@-~]', '').Trim()

    if ([string]::IsNullOrWhiteSpace($clean)) {
        return $null
    }

    Write-Log "compose: $clean"
    Write-Detail $clean
    return $clean
}

function Invoke-ComposeStreaming {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Activity
    )

    $processArguments = @(
        "compose",
        "--progress",
        "plain",
        "--env-file",
        $runtimeEnvironmentPath
    )
    $processArguments += $script:ComposeFileArguments
    $processArguments += $Arguments

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $script:Docker
    $startInfo.WorkingDirectory = $deploymentDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.Arguments = (
        $processArguments |
            ForEach-Object { ConvertTo-ProcessArgument -Value $_ }
    ) -join " "

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo

    Write-Log "compose: $($processArguments -join ' ')"
    Write-Detail "$Activity started."
    Start-ProgressActivity

    if (-not $process.Start()) {
        throw "Could not start Docker Compose."
    }

    $startedAt = [DateTime]::UtcNow
    $stdoutTask = $process.StandardOutput.ReadLineAsync()
    $stderrTask = $process.StandardError.ReadLineAsync()
    $latestLine = ""

    try {
        while (
            -not $process.HasExited -or
            $null -ne $stdoutTask -or
            $null -ne $stderrTask
        ) {
            if ($null -ne $stdoutTask -and $stdoutTask.IsCompleted) {
                $line = $stdoutTask.Result

                if ($null -eq $line) {
                    $stdoutTask = $null
                } else {
                    $published = Publish-ComposeOutput -Line $line
                    if ($null -ne $published) {
                        $latestLine = $published
                    }
                    $stdoutTask = $process.StandardOutput.ReadLineAsync()
                }
            }

            if ($null -ne $stderrTask -and $stderrTask.IsCompleted) {
                $line = $stderrTask.Result

                if ($null -eq $line) {
                    $stderrTask = $null
                } else {
                    $published = Publish-ComposeOutput -Line $line
                    if ($null -ne $published) {
                        $latestLine = $published
                    }
                    $stderrTask = $process.StandardError.ReadLineAsync()
                }
            }

            $elapsed = [DateTime]::UtcNow - $startedAt
            $elapsedText = "{0:mm\:ss}" -f $elapsed

            if ([string]::IsNullOrWhiteSpace($latestLine)) {
                Set-LiveStatus "$Activity - elapsed $elapsedText"
            } else {
                $summary = $latestLine
                if ($summary.Length -gt 92) {
                    $summary = $summary.Substring(0, 89) + "..."
                }

                Set-LiveStatus "$Activity - elapsed $elapsedText`n$summary"
            }

            if (-not $process.HasExited) {
                Start-Sleep -Milliseconds 100
                $process.Refresh()
            }
        }

        $process.WaitForExit()
        $exitCode = $process.ExitCode
    } finally {
        $process.Dispose()
    }

    if ($exitCode -ne 0) {
        throw "Docker Compose failed: $($Arguments -join ' ')"
    }

    $elapsed = [DateTime]::UtcNow - $startedAt
    Write-Detail "$Activity completed in $('{0:mm\:ss}' -f $elapsed)."
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    # Windows PowerShell 5.1 can turn normal native stderr output such as
    # "Image ... Pulling" into a terminating NativeCommandError when the
    # script uses ErrorActionPreference=Stop. Docker Compose writes progress
    # to stderr even on success, so temporarily disable terminating handling
    # only for the native Docker invocation and decide success from LASTEXITCODE.
    $previousErrorActionPreference = $ErrorActionPreference

    try {
        $ErrorActionPreference = "Continue"

        $composeFileArguments = $script:ComposeFileArguments
        $output = @(
            & $script:Docker compose `
                --env-file $runtimeEnvironmentPath `
                @composeFileArguments `
                @Arguments 2>&1
        )

        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    foreach ($line in $output) {
        Write-Log "compose: $($line.ToString())"
    }

    if ($exitCode -ne 0) {
        $detail = (
            $output |
                ForEach-Object { $_.ToString() } |
                Select-Object -Last 12 |
                Out-String
        ).Trim()

        if ([string]::IsNullOrWhiteSpace($detail)) {
            throw "Docker Compose failed: $($Arguments -join ' ')"
        }

        throw "Docker Compose failed: $($Arguments -join ' ')`n`n$detail"
    }
}

function Wait-ForContainerRunning {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
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
    param([Parameter(Mandatory = $true)][string]$Value)

    return $Value.Replace("'", "''")
}

function Synchronize-PostgresPassword {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [Parameter(Mandatory = $true)][string]$DatabaseUser,
        [Parameter(Mandatory = $true)][string]$DatabaseName,
        [Parameter(Mandatory = $true)][string]$Password
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
        [Parameter(Mandatory = $true)][string]$Url,
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
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $startedAt = [DateTime]::UtcNow
    $deadline = $startedAt.AddSeconds($TimeoutSeconds)
    Set-Status "Waiting for $Name..."

    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-HttpReady -Url $Url) {
            Set-Status "$Name ready."
            Write-Log "$Name ready: $Url"
            return
        }

        $elapsedSeconds = [int]([DateTime]::UtcNow - $startedAt).TotalSeconds
        Set-LiveStatus "Waiting for $Name... $elapsedSeconds / $TimeoutSeconds seconds"
        Start-Sleep -Seconds 2
    }

    throw "$Name did not become ready: $Url"
}

function Collect-Diagnostics {
    try {
        Write-Log "----- docker compose ps -----"
        $composeFileArguments = $script:ComposeFileArguments
        (& $script:Docker compose `
            --env-file $runtimeEnvironmentPath `
            @composeFileArguments `
            ps --all 2>&1) | ForEach-Object {
                Write-Log $_.ToString()
            }

        Write-Log "----- docker compose logs -----"
        (& $script:Docker compose `
            --env-file $runtimeEnvironmentPath `
            @composeFileArguments `
            logs --tail 180 --no-color 2>&1) | ForEach-Object {
                Write-Log $_.ToString()
            }
    } catch {
        Write-Log "Could not collect Docker diagnostics: $($_.Exception.Message)"
    }
}

function Fail {
    param([Parameter(Mandatory = $true)][string]$Message)

    Write-Log "FAILED: $Message"

    if ($null -ne $script:Docker) {
        Push-Location $deploymentDirectory
        try {
            Collect-Diagnostics
        } finally {
            Pop-Location
        }
    }

    [System.Windows.Forms.MessageBox]::Show(
        "$Message`n`nDiagnostic log:`n$logPath",
        "SecureChat Community Node",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null

    $form.Close()
    exit 1
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

    Set-ProgressValue -Value 5
    Set-Status "Starting Docker Desktop..."
    Ensure-Docker
    Assert-ComposeVersion

    $config = Read-EnvironmentFile -Path $networkConfigPath

    foreach ($requiredValue in @("SECURECHAT_IMAGE_PREFIX", "SECURECHAT_IMAGE_TAG")) {
        if (-not $config.ContainsKey($requiredValue) -or [string]::IsNullOrWhiteSpace($config[$requiredValue])) {
            throw "securechat.conf is missing $requiredValue."
        }
    }

    $mode = Get-NetworkMode -Config $config
    $script:ComposeFileArguments = @("-f", $composePath, "-f", $releaseComposePath)

    if ($mode -eq "public") {
        $script:ComposeFileArguments += @("-f", $productionComposePath)
    }

    Set-Status "Checking SecureChat control plane..."
    $controlPlane = Resolve-ControlPlaneCandidates -Config $config -Mode $mode
    $hostAddress = Get-PrimaryIpv4Address
    $publicDomain = if ($mode -eq "public") { Resolve-PublicDomain -Config $config } else { $null }

    Write-Detail "Mode: $mode"
    Write-Detail "Control plane: $($controlPlane.HostProbeUrl)"
    Write-Detail "Image: $($config['SECURECHAT_IMAGE_PREFIX']):$($config['SECURECHAT_IMAGE_TAG'])"
    if ($mode -eq "public") {
        Write-Detail "Public domain: $publicDomain"
    } else {
        Write-Detail "LAN address: $hostAddress"
    }

    Set-ProgressValue -Value 15
    Set-Status "Preparing SecureChat node secrets..."
    New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null

    $mailboxPasswordPath = Join-Path $secretsDirectory "mailbox-database-password.txt"
    $federationPasswordPath = Join-Path $secretsDirectory "federation-database-password.txt"
    $federationTokenPath = Join-Path $secretsDirectory "federation-internal-api-token.txt"
    $gatewayTokenPath = Join-Path $secretsDirectory "gateway-internal-api-token.txt"

    @(
        $mailboxPasswordPath,
        $federationPasswordPath,
        $federationTokenPath,
        $gatewayTokenPath
    ) | ForEach-Object {
        Ensure-SecretFile -Path $_
    }

    $mailboxPassword = (Get-Content -LiteralPath $mailboxPasswordPath -Raw).Trim()
    $federationPassword = (Get-Content -LiteralPath $federationPasswordPath -Raw).Trim()

    $clientEndpoint = if ($mode -eq "public") { "wss://$publicDomain/relay" } else { "ws://$hostAddress`:$publicPort/relay" }
    $httpEndpoint = if ($mode -eq "public") { "https://$publicDomain" } else { "http://$hostAddress`:$publicPort" }
    $siteAddress = if ($mode -eq "public") { $publicDomain } else { ":80" }

    Write-Detail "Client endpoint: $clientEndpoint"
    Write-Detail "Federation/mailbox endpoint: $httpEndpoint"

    $runtimeEnvironment = @(
        "COMMUNITY_NODE_PROJECT_NAME=securechat-community-node",
        "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
        "COMMUNITY_NODE_HTTP_PORT=$publicPort",
        "COMMUNITY_NODE_SITE_ADDRESS=$siteAddress",
        "COMMUNITY_NODE_DOMAIN=$publicDomain",
        "CONTROL_PLANE_URL=$($controlPlane.ContainerUrl)",
        "CLIENT_ENDPOINT=$clientEndpoint",
        "FEDERATION_ENDPOINT=$httpEndpoint",
        "MAILBOX_ENDPOINT=$httpEndpoint",
        "SECURECHAT_IMAGE_PREFIX=$($config['SECURECHAT_IMAGE_PREFIX'])",
        "SECURECHAT_IMAGE_TAG=$($config['SECURECHAT_IMAGE_TAG'])",
        "SECURECHAT_UPDATE_INTERVAL_SECONDS=300",
        "MAILBOX_DATABASE_PASSWORD_FILE=./secrets/mailbox-database-password.txt",
        "FEDERATION_DATABASE_PASSWORD_FILE=./secrets/federation-database-password.txt",
        "FEDERATION_INTERNAL_API_TOKEN_FILE=./secrets/federation-internal-api-token.txt",
        "GATEWAY_INTERNAL_API_TOKEN_FILE=./secrets/gateway-internal-api-token.txt"
    )

    [System.IO.File]::WriteAllLines(
        $runtimeEnvironmentPath,
        $runtimeEnvironment,
        [System.Text.UTF8Encoding]::new($false)
    )

    Push-Location $deploymentDirectory
    try {
        Set-ProgressValue -Value 25
        Set-Status "Validating node configuration..."
        Invoke-Compose -Arguments @("config", "--quiet")

        Set-Status "Pulling SecureChat node images..."
        Invoke-ComposeStreaming `
            -Arguments @("pull") `
            -Activity "Pulling SecureChat node images"
        Set-ProgressValue -Value 55

        Set-ProgressValue -Value 65
        Set-Status "Starting node databases..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "mailbox-database",
            "federation-database"
        )

        Wait-ForContainerRunning -Service "mailbox-database"
        Wait-ForContainerRunning -Service "federation-database"

        Start-Sleep -Seconds 3

        Synchronize-PostgresPassword `
            -Service "mailbox-database" `
            -DatabaseUser "securechat_mailbox" `
            -DatabaseName "securechat_mailbox" `
            -Password $mailboxPassword

        Synchronize-PostgresPassword `
            -Service "federation-database" `
            -DatabaseUser "securechat_federation" `
            -DatabaseName "securechat_federation" `
            -Password $federationPassword

        Set-ProgressValue -Value 82
        Set-Status "Starting SecureChat node services..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--remove-orphans"
        )

        Set-ProgressValue -Value 90
        Set-Status "Reloading community-node routing..."
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
        -Name "mailbox" `
        -Url "http://127.0.0.1:$publicPort/health/mailbox"

    Wait-ForEndpoint `
        -Name "federation" `
        -Url "http://127.0.0.1:$publicPort/health/federation"

    Wait-ForEndpoint `
        -Name "gateway" `
        -Url "http://127.0.0.1:$publicPort/health/gateway"

    Set-ProgressValue -Value 97
    Set-Status "Verifying control-plane registration..."

    if (-not (Test-ControlPlane -Url $controlPlane.HostProbeUrl)) {
        throw "The SecureChat control plane became unreachable."
    }

    Set-ProgressValue -Value 100
    $title.Text = "SecureChat community node is running"
    $displayUrl = if ($mode -eq "public") { "https://$publicDomain" } else { "http://$hostAddress`:$publicPort" }
    $status.Text = $displayUrl
    Write-Log "SUCCESS: $displayUrl"

    [System.Windows.Forms.Application]::DoEvents()
    Start-Sleep -Seconds 3

    $form.Close()
    exit 0
} catch {
    Fail -Message $_.Exception.Message
}
