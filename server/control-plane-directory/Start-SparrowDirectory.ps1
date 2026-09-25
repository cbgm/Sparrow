[CmdletBinding()]
param(
    [switch]$Install,
    [string]$ServerRoot = '',
    [string]$DirectoryHostname = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$scriptFile = $PSCommandPath
$compose = Join-Path $root 'docker-compose.yml'
$privateFolder = Join-Path $root 'private'
$beginRoute = '# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE'
$endRoute = '# END SPARROW INDEPENDENT DIRECTORY ROUTE'

# Docker Compose writes ordinary build/progress information to stderr. On
# Windows PowerShell 5.1 that can become a terminating NativeCommandError when
# the installer's global ErrorActionPreference is Stop. Capture both streams
# without treating stderr alone as failure; the Docker exit code is authoritative.
function Invoke-DockerResult([string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $lines = @(& docker @Arguments 2>&1 | ForEach-Object { [string]$_ })
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    return [pscustomobject]@{ ExitCode = $code; Lines = $lines }
}
function Invoke-DockerChecked([string[]]$Arguments) {
    $result = Invoke-DockerResult $Arguments
    if ($result.ExitCode -ne 0) {
        $detail = (@($result.Lines | Select-Object -Last 100) -join "`n").Trim()
        if (-not $detail) { $detail = 'Docker returned no diagnostic output.' }
        throw "Docker command failed (exit $($result.ExitCode)): docker $($Arguments -join ' ')`n$detail"
    }
    return $result.Lines
}
function Check-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker Desktop is required. Start Docker Desktop and retry.' }
    $null = Invoke-DockerChecked @('info','--format','{{.ServerVersion}}')
}
function Validate-Hostname([string]$Name) {
    if ($Name -notmatch '^(?=.{4,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$' -or
        $Name.Contains('..') -or $Name -ne $Name.ToLowerInvariant()) {
        throw 'Enter a valid public DNS hostname, e.g. directory-1-2-3-4.sslip.io.'
    }
    return $Name
}
function Directory-Route([string]$Name) {
    return @"
$beginRoute
$Name {
    @directory_public path /health /.well-known/sparrow-directory /v1/control-planes /v1/registrations /v1/registrations/challenge
    handle @directory_public {
        reverse_proxy sparrow-central-directory:9080
    }
    handle {
        respond "Not found" 404
    }
}
$endRoute
"@
}
function Managed-Route([string]$Contents) {
    $startAt = $Contents.IndexOf($beginRoute, [StringComparison]::Ordinal)
    $endAt = $Contents.IndexOf($endRoute, [StringComparison]::Ordinal)
    if (($startAt -ge 0) -ne ($endAt -ge 0)) { throw 'Existing Caddyfile has an incomplete Directory route; unchanged.' }
    if ($startAt -lt 0) { return '' }
    if ($endAt -le $startAt -or
        $Contents.IndexOf($beginRoute, $startAt + $beginRoute.Length, [StringComparison]::Ordinal) -ge 0 -or
        $Contents.IndexOf($endRoute, $endAt + $endRoute.Length, [StringComparison]::Ordinal) -ge 0) {
        throw 'Existing Caddyfile has multiple/ambiguous Directory routes; unchanged.'
    }
    return $Contents.Substring($startAt, $endAt + $endRoute.Length - $startAt)
}
function Preserve-InstalledProxyRoute([string]$CombinedRoot) {
    # The existing Windows GUI regenerates Caddyfile on Install / Start.
    # Add only the small route-preservation hook to that installed manager,
    # so the Directory site survives later combined-server updates.
    $manager = Join-Path $CombinedRoot 'Invoke-SparrowServer.ps1'
    if (-not (Test-Path -LiteralPath $manager -PathType Leaf)) {
        throw 'Existing Windows Sparrow Server manager not found; cannot guarantee persistent Directory route.'
    }
    $source = [IO.File]::ReadAllText($manager)
    if ($source.Contains('# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE')) { return }
    $old = @'
    $caddyfile = Join-Path $proxyRoot 'Caddyfile'
    $temp = "$caddyfile.pending"
    [System.IO.File]::WriteAllText($temp, (($routes + $entries) -join "`n"), [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temp -Destination $caddyfile -Force
'@
    $new = @'
    $caddyfile = Join-Path $proxyRoot 'Caddyfile'
    # Preserve the operator-managed Directory hostname across ordinary updates.
    $directoryRoute = ''
    if (Test-Path -LiteralPath $caddyfile -PathType Leaf) {
        $previous = [System.IO.File]::ReadAllText($caddyfile)
        $startMarker = '# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE'
        $endMarker = '# END SPARROW INDEPENDENT DIRECTORY ROUTE'
        $startAt = $previous.IndexOf($startMarker, [StringComparison]::Ordinal)
        $endAt = $previous.IndexOf($endMarker, [StringComparison]::Ordinal)
        if (($startAt -ge 0) -ne ($endAt -ge 0)) {
            throw 'Incomplete managed Directory route in Caddyfile. Existing proxy preserved.'
        }
        if ($startAt -ge 0) {
            if ($endAt -le $startAt -or
                $previous.IndexOf($startMarker, $startAt + $startMarker.Length, [StringComparison]::Ordinal) -ge 0 -or
                $previous.IndexOf($endMarker, $endAt + $endMarker.Length, [StringComparison]::Ordinal) -ge 0) {
                throw 'Ambiguous managed Directory route in Caddyfile. Existing proxy preserved.'
            }
            $directoryRoute = $previous.Substring($startAt, $endAt + $endMarker.Length - $startAt)
        }
    }
    $temp = "$caddyfile.pending"
    $normalRoutes = ($routes + $entries) -join "`n"
    $newContent = if ($directoryRoute) { $normalRoutes.TrimEnd() + "`n`n" + $directoryRoute + "`n" } else { $normalRoutes }
    [System.IO.File]::WriteAllText($temp, $newContent, [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temp -Destination $caddyfile -Force
'@
    if ($source.IndexOf($old, [StringComparison]::Ordinal) -lt 0 -or
        $source.IndexOf($old, $source.IndexOf($old, [StringComparison]::Ordinal) + $old.Length,
                        [StringComparison]::Ordinal) -ge 0) {
        throw 'Unknown installed Sparrow proxy-manager version. No Directory route added; use the accompanying source patch.'
    }
    $backup = "$manager.before-directory"
    if (-not (Test-Path -LiteralPath $backup)) {
        [IO.File]::WriteAllText($backup,$source,[Text.UTF8Encoding]::new($false))
    }
    [IO.File]::WriteAllText($manager,$source.Replace($old,$new),[Text.UTF8Encoding]::new($false))
}

function Test-StandalonePortsAvailable {
    # Two unrelated Docker projects cannot both own the same public IP:443.
    # When the unified proxy exists, use the OPTIONAL shared-edge mode instead.
    $running = @(Invoke-DockerChecked @('ps','--format','{{.Names}} {{.Ports}}'))
    foreach ($line in $running) {
        if ($line -match '(?:0\.0\.0\.0|\[::\]):(?:80|443)->' -and
            $line -notmatch '^sparrow-central-directory-caddy(?:\s|$)') {
            throw "TCP 80/443 already belong to another container ($line). For one public IP use the optional existing-proxy integration, or host the Directory on a separate machine/IP. No existing container was changed."
        }
    }
    $owner = @(Invoke-DockerChecked @('ps','--filter','name=^/sparrow-central-directory-caddy$', '--format','{{.Names}}'))
    if ($owner.Count -eq 0) {
        $listeners = @(Get-NetTCPConnection -LocalPort 80,443 -State Listen -ErrorAction SilentlyContinue)
        if ($listeners.Count -gt 0) {
            throw 'TCP 80/443 is already in use on this host. Directory standalone HTTPS cannot bind the same public IP/port as another proxy.'
        }
    }
}
function Start-StandaloneDirectoryProxy([string]$Hostname) {
    $caddy = Join-Path $privateFolder 'Caddyfile'
    $content = @"
$Hostname {
    @directory_public path /health /.well-known/sparrow-directory /v1/control-planes /v1/registrations /v1/registrations/challenge
    handle @directory_public {
        reverse_proxy directory:9080
    }
    handle { respond "Not found" 404 }
}
"@
    if (Test-Path -LiteralPath $caddy -PathType Leaf) {
        $prior = [IO.File]::ReadAllText($caddy)
        if ($prior -ne $content) {
            $oldHostFile = Join-Path $privateFolder 'directory-host.txt'
            if ((Test-Path -LiteralPath $oldHostFile) -and
                ([IO.File]::ReadAllText($oldHostFile).Trim() -ne $Hostname)) {
                # Changing a hostname reuses the existing Directory identity.
                Write-Output 'Updating standalone Directory HTTPS hostname (signing key unchanged).'
            } elseif ($prior -notmatch [regex]::Escape($Hostname)) {
                throw 'Unrecognized existing standalone Caddy configuration; refusing overwrite.'
            }
        }
    }
    [IO.File]::WriteAllText($caddy,$content,[Text.UTF8Encoding]::new($false))
    $null = Invoke-DockerChecked @('compose','-f',$compose,'--profile','standalone','up','-d','--no-deps','directory-caddy')
    Write-Output 'Standalone Directory Caddy started. DNS, router forwarding and certificate issuance still require external verification.'
}
function Install-Directory([string]$CombinedRoot,[string]$Hostname) {
    Check-Docker
    $Hostname = Validate-Hostname $Hostname
    $standalone = [string]::IsNullOrWhiteSpace($CombinedRoot)
    # A Directory never requires a running Control Plane or Community Node.
    # Reuse the existing shared public proxy only when the operator explicitly
    # selects a server folder. Standalone mode owns its OWN HTTPS proxy.
    $networks = @(Invoke-DockerChecked @('network','ls','--format','{{.Name}}'))
    if ('sparrow-public-edge' -notin $networks) {
        $null = Invoke-DockerChecked @('network','create','sparrow-public-edge')
    }
    $cpConf = ''
    $nodeConf = ''
    $current = ''
    $currentRoute = ''
    $proxyId = ''
    if ($standalone) {
        Test-StandalonePortsAvailable
    } else {
        $CombinedRoot = [IO.Path]::GetFullPath($CombinedRoot)
        $proxyFolder = Join-Path $CombinedRoot 'public-proxy'
        $caddyFile = Join-Path $proxyFolder 'Caddyfile'
        $cpConf = Join-Path $CombinedRoot 'control-plane/sparrow.conf'
        $nodeConf = Join-Path $CombinedRoot 'community-node/sparrow.conf'
        foreach ($path in @($caddyFile,$cpConf,$nodeConf)) {
            if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
                throw "Optional shared-proxy mode requires the existing combined server folder. Missing: $path"
            }
        }
        $proxyIds = @(Invoke-DockerChecked @('ps','--filter','label=com.docker.compose.project=sparrow-public-proxy',
                                   '--filter','label=com.docker.compose.service=caddy','--format','{{.ID}}') |
                      Where-Object { $_ -and $_.Trim() })
        if ($proxyIds.Count -ne 1) {
            throw 'The optional combined-server proxy is not running. Clear the server folder to install Directory standalone, provided public ports 80/443 are free.'
        }
        $proxyId = [string]$proxyIds[0]
        $current = [IO.File]::ReadAllText($caddyFile)
        if ($current -notmatch '(?m)^\s*import sparrow_control_routes\s*$' -or
            $current -notmatch '(?m)^\s*import sparrow_community_routes\s*$') {
            throw 'This is not a recognized combined Sparrow Caddyfile. No changes made.'
        }
        foreach ($config in @($cpConf,$nodeConf)) {
            $hostLine = @(Get-Content -LiteralPath $config | Where-Object { $_ -match '^PUBLIC_DOMAIN=' } | Select-Object -First 1)
            if ($hostLine.Count -eq 1 -and $Hostname -eq ($hostLine[0] -replace '^PUBLIC_DOMAIN=', '').Trim()) {
                throw 'Directory needs its own DNS hostname, distinct from Control Plane and Node.'
            }
        }
        $currentRoute = Managed-Route $current
    }
    New-Item -ItemType Directory -Force -Path $privateFolder | Out-Null
    $configHost = Join-Path $privateFolder 'directory-host.txt'
    $previousHost = if (Test-Path -LiteralPath $configHost) { [IO.File]::ReadAllText($configHost).Trim() } else { '' }
    if (-not $standalone) {
        if ($currentRoute -and $previousHost -and
            $currentRoute -ne (Directory-Route $previousHost) -and
            $currentRoute -ne (Directory-Route $previousHost).Replace(' /.well-known/sparrow-directory','')) {
            throw 'Existing Directory proxy route differs from the last installer configuration; unchanged.'
        }
        if ($currentRoute -and -not $previousHost -and $currentRoute -ne (Directory-Route $Hostname) -and
            $currentRoute -ne (Directory-Route $Hostname).Replace(' /.well-known/sparrow-directory','')) {
            throw 'Existing Directory route is not owned by this installation; unchanged.'
        }
    }
    $adminEnv = Join-Path $privateFolder 'admin.env'
    if (-not (Test-Path -LiteralPath $adminEnv -PathType Leaf)) {
        $entropy = New-Object byte[] 48
        [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($entropy)
        $secret = [Convert]::ToBase64String($entropy)
        [IO.File]::WriteAllText($adminEnv, "SPARROW_DIRECTORY_ADMIN_TOKEN=$secret`n", [Text.UTF8Encoding]::new($false))
    }
    Write-Output 'Building the independent Directory container...'
    $null = Invoke-DockerChecked @('compose','-f',$compose,'build','--progress=plain','directory')
    # A named Linux volume persists both the identity and SQLite database.
    # The runtime Compose service drops ALL Linux capabilities. Even a one-off
    # `compose run --user 0:0` inherits cap_drop: ALL, so root cannot chown a
    # fresh Docker named volume. Use a separate, disposable initialization
    # container with only the capabilities necessary to prepare that volume.
    # The long-running Directory container remains non-root with cap_drop: ALL.
    # Never replace or delete the volume: it may already hold the signing key.
    Write-Output 'Preparing persistent directory storage...'
    $null = Invoke-DockerChecked @('run','--rm','--user','0:0',
       '--cap-drop','ALL','--cap-add','CHOWN','--cap-add','FOWNER',
       '--cap-add','DAC_OVERRIDE',
       '--mount','type=volume,source=sparrow-central-directory-state,target=/state',
       '--entrypoint','/bin/sh','sparrow-central-directory:local','-c',
       'mkdir -p /state && chown -R 10001:10001 /state && chmod 700 /state')
    # Only create a key if it truly does not exist. A broken existing key must
    # stop installation, NOT be silently replaced with a new signing identity.
    $keyPresence = Invoke-DockerResult @('compose','-f',$compose,'run','--rm','--no-deps',
        '--entrypoint','/bin/sh','directory','-c','test -e /state/signing.pem')
    if ($keyPresence.ExitCode -eq 1) {
        Write-Output 'Generating the Directory signing identity (once)...'
        # O_EXCL in init-key prevents overwriting any existing key.
        $null = Invoke-DockerChecked @('compose','-f',$compose,'run','--rm','--no-deps','directory',
                         'init-key','--key-file','/state/signing.pem')
    } elseif ($keyPresence.ExitCode -ne 0) {
        throw "Cannot check the existing Directory key (exit $($keyPresence.ExitCode)):`n$(@($keyPresence.Lines) -join "`n")"
    }
    $keyOutput = @(Invoke-DockerChecked @('compose','-f',$compose,'run','--rm','--no-deps','directory',
                            'show-public-key','--key-file','/state/signing.pem'))
    $publicKey = @($keyOutput | Where-Object { $_ -match '^[A-Za-z0-9_-]{58,100}$' } | Select-Object -Last 1)
    if ($publicKey.Count -ne 1) { throw 'Could not read a valid Directory public verification key; no Caddy changes made.' }
    # A locally generated Directory is the trusted source of this initial pin.
    # Never overwrite a different previously pinned Directory signing identity.
    foreach ($config in @(if (-not $standalone) { $cpConf; $nodeConf })) {
        $oldPin = @(Get-Content -LiteralPath $config | Where-Object { $_ -match '^CONTROL_PLANE_DIRECTORY_PUBLIC_KEY=' } | Select-Object -First 1)
        if ($oldPin.Count -eq 1) {
            $value = ([string]$oldPin[0]).Substring('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY='.Length).Trim()
            if ($value -and $value -ne [string]$publicKey[0]) {
                throw "A different Directory signing key is already pinned in $config. Refusing silent trust-key replacement."
            }
        }
    }
    Write-Output 'Starting the separate directory service...'
    $null = Invoke-DockerChecked @('compose','-f',$compose,'up','-d','--no-deps','directory')
    if ($standalone) {
        Start-StandaloneDirectoryProxy $Hostname
    } else {
    $newRoute = Directory-Route $Hostname
    $updated = if ($currentRoute) { $current.Replace($currentRoute,$newRoute) }
               else { $current.TrimEnd() + "`n`n" + $newRoute + "`n" }
    # Do this before touching the Caddyfile so a future combined Install / Start
    # cannot accidentally remove the independently hosted Directory endpoint.
    Preserve-InstalledProxyRoute $CombinedRoot
    if ($updated -ne $current) {
        Write-Output 'Adding the Directory hostname to the existing Caddy proxy...'
        # Preserve an exact rollback copy. Never restart Control Plane/Node.
        $backup = "$caddyFile.before-directory"
        [IO.File]::WriteAllText($backup,$current,[Text.UTF8Encoding]::new($false))
        [IO.File]::WriteAllText($caddyFile,$updated,[Text.UTF8Encoding]::new($false))
        try {
            $null = Invoke-DockerChecked @('exec',$proxyId,'caddy','validate','--config','/etc/caddy/Caddyfile')
            $null = Invoke-DockerChecked @('exec',$proxyId,'caddy','reload','--config','/etc/caddy/Caddyfile')
        } catch {
            [IO.File]::WriteAllText($caddyFile,$current,[Text.UTF8Encoding]::new($false))
            try { $null = Invoke-DockerChecked @('exec',$proxyId,'caddy','reload','--config','/etc/caddy/Caddyfile') } catch {}
            throw
        }
    }
    }
    [IO.File]::WriteAllText($configHost,"$Hostname`n",[Text.UTF8Encoding]::new($false))
    $info = @{ url = "https://$Hostname"; publicKey = [string]$publicKey[0]; serverRoot = $CombinedRoot }
    # Provision only the public bootstrap URL and public key for the EXISTING
    # combined server. A normal server Install / Start applies these to its
    # running registration/sync clients; identities and secrets remain intact.
    foreach ($config in @(if (-not $standalone) { $cpConf; $nodeConf })) {
        $original = [IO.File]::ReadAllText($config)
        $revised = $original
        foreach ($pair in @(@('CONTROL_PLANE_DIRECTORY_URL',$info.url),
                           @('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY',$info.publicKey))) {
            $property = [regex]::Escape([string]$pair[0])
            $line = [string]$pair[0] + '=' + [string]$pair[1]
            if ($revised -match "(?m)^$property=") {
                $revised = [regex]::Replace($revised,"(?m)^$property=[^`r`n]*",$line)
            } else {
                $revised = $revised.TrimEnd() + "`n" + $line + "`n"
            }
        }
        if ($revised -ne $original) {
            [IO.File]::WriteAllText("$config.before-directory",$original,[Text.UTF8Encoding]::new($false))
            [IO.File]::WriteAllText($config,$revised,[Text.UTF8Encoding]::new($false))
        }
    }
    [IO.File]::WriteAllText((Join-Path $privateFolder 'directory-public.json'),
         ($info | ConvertTo-Json -Compress),[Text.UTF8Encoding]::new($false))
    Write-Output 'Independent Directory service is running (no dependency on the Control Plane or Community Node).'
    Write-Output "Directory URL: $($info.url)"
    Write-Output "Directory public verification key (informational): $($info.publicKey)"
    if (-not $standalone) {
        Write-Output 'Optional shared Caddy route was updated. Control Plane / Node containers were not reinstalled.'
        Write-Output 'Combined server discovery URL/public key were configured for server-side registration.'
    }
    Write-Output 'Android retrieves and pins the Directory public key automatically over validated HTTPS. No key in local.properties is required.'
}

if ($Install) {
    try { Install-Directory $ServerRoot $DirectoryHostname }
    catch { [Console]::Error.WriteLine(('Directory installation failed: ' + $_.Exception.Message)); exit 1 }
    exit 0
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Check-Docker
$form = [Windows.Forms.Form]::new()
$form.Text = 'Sparrow — Independent Directory Installer'
$form.Size = [Drawing.Size]::new(760,570)
$form.MinimumSize = [Drawing.Size]::new(700,520)
$form.StartPosition = 'CenterScreen'
$form.TopMost = $false
$label = [Windows.Forms.Label]::new()
$label.Location = [Drawing.Point]::new(18,15)
$label.Size = [Drawing.Size]::new(705,45)
$label.Text = 'Independent Directory installer. Leave server folder empty for standalone HTTPS; select one only to reuse its Caddy on the same public IP.'
$form.Controls.Add($label)
$serverLabel = [Windows.Forms.Label]::new()
$serverLabel.Location = [Drawing.Point]::new(18,71)
$serverLabel.Size = [Drawing.Size]::new(690,19)
$serverLabel.Text = 'Optional combined Sparrow Server folder (leave EMPTY for standalone Directory + own Caddy)'
$form.Controls.Add($serverLabel)
$serverInput = [Windows.Forms.TextBox]::new()
$serverInput.Location = [Drawing.Point]::new(18,94)
$serverInput.Size = [Drawing.Size]::new(595,24)
$serverInput.Text = ''  # Standalone is the default. Existing proxy use is an explicit choice.
$form.Controls.Add($serverInput)
$browse = [Windows.Forms.Button]::new()
$browse.Text = 'Browse...'
$browse.Location = [Drawing.Point]::new(622,92)
$browse.Size = [Drawing.Size]::new(103,29)
$browse.Add_Click({
    $selector = [Windows.Forms.FolderBrowserDialog]::new()
    $selector.Description = 'Optional: choose the installed combined Sparrow server folder to share one HTTPS proxy'
    if ($selector.ShowDialog($form) -eq [Windows.Forms.DialogResult]::OK) { $serverInput.Text = $selector.SelectedPath }
    $selector.Dispose()
})
$form.Controls.Add($browse)
$domainLabel = [Windows.Forms.Label]::new()
$domainLabel.Location = [Drawing.Point]::new(18,138)
$domainLabel.Size = [Drawing.Size]::new(705,19)
$domainLabel.Text = 'Directory public hostname (auto-generated from public IPv4; editable)'
$form.Controls.Add($domainLabel)
$domainInput = [Windows.Forms.TextBox]::new()
$domainInput.Location = [Drawing.Point]::new(18,162)
$domainInput.Size = [Drawing.Size]::new(707,24)
$storedHost = Join-Path $privateFolder 'directory-host.txt'
if (Test-Path -LiteralPath $storedHost) {
    $domainInput.Text = [IO.File]::ReadAllText($storedHost).Trim()
} else {
    try {
        $ipv4 = [string](Invoke-RestMethod -Uri 'https://api.ipify.org' -TimeoutSec 8)
        $address = [Net.IPAddress]::Parse($ipv4.Trim())
        if ($address.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork) {
            $domainInput.Text = 'directory-' + $ipv4.Trim().Replace('.','-') + '.sslip.io'
        }
    } catch { }
}
$form.Controls.Add($domainInput)
$installButton = [Windows.Forms.Button]::new()
$installButton.Text = 'Install / Start Directory'
$installButton.Location = [Drawing.Point]::new(18,204)
$installButton.Size = [Drawing.Size]::new(220,36)
$form.Controls.Add($installButton)
$approveButton = [Windows.Forms.Button]::new()
$approveButton.Text = 'Check registered planes'
$approveButton.Location = [Drawing.Point]::new(253,204)
$approveButton.Size = [Drawing.Size]::new(226,36)
$form.Controls.Add($approveButton)
$statusButton = [Windows.Forms.Button]::new()
$statusButton.Text = 'Docker status'
$statusButton.Location = [Drawing.Point]::new(495,204)
$statusButton.Size = [Drawing.Size]::new(115,36)
$form.Controls.Add($statusButton)
$logBox = [Windows.Forms.TextBox]::new()
$logBox.Multiline = $true
$logBox.ScrollBars = 'Vertical'
$logBox.ReadOnly = $true
$logBox.Location = [Drawing.Point]::new(18,258)
$logBox.Size = [Drawing.Size]::new(707,245)
$logBox.Anchor = [Windows.Forms.AnchorStyles]::Top -bor [Windows.Forms.AnchorStyles]::Bottom -bor [Windows.Forms.AnchorStyles]::Left -bor [Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($logBox)
$installButton.Add_Click({
    $installButton.Enabled = $false
    $logBox.AppendText("Starting independent Directory installer...`r`n")
    $script = $scriptFile
    $job = Start-Job -ScriptBlock {
        param($scriptPath,$installedRoot,$hostname)
        & powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File $scriptPath -Install -ServerRoot $installedRoot -DirectoryHostname $hostname 2>&1
        if ($LASTEXITCODE -ne 0) { throw "Directory installation failed (exit $LASTEXITCODE)." }
    } -ArgumentList $script,$serverInput.Text.Trim(),$domainInput.Text.Trim()
    $lastOutput = 0
    $timer = [Windows.Forms.Timer]::new()
    $timer.Interval = 700
    $timer.Add_Tick({
        $all = @(Receive-Job -Job $job -Keep -ErrorAction SilentlyContinue)
        for ($idx=$lastOutput; $idx -lt $all.Count; $idx++) {
            $logBox.AppendText(([string]$all[$idx]) + "`r`n")
        }
        $lastOutput = $all.Count
        if ($job.State -in @('Completed','Failed','Stopped')) {
            $timer.Stop(); $timer.Dispose()
            $installButton.Enabled = $true
            if ($job.State -ne 'Completed') { $logBox.AppendText("Installation failed. See details above.`r`n") }
            Remove-Job -Job $job -Force
        }
    }.GetNewClosure())
    $timer.Start()
}.GetNewClosure())
$statusButton.Add_Click({
    try {
        $result = @(Invoke-DockerChecked @('ps','-a','--filter','name=^/sparrow-central-directory$',
                                     '--format','{{.Names}} | {{.Status}}'))
        $logBox.AppendText(($result -join "`r`n") + "`r`n")
    } catch { $logBox.AppendText($_.Exception.Message + "`r`n") }
}.GetNewClosure())
$approveButton.Add_Click({
    try {
        $host = $domainInput.Text.Trim()
        if (-not $host) { throw 'Enter your Directory hostname first.' }
        $response = Invoke-RestMethod -Uri "https://$host/v1/control-planes" -TimeoutSec 10
        $planes = @($response.payload.controlPlanes)
        if ($planes.Count -eq 0) {
            $logBox.AppendText("Directory online; no verified Control Planes registered yet.`r`n")
        } else {
            foreach ($plane in $planes) {
                $logBox.AppendText("Registered: $($plane.baseUrl) ($($plane.controlPlaneId))`r`n")
            }
        }
    } catch { $logBox.AppendText("Directory list unavailable: $($_.Exception.Message)`r`n") }
}.GetNewClosure())
[void]$form.ShowDialog()
$form.Dispose()
