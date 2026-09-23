[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$root = $PSScriptRoot
$form = [System.Windows.Forms.Form]::new()
$form.Text = 'Sparrow Server — Installer & Manager'
$form.Size = [System.Drawing.Size]::new(850, 925)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'Sizable'
$form.MinimumSize = [System.Drawing.Size]::new(800, 895)
$form.TopMost = $false

function New-Label([string]$Text, [int]$X, [int]$Y, [int]$Width = 210) {
    $label = [System.Windows.Forms.Label]::new()
    $label.Text = $Text; $label.Location = [System.Drawing.Point]::new($X, $Y)
    $label.Size = [System.Drawing.Size]::new($Width, 22)
    $form.Controls.Add($label)
    return $label
}
function New-Input([int]$X, [int]$Y, [int]$Width = 550) {
    $input = [System.Windows.Forms.TextBox]::new()
    $input.Location = [System.Drawing.Point]::new($X, $Y)
    $input.Size = [System.Drawing.Size]::new($Width, 24)
    $input.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
    $form.Controls.Add($input)
    return $input
}
function New-Button([string]$Text, [int]$X, [int]$Y, [int]$Width) {
    $button = [System.Windows.Forms.Button]::new()
    $button.Text = $Text; $button.Location = [System.Drawing.Point]::new($X, $Y)
    $button.Size = [System.Drawing.Size]::new($Width, 32)
    $form.Controls.Add($button)
    return $button
}
function Read-Settings([string]$Path) {
    $values = @{}
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        foreach ($line in (Get-Content -LiteralPath $Path)) {
            if ($line -match '^([A-Z_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
        }
    }
    return $values
}
$nodeSettings = Read-Settings (Join-Path $root 'community-node\sparrow.conf')
$cpSettings = Read-Settings (Join-Path $root 'control-plane\sparrow.conf')

$heading = New-Label 'Sparrow Server — Control Plane + Community Node' 22 12 780
$heading.Font = [System.Drawing.Font]::new('Segoe UI', 12, [System.Drawing.FontStyle]::Bold)
New-Label 'Installed components' 22 52 | Out-Null
$components = [System.Windows.Forms.ComboBox]::new()
$components.Location = [System.Drawing.Point]::new(245, 48)
$components.Size = [System.Drawing.Size]::new(535, 28)
$components.DropDownStyle = 'DropDownList'
[void]$components.Items.Add('Control Plane + Community Node')
$components.SelectedIndex = 0; $form.Controls.Add($components)
New-Label 'Reachability' 22 88 | Out-Null
$mode = [System.Windows.Forms.ComboBox]::new()
$mode.Location = [System.Drawing.Point]::new(245, 84); $mode.Size = [System.Drawing.Size]::new(535, 28)
$mode.DropDownStyle = 'DropDownList'; [void]$mode.Items.AddRange([object[]]@('LAN', 'Public'))
$mode.SelectedIndex = if ($nodeSettings['MODE'] -eq 'public' -or $cpSettings['MODE'] -eq 'public') { 1 } else { 0 }
$form.Controls.Add($mode)
$nodeLabel = New-Label 'Community Node hostname (Public)' 22 124
$nodeDomain = New-Input 245 120 535
$nodeDomain.Text = [string]$nodeSettings['PUBLIC_DOMAIN']
$cpLabel = New-Label 'Control Plane hostname (Public)' 22 160
$cpDomain = New-Input 245 156 535
$cpDomain.Text = [string]$cpSettings['PUBLIC_DOMAIN']
$directoryLabel = New-Label 'Directory Server URL (HTTPS)' 22 196
$directoryUrl = New-Input 245 192 535
$directoryUrl.Text = if ($nodeSettings['CONTROL_PLANE_DIRECTORY_URL']) { [string]$nodeSettings['CONTROL_PLANE_DIRECTORY_URL'] } elseif ($cpSettings['CONTROL_PLANE_DIRECTORY_URL']) { [string]$cpSettings['CONTROL_PLANE_DIRECTORY_URL'] } else { [string]$env:CONTROL_PLANE_DIRECTORY_URL }
New-Label 'Image prefix' 22 232 | Out-Null
$imagePrefix = New-Input 245 228 535
$imagePrefix.Text = if ($cpSettings['SPARROW_IMAGE_PREFIX']) { $cpSettings['SPARROW_IMAGE_PREFIX'] } elseif ($nodeSettings['SPARROW_IMAGE_PREFIX']) { $nodeSettings['SPARROW_IMAGE_PREFIX'] } else { 'ghcr.io/cbgm/sparrow' }
New-Label 'Image tag' 22 268 | Out-Null
$imageTag = New-Input 245 264 535
$imageTag.Text = if ($cpSettings['SPARROW_IMAGE_TAG']) { $cpSettings['SPARROW_IMAGE_TAG'] } elseif ($nodeSettings['SPARROW_IMAGE_TAG']) { $nodeSettings['SPARROW_IMAGE_TAG'] } else { 'latest' }
New-Label 'Independent Firebase credentials' 22 304 | Out-Null
$firebase = New-Input 245 300 425
$firebase.ReadOnly = $true
$browse = New-Button 'Import...' 680 299 100
$browse.Add_Click({
    $picker = [System.Windows.Forms.OpenFileDialog]::new()
    $picker.Filter = 'Google service account (*.json)|*.json|All files (*.*)|*.*'
    if ($picker.ShowDialog($form) -eq [System.Windows.Forms.DialogResult]::OK) { $firebase.Text = $picker.FileName }
    $picker.Dispose()
})
$enablePush = [System.Windows.Forms.CheckBox]::new()
$enablePush.Text = 'Enable Firebase if authorized credentials are available'
$enablePush.Location = [System.Drawing.Point]::new(245, 332)
$enablePush.Size = [System.Drawing.Size]::new(530, 23)
$enablePush.Checked = $true
$form.Controls.Add($enablePush)
$hint = New-Label "Push is optional. Credentials need FCM permission on Sparrow's shared Android Firebase project." 22 358 790
$hint.ForeColor = [System.Drawing.Color]::DimGray
$autoDns = [System.Windows.Forms.CheckBox]::new()
$autoDns.Text = 'Caddy + automatic free public hostnames (no domain purchase; sslip.io DNS)'
$autoDns.Location = [System.Drawing.Point]::new(245, 382)
$autoDns.Size = [System.Drawing.Size]::new(550, 23)
$autoDns.Checked = (-not $nodeSettings['PUBLIC_DOMAIN'] -and -not $cpSettings['PUBLIC_DOMAIN'])
$form.Controls.Add($autoDns)
$reinstallPublic = New-Button 'Reinstall Public from scratch (ERASE existing TEST server)' 22 410 755
$reinstallPublic.Enabled = $false
$reinstallPublic.BackColor = [System.Drawing.Color]::MistyRose
$form.Controls.Add($reinstallPublic)

# Only routine operations belong in the main installer. Advanced migration,
# offline backups and diagnostic preflight remain available as explicit scripts.
$start = New-Button 'Install / Start' 22 448 160
$stop = New-Button 'Stop' 194 448 96
$statusButton = New-Button 'Status' 302 448 96
$logsButton = New-Button 'Logs' 410 448 96
$cancel = New-Button 'Cancel task' 518 448 140
# The public address fields are read from the INSTALLED config, never guessed
# from the current router IP. Copy controls remain available after Status.
$publicLabel = New-Label 'Configured public addresses (external reachability not verified)' 22 491 760
$publicLabel.ForeColor = [System.Drawing.Color]::DimGray
$cpEndpoint = New-Input 245 518 410
$cpEndpoint.ReadOnly = $true
$cpEndpointLabel = New-Label 'Control Plane HTTPS' 22 522
$cpCopy = New-Button 'Copy' 670 515 106
$nodeEndpoint = New-Input 245 554 410
$nodeEndpoint.ReadOnly = $true
$nodeEndpointLabel = New-Label 'Node HTTPS' 22 558
$nodeCopy = New-Button 'Copy' 670 551 106
$allCopy = New-Button 'Copy all (incl. WSS)' 518 593 258
function Refresh-PublicEndpoints {
    $currentNode = Read-Settings (Join-Path $root 'community-node\sparrow.conf')
    $currentCp = Read-Settings (Join-Path $root 'control-plane\sparrow.conf')
    $cpInstalled = Test-Path -LiteralPath (Join-Path $root 'control-plane\.env.runtime') -PathType Leaf
    $nodeInstalled = Test-Path -LiteralPath (Join-Path $root 'community-node\.env.runtime') -PathType Leaf
    $cpEndpoint.Text = if ($cpInstalled -and $currentCp['MODE'] -eq 'public' -and $currentCp['PUBLIC_DOMAIN']) { "https://$($currentCp['PUBLIC_DOMAIN'])" } else { '' }
    $nodeEndpoint.Text = if ($nodeInstalled -and $currentNode['MODE'] -eq 'public' -and $currentNode['PUBLIC_DOMAIN']) { "https://$($currentNode['PUBLIC_DOMAIN'])" } else { '' }
    $cpCopy.Enabled = [bool]$cpEndpoint.Text
    $nodeCopy.Enabled = [bool]$nodeEndpoint.Text
    $allCopy.Enabled = $cpCopy.Enabled -or $nodeCopy.Enabled
}
$cpCopy.Add_Click({ if ($cpEndpoint.Text) { [System.Windows.Forms.Clipboard]::SetText($cpEndpoint.Text) } })
$nodeCopy.Add_Click({ if ($nodeEndpoint.Text) { [System.Windows.Forms.Clipboard]::SetText($nodeEndpoint.Text) } })
$allCopy.Add_Click({
    $addresses = @()
    if ($cpEndpoint.Text) { $addresses += "Control Plane: $($cpEndpoint.Text)"; $addresses += "Signed directory: $($cpEndpoint.Text)/v1/nodes" }
    if ($nodeEndpoint.Text) { $addresses += "Community Node: $($nodeEndpoint.Text)"; $addresses += "Gateway WSS: $($nodeEndpoint.Text.Replace('https://','wss://'))/v1/gateway" }
    if ($addresses.Count) { [System.Windows.Forms.Clipboard]::SetText(($addresses -join "`r`n")) }
})
Refresh-PublicEndpoints
$attachmentFile = Join-Path $root '.sparrow-attached.json'
$cancel.Enabled = $false
$activity = New-Label 'Ready. Install / Start installs or refreshes selected servers.' 22 827 755
$progress = [System.Windows.Forms.ProgressBar]::new()
$progress.Location = [System.Drawing.Point]::new(22, 637)
$progress.Size = [System.Drawing.Size]::new(755, 14)
$progress.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$progress.Style = 'Marquee'; $progress.MarqueeAnimationSpeed = 0
$form.Controls.Add($progress)
$details = [System.Windows.Forms.TextBox]::new()
$details.Location = [System.Drawing.Point]::new(22, 662)
$details.Size = [System.Drawing.Size]::new(756, 154)
$details.Multiline = $true; $details.ScrollBars = 'Vertical'; $details.ReadOnly = $true
$details.WordWrap = $true; $details.Font = [System.Drawing.Font]::new('Consolas', 9)
$details.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Bottom -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($details)

$script:process = $null
$script:reader = $null
$script:logFile = $null
$script:errorLog = $null
$script:failureFile = $null
$script:resultFile = $null
$script:logPosition = [long]0
$script:utf8Decoder = [System.Text.Encoding]::UTF8.GetDecoder()
$script:pendingCarriageReturn = $false
$script:busy = $false
$script:activeAction = ''
$timer = [System.Windows.Forms.Timer]::new()
$timer.Interval = 350
function Append-Lines([string]$Value) {
    if ([string]::IsNullOrEmpty($Value)) { return }
    # WinForms TextBox needs CRLF for reliable multiline rendering. Docker and
    # PowerShell emit a mixture of LF, CRLF and standalone CR progress updates,
    # sometimes split across separate reads. Never double a split CRLF.
    if ($script:pendingCarriageReturn) {
        if ($Value.StartsWith("`n", [StringComparison]::Ordinal)) { $Value = $Value.Substring(1) }
        $Value = "`n" + $Value
        $script:pendingCarriageReturn = $false
    }
    if ($Value.EndsWith("`r", [StringComparison]::Ordinal)) {
        $Value = $Value.Substring(0, $Value.Length - 1)
        $script:pendingCarriageReturn = $true
    }
    $normal = $Value.Replace("`r`n", "`n").Replace("`r", "`n").Replace("`n", "`r`n")
    if ($normal.Length -eq 0) { return }
    $details.AppendText($normal)
    if ($details.TextLength -gt 110000) {
        $details.Text = $details.Text.Substring($details.TextLength - 80000)
    }
    $details.SelectionStart = $details.TextLength
    $details.ScrollToCaret()
}
function Append-WorkerErrors([string]$Raw) {
    if ([string]::IsNullOrWhiteSpace($Raw)) { return }
    # Windows PowerShell serializes non-text streams as CLIXML when its stderr
    # is redirected. Progress records are not application failures. Retain any
    # serialized error text without displaying the raw XML progress envelope.
    if ($Raw.TrimStart().StartsWith('#< CLIXML', [StringComparison]::Ordinal)) {
        $errors = [regex]::Matches($Raw, '(?s)<S S="Error">(?<message>.*?)</S>')
        foreach ($entry in $errors) {
            Append-Lines ([System.Net.WebUtility]::HtmlDecode($entry.Groups['message'].Value) + "`r`n")
        }
        return
    }
    Append-Lines $Raw
}
function Update-Selection {
    $hasAttachment = Test-Path -LiteralPath $attachmentFile -PathType Leaf
    $start.Text = if ($hasAttachment) { 'Start existing' } else { 'Install / Start' }
    if ($hasAttachment) { $activity.Text = 'Cutover attached (validated on every action).' }
    $isPublic = $mode.SelectedIndex -eq 1
    $nodeSelected = $true
    $cpSelected = $true
    $reinstallPublic.Enabled = -not $hasAttachment -and $components.SelectedIndex -eq 0 -and $isPublic
    $reinstallPublic.Visible = $isPublic
    $autoDns.Enabled = $isPublic -and -not $hasAttachment
    $autoDns.Visible = $isPublic
    $nodeDomain.Enabled = $isPublic -and $nodeSelected -and -not $autoDns.Checked
    $nodeLabel.Enabled = $nodeDomain.Enabled
    $cpDomain.Enabled = $isPublic -and $cpSelected -and -not $autoDns.Checked
    $cpLabel.Enabled = $cpDomain.Enabled
    $directoryUrl.Enabled = -not $hasAttachment
    $directoryLabel.Enabled = $directoryUrl.Enabled
    $firebase.Enabled = -not $hasAttachment -and $cpSelected -and $enablePush.Checked
    $browse.Enabled = $firebase.Enabled; $enablePush.Enabled = -not $hasAttachment -and $cpSelected
    foreach ($settingControl in @($mode,$nodeDomain,$cpDomain,$directoryUrl,$imagePrefix,$imageTag)) { if ($hasAttachment) { $settingControl.Enabled = $false } }
}
$autoDns.Add_CheckedChanged({ Update-Selection })
$enablePush.Add_CheckedChanged({ Update-Selection })
$mode.Add_SelectedIndexChanged({ Update-Selection })
$components.Add_SelectedIndexChanged({ Update-Selection })
Update-Selection
function Set-Busy([bool]$Busy) {
    $script:busy = $Busy
    foreach ($button in @($start, $stop, $statusButton, $logsButton)) { $button.Enabled = -not $Busy }
    $cancel.Enabled = $Busy
    $components.Enabled = -not $Busy
    $reinstallPublic.Enabled = -not $Busy -and -not (Test-Path -LiteralPath $attachmentFile -PathType Leaf) -and $components.SelectedIndex -eq 0 -and $mode.SelectedIndex -eq 1
    $progress.MarqueeAnimationSpeed = if ($Busy) { 20 } else { 0 }
}
function Receive-WorkerOutput {
    if ($null -eq $script:logFile -or -not (Test-Path -LiteralPath $script:logFile -PathType Leaf)) { return }
    try { $stream = [System.IO.File]::Open($script:logFile, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite) } catch { return }
    try {
        if ($stream.Length -lt $script:logPosition) { $script:logPosition = 0 }
        [void]$stream.Seek($script:logPosition, [System.IO.SeekOrigin]::Begin)
        $buffer = [byte[]]::new([Math]::Min(65536, [int]($stream.Length - $script:logPosition)))
        if ($buffer.Length -gt 0) {
            $count = $stream.Read($buffer, 0, $buffer.Length)
            $script:logPosition += $count
            # Decoder carries an unfinished UTF-8 character into the next read.
            $chars = [char[]]::new($count)
            $charCount = $script:utf8Decoder.GetChars($buffer, 0, $count, $chars, 0, $false)
            if ($charCount -gt 0) { Append-Lines ([string]::new($chars, 0, $charCount)) }
        }
    } finally { $stream.Dispose() }
}
$timer.Add_Tick({
    Receive-WorkerOutput
    if ($script:busy -and $null -ne $script:process) {
        $script:process.Refresh()
        if ($script:process.HasExited) {
            Receive-WorkerOutput
            if ($script:pendingCarriageReturn) {
                $script:pendingCarriageReturn = $false
                $details.AppendText("`r`n")
            }
            $errorLog = $script:errorLog
            if ($errorLog -and (Test-Path -LiteralPath $errorLog -PathType Leaf)) {
                Append-WorkerErrors (Get-Content -LiteralPath $errorLog -Raw)
            }
            $failureMessage = ''
            if ($script:failureFile -and (Test-Path -LiteralPath $script:failureFile -PathType Leaf)) {
                $failureMessage = ([string](Get-Content -LiteralPath $script:failureFile -Raw)).Trim()
                if ($failureMessage) { Append-Lines "`r`n$failureMessage`r`n" }
            }
            # Start-Process -PassThru sometimes yields a null ExitCode with
            # redirected output on Windows PowerShell 5.1. First try to read
            # the real code, then consult the explicit result from the worker.
            $exitCode = $null
            try {
                $script:process.WaitForExit()
                $exitCode = $script:process.ExitCode
            } catch {
                Append-Lines "Could not retrieve worker exit code: $($_.Exception.Message)`r`n"
            }
            $result = ''
            if ($script:resultFile -and (Test-Path -LiteralPath $script:resultFile -PathType Leaf)) {
                $result = ([string](Get-Content -LiteralPath $script:resultFile -Raw)).Trim()
            }
            # A nonzero exit code or an explicit FAILED result is a real failure.
            # For old workers without a result file, zero exit is sufficient.
            $success = (($result -eq 'SUCCESS' -and ($null -eq $exitCode -or $exitCode -eq 0)) -or
                        (-not $result -and $null -ne $exitCode -and $exitCode -eq 0))
            Set-Busy $false
            if ($success -and (Test-Path -LiteralPath $attachmentFile -PathType Leaf)) {
                $mode.SelectedIndex = 1
                Update-Selection
            }
            if ($success) {
                $activity.Text = 'Operation completed.'
            } elseif ($failureMessage) {
                $activity.Text = $failureMessage
            } elseif ($null -ne $exitCode -and $exitCode -ne 0) {
                $activity.Text = "Operation failed (exit $exitCode). See $($script:errorLog)"
            } elseif ($result -eq 'FAILED') {
                $activity.Text = "Operation failed. See $($script:errorLog)"
            } else {
                $activity.Text = 'Operation ended without a readable completion result. Inspect the log and Docker state; do not reinstall automatically.'
            }
            Refresh-PublicEndpoints
            Append-Lines "`r`n$($activity.Text)`r`n"
            $script:process.Dispose(); $script:process = $null
            if ($script:resultFile) { Remove-Item -LiteralPath $script:resultFile -Force -ErrorAction SilentlyContinue }

        }
    }
})
$timer.Start()
# Switching this disposable Combined TEST deployment from LAN to Public is a
# fresh install, not a migration. The reinstall button can also force a reset
# of an incomplete Public deployment (without guessing why it is incomplete).
function Test-NeedsPublicReinstall {
    $installed = $false
    foreach ($folder in @('community-node', 'control-plane')) {
        $rootPath = Join-Path $root $folder
        if (-not (Test-Path -LiteralPath (Join-Path $rootPath '.env.runtime') -PathType Leaf)) { continue }
        $installed = $true
        $settings = Read-Settings (Join-Path $rootPath 'sparrow.conf')
        if ($settings['MODE'] -ne 'public' -or $settings['SHARED_PROXY'] -ne 'true' -or
            [string]::IsNullOrWhiteSpace([string]$settings['PUBLIC_DOMAIN'])) { return $true }
    }
    # Absent or incomplete shared Caddy setup means this is not yet a normal
    # installed Combined Public deployment. Don't confuse a stopped proxy with
    # an absent one; a stopped existing Public install is non-destructive.
    if ($installed -and -not (Test-Path -LiteralPath (Join-Path $root 'public-proxy\Caddyfile') -PathType Leaf)) {
        return $true
    }
    return $false
}
function Start-Task([string]$Action) {
    if ($script:busy) { return }
    $hasAttachment = Test-Path -LiteralPath $attachmentFile -PathType Leaf
    $selection = 'Combined'
    if ($selection -eq 'Proxy' -and -not $hasAttachment) {
        [System.Windows.Forms.MessageBox]::Show('Attach a completed Step 8f cutover to manage its original shared proxy.', 'Sparrow Server') | Out-Null
        return
    }
    if ($hasAttachment -and $Action -in @('Stop','Restart') -and $selection -in @('Proxy','Combined')) {
        $answer = [System.Windows.Forms.MessageBox]::Show(
            'This operation will interrupt the shared public proxy, affecting ALL attached public hostnames. The original data and TLS volumes are preserved. Continue?',
            'Shared proxy — public endpoint interruption', [System.Windows.Forms.MessageBoxButtons]::YesNo)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    }
    # Install / Start is never destructive. An explicitly separate Reinstall
    # action with its own confirmation is the only path that can erase data.
    # Do not change $Action to Reinstall based on incomplete local metadata.
    if ($Action -in @('Start','Reinstall') -and -not (Test-Path -LiteralPath $attachmentFile -PathType Leaf) -and $mode.SelectedIndex -eq 1 -and -not $autoDns.Checked) {
        if (-not $nodeDomain.Text.Trim() -or -not $cpDomain.Text.Trim()) {
            [System.Windows.Forms.MessageBox]::Show('Enter a hostname or select Caddy automatic public hostnames (no owned domain).','Sparrow Server') | Out-Null
            return
        }
    }
    # Persist each operation's logs beside the installed Sparrow manager,
    # not in the Windows temporary directory. Never overwrite an older run.
    $logsDirectory = Join-Path $root 'logs'
    try { New-Item -ItemType Directory -Path $logsDirectory -Force -ErrorAction Stop | Out-Null }
    catch {
        [System.Windows.Forms.MessageBox]::Show("Cannot create Sparrow logs folder: $($_.Exception.Message)", 'Sparrow Server') | Out-Null
        return
    }
    if ($Action -eq 'Reinstall') {
        $answer = [System.Windows.Forms.MessageBox]::Show(
            'REINSTALL COMBINED PUBLIC SERVER FROM SCRATCH? All existing Sparrow TEST Docker databases, queued messages, server identities, generated secrets and Caddy certificates will be permanently erased. No LAN-to-Public migration. Android clients must trust the NEW server identities. Continue?',
            'DELETE SPARROW SERVER DATA', [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    }
    $workerAction = if ($Action -eq 'Reinstall') { 'Start' } else { $Action }
    $sessionId = [Guid]::NewGuid().ToString('N')
    $script:resultFile = Join-Path $logsDirectory "sparrow-server-ui-$sessionId-result.txt"
    $script:failureFile = Join-Path $logsDirectory "sparrow-server-ui-$sessionId-failure.log"
    $values = @{
        Action = $workerAction; Component = $selection; ExpectedState = 'Any'; ResultPath = $script:resultFile
        FailurePath = $script:failureFile
        Mode = $(if ($mode.SelectedIndex -eq 1) { 'public' } else { 'lan' })
        NodeDomain = $nodeDomain.Text.Trim()
        ControlPlaneDomain = $cpDomain.Text.Trim()
        PublicHostnameMode = $(if ($mode.SelectedIndex -eq 1 -and $autoDns.Checked) { 'CaddyAutomatic' } else { 'Manual' })
        DirectoryUrl = $directoryUrl.Text.Trim()
        ImagePrefix = $imagePrefix.Text.Trim()
        ImageTag = $imageTag.Text.Trim()
        FirebaseCredentialsPath = $firebase.Text.Trim()
        FirebaseMode = $(if ($enablePush.Checked) { 'Enabled' } else { 'Disabled' })
        ExistingDeployment = $(if ($Action -eq 'Reinstall') { 'ReplaceTestData' } else { 'Keep' })
        ReplacementConfirmation = $(if ($Action -eq 'Reinstall') { 'DELETE SPARROW SERVER DATA' } else { '' })
    }
    $script:activeAction = $Action
    $worker = Join-Path $root 'Invoke-SparrowServer.ps1'
    $arguments = @("& '$($worker.Replace("'", "''"))'")
    foreach ($entry in $values.GetEnumerator()) {
        $arguments += "-$($entry.Key) '$(([string]$entry.Value).Replace("'", "''"))'"
    }
    # Progress records are serialized as CLIXML in redirected PowerShell 5.1
    # stderr. Suppress them without suppressing real command failures.
    $command = '$ProgressPreference = ''SilentlyContinue''; ' + ($arguments -join ' ')
    $encoded = [Convert]::ToBase64String([System.Text.Encoding]::Unicode.GetBytes($command))
    # Distinct log files prevent concurrent manager windows from corrupting each
    # other's output or revealing an unrelated deployment's diagnostic stream.
    $script:logFile = Join-Path $logsDirectory "sparrow-server-ui-$sessionId-output.log"
    $stderrPath = Join-Path $logsDirectory "sparrow-server-ui-$sessionId-errors.log"
    $script:errorLog = $stderrPath
    Remove-Item -LiteralPath $script:logFile, $stderrPath, $script:resultFile, $script:failureFile -Force -ErrorAction SilentlyContinue
    $details.Clear(); $script:logPosition = 0
    Append-Lines "Sparrow log folder: $logsDirectory`r`n"
    $script:utf8Decoder = [System.Text.Encoding]::UTF8.GetDecoder()
    $script:pendingCarriageReturn = $false
    try {
        $script:process = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
            '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
            '-EncodedCommand', $encoded
        ) -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput $script:logFile -RedirectStandardError $stderrPath
        Set-Busy $true; $activity.Text = "$Action ($selection) ..."
    } catch {
        if ($null -ne $script:process) { $script:process.Dispose(); $script:process = $null }
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, 'Sparrow Server') | Out-Null
    }

}
$start.Add_Click({ Start-Task 'Start' })
$reinstallPublic.Add_Click({ Start-Task 'Reinstall' })
$stop.Add_Click({ Start-Task 'Stop' })
$statusButton.Add_Click({ Start-Task 'Status' })
$logsButton.Add_Click({ Start-Task 'Logs' })
$cancel.Add_Click({
    if (-not $script:busy -or $null -eq $script:process) { return }
    $answer = [System.Windows.Forms.MessageBox]::Show(
        'Stop the active deployment command? Some services may already have started. Existing volumes are not removed; select Install / Start again to resume.',
        'Cancel Sparrow operation', [System.Windows.Forms.MessageBoxButtons]::YesNo)
    if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    & taskkill.exe /PID $script:process.Id /T /F 2>$null | Out-Null
    $activity.Text = 'Cancelled. Existing data was not deleted. Rerun Install / Start to resume.'
})
$form.Add_FormClosing({
    if ($script:busy) {
        $choice = [System.Windows.Forms.MessageBox]::Show('A deployment is still running. Keep this manager open or minimize it until the command finishes. Close anyway and leave the deployment running?', 'Sparrow Server', [System.Windows.Forms.MessageBoxButtons]::YesNo)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { $_.Cancel = $true }
    }
})
[System.Windows.Forms.Application]::Run($form)
$timer.Stop(); $timer.Dispose()
