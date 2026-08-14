[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$deploymentDirectory = $PSScriptRoot
$runtimeEnvironmentPath = Join-Path $deploymentDirectory ".env.runtime"
$networkConfigPath = Join-Path $deploymentDirectory "sparrow.conf"
$secretsDirectory = Join-Path $deploymentDirectory "secrets"
$composePath = Join-Path $deploymentDirectory "docker-compose.yml"
$releaseComposePath = Join-Path $deploymentDirectory "docker-compose.release.yml"
$productionComposePath = Join-Path $deploymentDirectory "docker-compose.production.yml"
$logPath = Join-Path $deploymentDirectory "bootstrap-control-plane.log"
$controlPlanePort = 8390

$deploymentPath = [System.IO.Path]::GetFullPath($deploymentDirectory).ToLowerInvariant()
$deploymentBytes = [System.Text.Encoding]::UTF8.GetBytes($deploymentPath)
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $deploymentHash =
        [System.BitConverter]::ToString($sha256.ComputeHash($deploymentBytes)).Replace("-", "")
} finally {
    $sha256.Dispose()
}
$mutexName = "Local\SparrowControlPlane-$($deploymentHash.Substring(0, 24))"
$script:DeploymentMutex = [System.Threading.Mutex]::new($false, $mutexName)
$hasDeploymentLock = $false
try {
    $hasDeploymentLock = $script:DeploymentMutex.WaitOne(0)
} catch [System.Threading.AbandonedMutexException] {
    $hasDeploymentLock = $true
}
if (-not $hasDeploymentLock) {
    [System.Windows.Forms.MessageBox]::Show(
        "This Control Plane launcher is already running for this deployment folder.",
        "Sparrow Control Plane",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Information
    ) | Out-Null
    exit 0
}

$script:Docker = $null
$script:ComposeFileArguments = @()

$form = New-Object System.Windows.Forms.Form
$form.Text = "Sparrow Control Plane"
$form.Size = New-Object System.Drawing.Size(760, 430)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
$form.MaximizeBox = $false
$form.TopMost = $true

$title = New-Object System.Windows.Forms.Label
$title.Location = New-Object System.Drawing.Point(24, 22)
$title.Size = New-Object System.Drawing.Size(705, 28)
$title.Font = New-Object System.Drawing.Font("Segoe UI", 12, [System.Drawing.FontStyle]::Bold)
$title.Text = "Starting Sparrow control plane"
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
    param([string]$Message)

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
    param([string]$Message)

    $status.Text = $Message
    Write-Log $Message
    Write-Detail $Message
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
        "Sparrow Control Plane",
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

function Write-NetworkConfiguration {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Config,
        [Parameter(Mandatory = $true)][string]$Mode,
        [string]$PublicDomain,
        [Parameter(Mandatory = $true)][string]$ControlPlaneId
    )

    [System.IO.File]::WriteAllLines(
        $networkConfigPath,
        @(
            "# Sparrow control-plane configuration",
            "# Generated and updated by the launcher. You may also edit this file by hand while the stack is stopped.",
            "CONFIGURED=true",
            "MODE=$Mode",
            "PUBLIC_DOMAIN=$PublicDomain",
            "CONTROL_PLANE_ID=$ControlPlaneId",
            "SPARROW_IMAGE_PREFIX=$($Config['SPARROW_IMAGE_PREFIX'])",
            "SPARROW_IMAGE_TAG=$($Config['SPARROW_IMAGE_TAG'])"
        ),
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

    $hint = New-Object System.Windows.Forms.Label
    $hint.Location = New-Object System.Drawing.Point(220, 128)
    $hint.Size = New-Object System.Drawing.Size(460, 42)
    $hint.Text = "Automatic detects the public IPv4 address and uses <public-ip>.sslip.io. Own address expects a domain or host such as cp.example.com."
    $panel.Controls.Add($hint)

    $validation = New-Object System.Windows.Forms.Label
    $validation.Location = New-Object System.Drawing.Point(0, 184)
    $validation.Size = New-Object System.Drawing.Size(680, 40)
    $validation.ForeColor = [System.Drawing.Color]::Firebrick
    $validation.Text = ""
    $panel.Controls.Add($validation)

    $startButton = New-Object System.Windows.Forms.Button
    $startButton.Location = New-Object System.Drawing.Point(480, 250)
    $startButton.Size = New-Object System.Drawing.Size(95, 34)
    $startButton.Text = "Start"
    $panel.Controls.Add($startButton)

    $cancelButton = New-Object System.Windows.Forms.Button
    $cancelButton.Location = New-Object System.Drawing.Point(585, 250)
    $cancelButton.Size = New-Object System.Drawing.Size(95, 34)
    $cancelButton.Text = "Cancel"
    $panel.Controls.Add($cancelButton)

    $state = @{
        Done = $false
        Cancelled = $false
        Mode = "lan"
        PublicDomain = ""
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

        $state.Mode = $mode
        $state.PublicDomain = $publicDomain
        $state.Done = $true
    })

    $cancelButton.Add_Click({
        $state.Cancelled = $true
        $state.Done = $true
    })

    $title.Text = "Configure Sparrow control plane"
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
    $title.Text = "Starting Sparrow control plane"
    [System.Windows.Forms.Application]::DoEvents()

    if ($state.Cancelled -or $form.IsDisposed) {
        throw "Control-plane setup was cancelled."
    }

    return [PSCustomObject]@{
        Mode = $state.Mode
        PublicDomain = $state.PublicDomain
    }
}

function New-ControlPlaneId {
    return [Guid]::NewGuid().ToString("N")
}

function Initialize-NetworkConfiguration {
    param([Parameter(Mandatory = $true)][hashtable]$Config)

    $controlPlaneId =
        if ($Config.ContainsKey("CONTROL_PLANE_ID") -and -not [string]::IsNullOrWhiteSpace($Config["CONTROL_PLANE_ID"])) {
            $Config["CONTROL_PLANE_ID"].Trim()
        } else {
            New-ControlPlaneId
        }

    $launcherConfig = Read-LauncherConfiguration -Config $Config

    Write-NetworkConfiguration `
        -Config $Config `
        -Mode $launcherConfig.Mode `
        -PublicDomain $launcherConfig.PublicDomain `
        -ControlPlaneId $controlPlaneId

    return Read-EnvironmentFile -Path $networkConfigPath
}

function Get-NetworkMode {
    param([hashtable]$Config)

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

function Get-NodeRegistryImageReference {
    param([hashtable]$Config)

    return "$($Config['SPARROW_IMAGE_PREFIX'])-node-registry:$($Config['SPARROW_IMAGE_TAG'])"
}

function Find-LegacyRegistryIdentityVolume {
    $volumeNames = @(& $script:Docker volume ls --format "{{.Name}}" 2>$null)
    $knownVolumes = @(
        "sparrow-control-plane_registry-identity",
        "control-plane_registry-identity"
    )

    foreach ($volume in $knownVolumes) {
        if ($volumeNames -contains $volume) {
            return $volume
        }
    }

    return $null
}

function Export-LegacyRegistryRoot {
    param(
        [Parameter(Mandatory = $true)][string]$Image,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    $volume = Find-LegacyRegistryIdentityVolume
    if ([string]::IsNullOrWhiteSpace($volume)) {
        return $false
    }

    Write-Detail "Migrating the existing registry trust root from Docker volume $volume."
    $output = @(
        & $script:Docker run `
            --rm `
            --entrypoint /bin/cat `
            -v "${volume}:/legacy:ro" `
            $Image `
            /legacy/registry.identity 2>&1
    )

    if ($LASTEXITCODE -ne 0 -or $output.Count -eq 0) {
        Write-Log "Existing registry root could not be exported from $volume."
        return $false
    }

    [System.IO.File]::WriteAllLines(
        $Destination,
        $output,
        [System.Text.UTF8Encoding]::new($false)
    )
    return $true
}

function Ensure-RegistryAuthority {
    param([hashtable]$Config)

    $authorityIdentityPath = Join-Path $secretsDirectory "registry-authority.identity"
    $authorityCertificatePath = Join-Path $secretsDirectory "registry-authority-certificate.json"
    $rootIdentityPath = Join-Path $secretsDirectory "registry-root.identity"
    $hasAuthorityIdentity = Test-Path -LiteralPath $authorityIdentityPath -PathType Leaf
    $hasAuthorityCertificate = Test-Path -LiteralPath $authorityCertificatePath -PathType Leaf

    if ($hasAuthorityIdentity -and $hasAuthorityCertificate) {
        return
    }
    if ($hasAuthorityIdentity -or $hasAuthorityCertificate) {
        throw "Registry authority identity and certificate must either both exist or both be absent."
    }

    $image = Get-NodeRegistryImageReference -Config $Config
    Set-LiveStatus "Preparing registry signing authority..."
    Write-Detail "Pulling registry image required for one-time authority provisioning."
    & $script:Docker pull $image 2>&1 | ForEach-Object { Write-Detail $_.ToString() }
    if ($LASTEXITCODE -ne 0) {
        throw "Could not pull the node-registry image required for authority provisioning."
    }

    $rootWasMigrated = $false
    if (-not (Test-Path -LiteralPath $rootIdentityPath -PathType Leaf)) {
        $rootWasMigrated = Export-LegacyRegistryRoot -Image $image -Destination $rootIdentityPath
    }
    if (-not (Test-Path -LiteralPath $rootIdentityPath -PathType Leaf)) {
        throw (
            "Registry authority is not provisioned. Copy the existing Sparrow registry-root.identity " +
            "into the secrets folder once, or copy registry-authority.identity and " +
            "registry-authority-certificate.json from another trusted control plane."
        )
    }

    $dockerSecretsPath = $secretsDirectory.Replace('\', '/')
    try {
        & $script:Docker run `
            --rm `
            --entrypoint java `
            -v "${dockerSecretsPath}:/secrets" `
            $image `
            -cp "/app/lib/*" `
            com.cbgm.sparrow.server.registry.RegistryAuthorityProvisioningCli `
            /secrets/registry-root.identity `
            /secrets/registry-authority.identity `
            /secrets/registry-authority-certificate.json 2>&1 |
            ForEach-Object { Write-Detail $_.ToString() }
        if ($LASTEXITCODE -ne 0) {
            throw "Registry authority provisioning failed."
        }
    } finally {
        if ($rootWasMigrated -and (Test-Path -LiteralPath $rootIdentityPath -PathType Leaf)) {
            Remove-Item -LiteralPath $rootIdentityPath -Force
            Write-Detail "Removed the temporary migrated root identity after authority provisioning."
        }
    }

    if (-not (Test-Path -LiteralPath $authorityIdentityPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $authorityCertificatePath -PathType Leaf)) {
        throw "Registry authority provisioning did not create the required files."
    }
}

function Find-FirebaseCredentials {
    $candidate = Join-Path $secretsDirectory "firebase-admin.json"

    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
        return $candidate
    }

    throw "firebase-admin.json is missing from the secrets folder."
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

function Reset-ObsoletePushSchemaIfNeeded {
    Set-Status "Checking push database schema..."

    $schemaCheckSql = @"
SELECT CASE
    WHEN to_regclass('public.push_devices') IS NULL THEN 'EMPTY'
    WHEN EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'push_devices'
          AND column_name = 'routing_id'
    ) THEN 'CURRENT'
    ELSE 'OBSOLETE'
END;
"@

    $composeFileArguments = $script:ComposeFileArguments
    $output = & $script:Docker compose `
        --env-file $runtimeEnvironmentPath `
        @composeFileArguments `
        exec -T `
        push-database `
        psql `
        -v ON_ERROR_STOP=1 `
        -At `
        -U sparrow_push `
        -d sparrow_push `
        -c $schemaCheckSql 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect the push database schema."
    }

    $schemaState = ($output | Out-String).Trim()
    Write-Log "push database schema: $schemaState"

    if ($schemaState -ne "OBSOLETE") {
        return
    }

    Set-Status "Resetting obsolete push database schema..."
    Write-Detail "Detected the pre-routing-id push schema. Resetting only push runtime data."

    $resetSql = @"
DROP TABLE IF EXISTS push_wake_ups CASCADE;
DROP TABLE IF EXISTS pending_envelopes CASCADE;
DROP TABLE IF EXISTS push_devices CASCADE;
"@

    $resetOutput = & $script:Docker compose `
        --env-file $runtimeEnvironmentPath `
        @composeFileArguments `
        exec -T `
        push-database `
        psql `
        -v ON_ERROR_STOP=1 `
        -U sparrow_push `
        -d sparrow_push `
        -c $resetSql 2>&1

    foreach ($line in $resetOutput) {
        Write-Log "push database reset: $line"
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Could not reset the obsolete push database schema."
    }

    Write-Detail "Obsolete push database schema removed. The push service will create the current schema."
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
    $config = Initialize-NetworkConfiguration -Config $config

    foreach ($requiredValue in @("SPARROW_IMAGE_PREFIX", "SPARROW_IMAGE_TAG", "CONTROL_PLANE_ID")) {
        if (-not $config.ContainsKey($requiredValue) -or [string]::IsNullOrWhiteSpace($config[$requiredValue])) {
            throw "sparrow.conf is missing $requiredValue."
        }
    }

    $mode = Get-NetworkMode -Config $config
    $publicDomain = if ($mode -eq "public") { Resolve-PublicDomain -Config $config } else { $null }
    $script:ComposeFileArguments = @("-f", $composePath, "-f", $releaseComposePath)

    if ($mode -eq "public") {
        $script:ComposeFileArguments += @("-f", $productionComposePath)
    }

    Set-ProgressValue -Value 5
    Set-Status "Starting Docker Desktop..."
    $script:Docker = Ensure-Docker

    Write-Detail "Mode: $mode"
    Write-Detail "Image: $($config['SPARROW_IMAGE_PREFIX']):$($config['SPARROW_IMAGE_TAG'])"
    if ($mode -eq "public") {
        Write-Detail "Public control plane: https://$publicDomain"
    } else {
        Write-Detail "LAN control-plane port: $controlPlanePort"
    }

    Set-ProgressValue -Value 15
    Set-Status "Preparing Sparrow secrets..."
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
    Ensure-RegistryAuthority -Config $config

    $siteAddress = if ($mode -eq "public") { $publicDomain } else { ":80" }
    $runtime = @(
        "CONTROL_PLANE_PROJECT_NAME=sparrow-control-plane",
        "CONTROL_PLANE_ID=$($config['CONTROL_PLANE_ID'])",
        "CONTROL_PLANE_BIND_ADDRESS=0.0.0.0",
        "CONTROL_PLANE_HTTP_PORT=$controlPlanePort",
        "CONTROL_PLANE_SITE_ADDRESS=$siteAddress",
        "CONTROL_PLANE_DOMAIN=$publicDomain",
        "FIREBASE_ADMIN_CREDENTIALS=$($firebaseCredentials.Replace('\','/'))",
        "REGISTRY_AUTHORITY_IDENTITY_FILE=./secrets/registry-authority.identity",
        "REGISTRY_AUTHORITY_CERTIFICATE_FILE=./secrets/registry-authority-certificate.json",
        "NODE_REGISTRY_DATABASE_PASSWORD=$registryPassword",
        "PRESENCE_REDIS_PASSWORD=$presencePassword",
        "PUSH_DATABASE_PASSWORD=$pushPassword",
        "PUSH_INTERNAL_API_TOKEN=$pushToken",
        "NODE_REGISTRY_DATABASE_PASSWORD_FILE=./secrets/node-registry-database-password.txt",
        "PRESENCE_REDIS_PASSWORD_FILE=./secrets/presence-redis-password.txt",
        "PUSH_DATABASE_PASSWORD_FILE=./secrets/push-database-password.txt",
        "PUSH_INTERNAL_API_TOKEN_FILE=./secrets/push-internal-api-token.txt",
        "SPARROW_IMAGE_PREFIX=$($config['SPARROW_IMAGE_PREFIX'])",
        "SPARROW_IMAGE_TAG=$($config['SPARROW_IMAGE_TAG'])"
    )

    [System.IO.File]::WriteAllLines(
        $runtimeEnvironmentPath,
        $runtime,
        [System.Text.UTF8Encoding]::new($false)
    )

    Push-Location $deploymentDirectory

    try {
        Set-Status "Pulling Sparrow images..."
        Invoke-ComposeStreaming `
            -Arguments @("pull") `
            -Activity "Pulling Sparrow control-plane images"
        Set-ProgressValue -Value 55

        # Start stateful dependencies first. PostgreSQL keeps its original
        # role passwords in existing volumes, so those roles are synchronized
        # below. Redis reads requirepass only when the Redis process starts,
        # therefore recreate only that container so it always consumes the
        # current presence-redis-password secret while preserving its volume.
        Set-ProgressValue -Value 65
        Set-Status "Starting Sparrow databases..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "node-registry-database",
            "push-database"
        )

        Set-ProgressValue -Value 72
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
            -DatabaseUser "sparrow_registry" `
            -DatabaseName "sparrow_registry" `
            -Password $registryPassword

        Synchronize-PostgresPassword `
            -Service "push-database" `
            -DatabaseUser "sparrow_push" `
            -DatabaseName "sparrow_push" `
            -Password $pushPassword

        Reset-ObsoletePushSchemaIfNeeded

        Set-ProgressValue -Value 80
        Set-Status "Preparing registry signing storage..."
        Invoke-Compose -Arguments @(
            "run",
            "--rm",
            "--no-deps",
            "registry-signing-init"
        )

        Set-ProgressValue -Value 84
        Set-Status "Starting Sparrow services..."
        Invoke-Compose -Arguments @(
            "up",
            "-d",
            "--remove-orphans"
        )

        Set-ProgressValue -Value 90
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

    Set-ProgressValue -Value 100
    $title.Text = "Sparrow control plane is running"
    $status.Text = $url
    Write-Log "SUCCESS: $url"

    [System.Windows.Forms.Application]::DoEvents()
    Start-Sleep -Seconds 3

    $form.Close()
    exit 0
} catch {
    Fail $_.Exception.Message
}
