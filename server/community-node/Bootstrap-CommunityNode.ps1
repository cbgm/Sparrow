[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$networkConfigPath = Join-Path $deploymentDirectory "sparrow.conf"
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
$form.Text = "Sparrow Community Node"
$form.Size = New-Object System.Drawing.Size(760, 430)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(705, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting Sparrow community node"
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

function Write-NetworkConfiguration {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Config,
        [Parameter(Mandatory = $true)][string]$Mode,
        [string]$PublicDomain,
        [Parameter(Mandatory = $true)][string]$DirectoryUrl,
        [Parameter(Mandatory = $true)][string]$ImageTag
    )

    $lines = @(
        "# Sparrow community-node configuration",
        "# Generated and updated by the launcher. You may also edit this file by hand while the stack is stopped.",
        "CONFIGURED=true",
        "MODE=$Mode",
        "PUBLIC_DOMAIN=$PublicDomain",
        "CONTROL_PLANE_DIRECTORY_URL=$DirectoryUrl",
        "SPARROW_IMAGE_PREFIX=$($Config['SPARROW_IMAGE_PREFIX'])",
        "SPARROW_IMAGE_TAG=$ImageTag"
    )

    [System.IO.File]::WriteAllLines(
        $networkConfigPath,
        $lines,
        [System.Text.UTF8Encoding]::new($false)
    )
}

function Read-LauncherConfiguration {
    param([Parameter(Mandatory = $true)][hashtable]$Config)

    $panel = New-Object System.Windows.Forms.Panel
    $panel.Location = New-Object System.Drawing.Point(24, 58)
    $panel.Size = New-Object System.Drawing.Size(705, 316)

    $modeLabel = New-Object System.Windows.Forms.Label
    $modeLabel.Location = New-Object System.Drawing.Point(0, 4)
    $modeLabel.Size = New-Object System.Drawing.Size(205, 22)
    $modeLabel.Text = "Reachability"
    $panel.Controls.Add($modeLabel)

    $modeCombo = New-Object System.Windows.Forms.ComboBox
    $modeCombo.Location = New-Object System.Drawing.Point(220, 0)
    $modeCombo.Size = New-Object System.Drawing.Size(460, 28)
    $modeCombo.DropDownStyle = [System.Windows.Forms.ComboBoxStyle]::DropDownList
    [void]$modeCombo.Items.Add("LAN")
    [void]$modeCombo.Items.Add("Public")
    $configuredMode = if ($Config.ContainsKey("MODE")) { $Config["MODE"].Trim().ToLowerInvariant() } else { "lan" }
    $modeCombo.SelectedIndex = if ($configuredMode -eq "public") { 1 } else { 0 }
    $panel.Controls.Add($modeCombo)

    $publicModeLabel = New-Object System.Windows.Forms.Label
    $publicModeLabel.Location = New-Object System.Drawing.Point(0, 52)
    $publicModeLabel.Size = New-Object System.Drawing.Size(205, 22)
    $publicModeLabel.Text = "Public address"
    $panel.Controls.Add($publicModeLabel)

    $publicModeCombo = New-Object System.Windows.Forms.ComboBox
    $publicModeCombo.Location = New-Object System.Drawing.Point(220, 48)
    $publicModeCombo.Size = New-Object System.Drawing.Size(460, 28)
    $publicModeCombo.DropDownStyle = [System.Windows.Forms.ComboBoxStyle]::DropDownList
    [void]$publicModeCombo.Items.Add("Automatic (sslip.io)")
    [void]$publicModeCombo.Items.Add("Own address")
    $configuredPublicDomain = if ($Config.ContainsKey("PUBLIC_DOMAIN")) { $Config["PUBLIC_DOMAIN"].Trim() } else { "" }
    $publicModeCombo.SelectedIndex = if ([string]::IsNullOrWhiteSpace($configuredPublicDomain)) { 0 } else { 1 }
    $panel.Controls.Add($publicModeCombo)

    $publicAddressLabel = New-Object System.Windows.Forms.Label
    $publicAddressLabel.Location = New-Object System.Drawing.Point(0, 100)
    $publicAddressLabel.Size = New-Object System.Drawing.Size(205, 22)
    $publicAddressLabel.Text = "Own public domain / host"
    $panel.Controls.Add($publicAddressLabel)

    $publicAddressText = New-Object System.Windows.Forms.TextBox
    $publicAddressText.Location = New-Object System.Drawing.Point(220, 96)
    $publicAddressText.Size = New-Object System.Drawing.Size(460, 27)
    $publicAddressText.Text = $configuredPublicDomain
    $panel.Controls.Add($publicAddressText)

    $directoryLabel = New-Object System.Windows.Forms.Label
    $directoryLabel.Location = New-Object System.Drawing.Point(0, 148)
    $directoryLabel.Size = New-Object System.Drawing.Size(205, 22)
    $directoryLabel.Text = "Control-plane directory URL"
    $panel.Controls.Add($directoryLabel)

    $directoryText = New-Object System.Windows.Forms.TextBox
    $directoryText.Location = New-Object System.Drawing.Point(220, 144)
    $directoryText.Size = New-Object System.Drawing.Size(460, 27)
    $directoryText.Text = if ($Config.ContainsKey("CONTROL_PLANE_DIRECTORY_URL")) { $Config["CONTROL_PLANE_DIRECTORY_URL"].Trim() } else { "" }
    $panel.Controls.Add($directoryText)

    $hint = New-Object System.Windows.Forms.Label
    $hint.Location = New-Object System.Drawing.Point(220, 176)
    $hint.Size = New-Object System.Drawing.Size(460, 36)
    $hint.Text = "The directory URL must return JSON containing a controlPlanes array. This is the only configured source of control-plane addresses."
    $panel.Controls.Add($hint)

    $imageTagLabel = New-Object System.Windows.Forms.Label
    $imageTagLabel.Location = New-Object System.Drawing.Point(0, 218)
    $imageTagLabel.Size = New-Object System.Drawing.Size(205, 22)
    $imageTagLabel.Text = "Image tag"
    $panel.Controls.Add($imageTagLabel)

    $imageTagText = New-Object System.Windows.Forms.TextBox
    $imageTagText.Location = New-Object System.Drawing.Point(220, 214)
    $imageTagText.Size = New-Object System.Drawing.Size(460, 27)
    $imageTagText.Text = "latest"
    $panel.Controls.Add($imageTagText)

    $validation = New-Object System.Windows.Forms.Label
    $validation.Location = New-Object System.Drawing.Point(0, 246)
    $validation.Size = New-Object System.Drawing.Size(470, 28)
    $validation.ForeColor = [System.Drawing.Color]::Firebrick
    $validation.Text = ""
    $panel.Controls.Add($validation)

    $startButton = New-Object System.Windows.Forms.Button
    $startButton.Location = New-Object System.Drawing.Point(480, 278)
    $startButton.Size = New-Object System.Drawing.Size(95, 34)
    $startButton.Text = "Start"
    $panel.Controls.Add($startButton)

    $cancelButton = New-Object System.Windows.Forms.Button
    $cancelButton.Location = New-Object System.Drawing.Point(585, 278)
    $cancelButton.Size = New-Object System.Drawing.Size(95, 34)
    $cancelButton.Text = "Cancel"
    $panel.Controls.Add($cancelButton)

    $state = @{
        Done = $false
        Cancelled = $false
        Mode = "lan"
        PublicDomain = ""
        DirectoryUrl = ""
        ImageTag = "latest"
    }

    $updatePublicControls = {
        $isPublic = $modeCombo.SelectedIndex -eq 1
        $publicModeLabel.Enabled = $isPublic
        $publicModeCombo.Enabled = $isPublic
        $publicAddressLabel.Enabled = $isPublic
        $publicAddressText.Enabled = $isPublic -and $publicModeCombo.SelectedIndex -eq 1
    }

    $modeCombo.Add_SelectedIndexChanged($updatePublicControls)
    $publicModeCombo.Add_SelectedIndexChanged($updatePublicControls)
    & $updatePublicControls

    $startButton.Add_Click({
        $validation.Text = ""
        $mode = if ($modeCombo.SelectedIndex -eq 1) { "public" } else { "lan" }
        $publicDomain = ""
        $directoryUrl = $directoryText.Text.Trim()
        $imageTag = $imageTagText.Text.Trim()

        if ($mode -eq "public" -and $publicModeCombo.SelectedIndex -eq 1) {
            $publicDomain = $publicAddressText.Text.Trim().TrimEnd(".")
            if ([string]::IsNullOrWhiteSpace($publicDomain)) {
                $validation.Text = "Enter your public domain / host, or select Automatic (sslip.io)."
                return
            }
            if ($publicDomain.Contains("://") -or $publicDomain.Contains("/") -or $publicDomain.Contains(" ")) {
                $validation.Text = "Enter only the public domain / host, without a scheme or path."
                return
            }
        }

        if ($imageTag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
            $validation.Text = "Enter a valid Docker image tag, for example latest, pr-49, or sha-..."
            return
        }

        $directoryUri = $null
        if (
            [string]::IsNullOrWhiteSpace($directoryUrl) -or
            -not [Uri]::TryCreate($directoryUrl, [UriKind]::Absolute, [ref]$directoryUri) -or
            $directoryUri.Scheme -notin @("http", "https")
        ) {
            $validation.Text = "Enter a valid HTTP or HTTPS control-plane directory URL."
            return
        }

        $state.Mode = $mode
        $state.PublicDomain = $publicDomain
        $state.DirectoryUrl = $directoryUrl
        $state.ImageTag = $imageTag
        $state.Done = $true
    })

    $cancelButton.Add_Click({
        $state.Cancelled = $true
        $state.Done = $true
    })

    $title.Text = "Configure Sparrow community node"
    $status.Visible = $false
    $progress.Visible = $false
    $details.Visible = $false
    $form.Controls.Add($panel)
    $panel.BringToFront()
    $form.AcceptButton = $startButton
    $form.CancelButton = $cancelButton

    while (-not $state.Done -and -not $form.IsDisposed) {
        [System.Windows.Forms.Application]::DoEvents()
        Start-Sleep -Milliseconds 40
    }

    $form.AcceptButton = $null
    $form.CancelButton = $null
    $form.Controls.Remove($panel)
    $panel.Dispose()
    $status.Visible = $true
    $progress.Visible = $true
    $details.Visible = $true
    $title.Text = "Starting Sparrow community node"
    [System.Windows.Forms.Application]::DoEvents()

    if ($state.Cancelled -or $form.IsDisposed) {
        throw "Community-node setup was cancelled."
    }

    return [PSCustomObject]@{
        Mode = $state.Mode
        PublicDomain = $state.PublicDomain
        DirectoryUrl = $state.DirectoryUrl
        ImageTag = $state.ImageTag
    }
}

function Initialize-NetworkConfiguration {
    param([Parameter(Mandatory = $true)][hashtable]$Config)

    $launcherConfig = Read-LauncherConfiguration -Config $Config

    Write-NetworkConfiguration `
        -Config $Config `
        -Mode $launcherConfig.Mode `
        -PublicDomain $launcherConfig.PublicDomain `
        -DirectoryUrl $launcherConfig.DirectoryUrl `
        -ImageTag $launcherConfig.ImageTag

    return Read-EnvironmentFile -Path $networkConfigPath
}

function Get-DirectoryControlPlaneUrls {
    param(
        [Parameter(Mandatory = $true)][string]$DirectoryUrl,
        [Parameter(Mandatory = $true)][string]$Mode
    )

    $response = Invoke-WebRequest -Uri $DirectoryUrl -Method Get -TimeoutSec 8 -UseBasicParsing
    try {
        $document = $response.Content | ConvertFrom-Json -ErrorAction Stop
    } catch {
        throw "Control-plane directory response is not valid JSON: $($_.Exception.Message)"
    }

    if ($null -eq $document.controlPlanes) {
        throw "Control-plane directory response does not contain controlPlanes."
    }

    $urls = @(
        $document.controlPlanes |
            ForEach-Object { Normalize-ControlPlaneUrl -Value $_.ToString() -Mode $Mode } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            Select-Object -Unique
    )

    if ($urls.Count -eq 0) {
        throw "Control-plane directory returned no control-plane addresses."
    }

    return $urls
}

function Get-CachedControlPlaneUrls {
    param([Parameter(Mandatory = $true)][string]$Mode)

    if (-not (Test-Path -LiteralPath $runtimeEnvironmentPath -PathType Leaf)) {
        return @()
    }

    $runtime = Read-EnvironmentFile -Path $runtimeEnvironmentPath
    $cachedValue = if ($runtime.ContainsKey("ADVERTISED_CONTROL_PLANE_URLS")) {
            $runtime["ADVERTISED_CONTROL_PLANE_URLS"]
        } elseif ($runtime.ContainsKey("CONTROL_PLANE_URLS")) {
            $runtime["CONTROL_PLANE_URLS"]
        } else {
            ""
        }

    if ([string]::IsNullOrWhiteSpace($cachedValue)) {
        return @()
    }

    $urls = [System.Collections.Generic.List[string]]::new()
    foreach ($rawValue in ($cachedValue -split '[,;]')) {
        try {
            $normalized = Normalize-ControlPlaneUrl -Value $rawValue -Mode $Mode
            if (-not [string]::IsNullOrWhiteSpace($normalized) -and -not $urls.Contains($normalized)) {
                $urls.Add($normalized)
            }
        } catch {
            Write-Log "Ignoring cached control-plane address '$rawValue': $($_.Exception.Message)"
        }
    }

    return @($urls)
}

function Wait-RetryInterval {
    param([Parameter(Mandatory = $true)][int]$Seconds)

    $steps = $Seconds * 10
    for ($step = 0; $step -lt $steps; $step++) {
        if ($form.IsDisposed) {
            throw "Community-node startup was cancelled."
        }

        Start-Sleep -Milliseconds 100
        [System.Windows.Forms.Application]::DoEvents()
    }
}

function Resolve-ConfiguredControlPlaneUrls {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Config,
        [Parameter(Mandatory = $true)][string]$Mode
    )

    $directoryUrl = if ($Config.ContainsKey("CONTROL_PLANE_DIRECTORY_URL")) {
        $Config["CONTROL_PLANE_DIRECTORY_URL"].Trim()
    } else {
        ""
    }

    if ([string]::IsNullOrWhiteSpace($directoryUrl)) {
        throw "sparrow.conf is missing CONTROL_PLANE_DIRECTORY_URL."
    }

    while ($true) {
        Set-Status "Loading control-plane directory..."
        try {
            return @(Get-DirectoryControlPlaneUrls -DirectoryUrl $directoryUrl -Mode $Mode)
        } catch {
            Write-Log "Control-plane directory unavailable: $($_.Exception.Message)"

            $cachedUrls = @(Get-CachedControlPlaneUrls -Mode $Mode)
            if ($cachedUrls.Count -gt 0) {
                Write-Detail "Directory unavailable; using the last known control-plane addresses."
                return $cachedUrls
            }

            Set-Status "Control-plane directory unavailable. Retrying in 5 seconds..."
            Wait-RetryInterval -Seconds 5
        }
    }
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

    $base = $Url.TrimEnd("/")
    $healthUrl = "$base/health/registry"

    try {
        $request = [System.Net.HttpWebRequest]::Create($healthUrl)
        $request.Method = "GET"
        $request.Timeout = 3000
        $request.ReadWriteTimeout = 3000
        $request.AllowAutoRedirect = $true

        $response = $request.GetResponse()
        try {
            $statusCode = [int]$response.StatusCode
            return $statusCode -ge 200 -and $statusCode -lt 300
        } finally {
            $response.Close()
        }
    } catch {
        Write-Log "Control-plane health probe failed at ${healthUrl}: $($_.Exception.Message)"
        return $false
    }
}

function Get-NetworkMode {
    param([Parameter(Mandatory = $true)][hashtable]$Config)

    $mode = if ($Config.ContainsKey("MODE")) { $Config["MODE"].Trim().ToLowerInvariant() } else { "lan" }

    if ($mode -notin @("lan", "public")) {
        throw "sparrow.conf MODE must be lan or public."
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

    throw "Could not detect the public IPv4 address. Set PUBLIC_DOMAIN in sparrow.conf."
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
        throw "Invalid control-plane address in sparrow.conf: $Value"
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
        $hostProbeUrl = $ConfiguredUrl.TrimEnd("/")

        if (-not (Test-ControlPlane -Url $hostProbeUrl)) {
            throw "The local Sparrow control plane is not reachable at $hostProbeUrl."
        }

        $containerUrl = if ($uri.Scheme -eq "http") { "http://host.docker.internal`:$port" } else { $ConfiguredUrl }

        return [PSCustomObject]@{
            HostProbeUrl = $hostProbeUrl
            ContainerUrl = $containerUrl.TrimEnd("/")
        }
    }

    if (-not (Test-ControlPlane -Url $ConfiguredUrl)) {
        throw "The Sparrow control plane is not reachable at $ConfiguredUrl."
    }

    return [PSCustomObject]@{
        HostProbeUrl = $ConfiguredUrl.TrimEnd("/")
        ContainerUrl = $ConfiguredUrl.TrimEnd("/")
    }
}

function Convert-ControlPlaneUrlForContainer {
    param([Parameter(Mandatory = $true)][string]$ConfiguredUrl)

    $uri = [Uri]$ConfiguredUrl
    $port = if ($uri.IsDefaultPort) { if ($uri.Scheme -eq "https") { 443 } else { 80 } } else { $uri.Port }

    if ((Test-IsLocalHostAddress -HostName $uri.Host) -and $uri.Scheme -eq "http") {
        return "http://host.docker.internal`:$port"
    }

    return $ConfiguredUrl.TrimEnd("/")
}

function Resolve-ControlPlaneCandidates {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Config,
        [Parameter(Mandatory = $true)][string]$Mode
    )

    $failures = @()
    $containerUrls = [System.Collections.Generic.List[string]]::new()
    $advertisedUrls = [System.Collections.Generic.List[string]]::new()
    $selected = $null
    $configuredUrls = Resolve-ConfiguredControlPlaneUrls -Config $Config -Mode $Mode

    foreach ($candidate in $configuredUrls) {
        $containerUrl = Convert-ControlPlaneUrlForContainer -ConfiguredUrl $candidate
        if (-not $containerUrls.Contains($containerUrl)) {
            $containerUrls.Add($containerUrl)
        }
        if (-not $advertisedUrls.Contains($candidate)) {
            $advertisedUrls.Add($candidate)
        }

        if ($null -ne $selected) {
            continue
        }

        try {
            Set-Status "Checking control plane $candidate..."
            $selected = Resolve-ControlPlaneUrl -ConfiguredUrl $candidate
        } catch {
            $failures += "$candidate - $($_.Exception.Message)"
            Write-Log "Control-plane candidate failed: $candidate"
        }
    }

    if ($null -eq $selected) {
        $fallback = $configuredUrls[0]
        $selected = [PSCustomObject]@{
            HostProbeUrl = $fallback
            ContainerUrl = (Convert-ControlPlaneUrlForContainer -ConfiguredUrl $fallback)
        }
        $failureDetails = $failures -join "`n"
        Write-Log "No control plane is currently reachable; node services will keep retrying in the background.`n$failureDetails"
        Write-Detail "No control plane is currently reachable. Starting node; registration will retry automatically."
    }

    return [PSCustomObject]@{
        HostProbeUrl = $selected.HostProbeUrl
        ContainerUrl = $selected.ContainerUrl
        ContainerUrls = @($containerUrls)
        AdvertisedUrls = @($advertisedUrls)
    }
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
    if (-not (Test-Path -LiteralPath $runtimeEnvironmentPath -PathType Leaf)) {
        Write-Log "Skipping Docker diagnostics because .env.runtime has not been generated yet."
        return
    }

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
        "Sparrow Community Node",
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
    $config = Initialize-NetworkConfiguration -Config $config

    foreach ($requiredValue in @("SPARROW_IMAGE_PREFIX", "SPARROW_IMAGE_TAG")) {
        if (-not $config.ContainsKey($requiredValue) -or [string]::IsNullOrWhiteSpace($config[$requiredValue])) {
            throw "sparrow.conf is missing $requiredValue."
        }
    }

    $mode = Get-NetworkMode -Config $config
    $script:ComposeFileArguments = @("-f", $composePath, "-f", $releaseComposePath)

    if ($mode -eq "public") {
        $script:ComposeFileArguments += @("-f", $productionComposePath)
    }

    Set-Status "Checking Sparrow control plane..."
    $controlPlane = Resolve-ControlPlaneCandidates -Config $config -Mode $mode
    $hostAddress = Get-PrimaryIpv4Address
    $publicDomain = if ($mode -eq "public") { Resolve-PublicDomain -Config $config } else { $null }

    Write-Detail "Mode: $mode"
    Write-Detail "Control plane: $($controlPlane.HostProbeUrl)"
    Write-Detail "Image: $($config['SPARROW_IMAGE_PREFIX']):$($config['SPARROW_IMAGE_TAG'])"
    if ($mode -eq "public") {
        Write-Detail "Public domain: $publicDomain"
    } else {
        Write-Detail "LAN address: $hostAddress"
    }

    Set-ProgressValue -Value 15
    Set-Status "Preparing Sparrow node secrets..."
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

    $clientEndpoint = if ($mode -eq "public") { "wss://$publicDomain/v1/gateway" } else { "ws://$hostAddress`:$publicPort/v1/gateway" }
    $httpEndpoint = if ($mode -eq "public") { "https://$publicDomain" } else { "http://$hostAddress`:$publicPort" }
    $siteAddress = if ($mode -eq "public") { $publicDomain } else { ":80" }

    Write-Detail "Client endpoint: $clientEndpoint"
    Write-Detail "Federation/mailbox endpoint: $httpEndpoint"

    $runtimeEnvironment = @(
        "COMMUNITY_NODE_PROJECT_NAME=sparrow-community-node",
        "COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0",
        "COMMUNITY_NODE_HTTP_PORT=$publicPort",
        "COMMUNITY_NODE_SITE_ADDRESS=$siteAddress",
        "COMMUNITY_NODE_DOMAIN=$publicDomain",
        "CONTROL_PLANE_URL=$($controlPlane.ContainerUrl)",
        "CONTROL_PLANE_URLS=$($controlPlane.ContainerUrls -join ',')",
        "ADVERTISED_CONTROL_PLANE_URLS=$($controlPlane.AdvertisedUrls -join ',')",
        "CLIENT_ENDPOINT=$clientEndpoint",
        "FEDERATION_ENDPOINT=$httpEndpoint",
        "MAILBOX_ENDPOINT=$httpEndpoint",
        "SPARROW_IMAGE_PREFIX=$($config['SPARROW_IMAGE_PREFIX'])",
        "SPARROW_IMAGE_TAG=$($config['SPARROW_IMAGE_TAG'])",
        "SPARROW_UPDATE_INTERVAL_SECONDS=300",
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

        Set-Status "Pulling Sparrow node images..."
        Invoke-ComposeStreaming `
            -Arguments @("pull") `
            -Activity "Pulling Sparrow node images"
        Set-ProgressValue -Value 55

        Set-ProgressValue -Value 65
        Set-Status "Starting node databases..."
        # Always recreate the database containers here. Docker Desktop implements
        # Compose file-based secrets as host bind mounts. If a secret file was
        # temporarily unavailable, a stopped container can retain a stale mount
        # and `docker compose start`/`up` may keep failing even after the file is
        # present again. Recreating the containers refreshes those mounts while
        # preserving the named database volumes.
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--force-recreate",
            "mailbox-database",
            "federation-database"
        )

        Wait-ForContainerRunning -Service "mailbox-database"
        Wait-ForContainerRunning -Service "federation-database"

        Start-Sleep -Seconds 3

        Synchronize-PostgresPassword `
            -Service "mailbox-database" `
            -DatabaseUser "sparrow_mailbox" `
            -DatabaseName "sparrow_mailbox" `
            -Password $mailboxPassword

        Synchronize-PostgresPassword `
            -Service "federation-database" `
            -DatabaseUser "sparrow_federation" `
            -DatabaseName "sparrow_federation" `
            -Password $federationPassword

        Set-ProgressValue -Value 82
        Set-Status "Starting Sparrow node services..."
        # Refresh all secret-bearing service containers as well so their
        # /run/secrets mounts cannot remain stale after a Docker Desktop restart.
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--remove-orphans",
            "--force-recreate"
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
        Write-Log "Control plane is currently unavailable; node registration will recover automatically."
        Write-Detail "Control plane unavailable; registration will retry in the background."
    }

    Set-ProgressValue -Value 100
    $title.Text = "Sparrow community node is running"
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
