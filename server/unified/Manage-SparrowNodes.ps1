[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$script:nodeScript = Join-Path $root 'Manage-SparrowNodes.py'
$script:instanceRoot = Join-Path $root 'node-instances'
$script:deploymentRoot = $root
$script:detectedPlane = ''
$script:detectedMode = ''
$script:detectionError = ''
# Read-only detection from existing Docker Compose ownership. This works when
# the updated manager is in a NEW bundle and Combined remains in its ORIGINAL
# installation directory. Python uses that original root for its node state.
$pythonForDetection = Get-Command python3 -ErrorAction SilentlyContinue
if (-not $pythonForDetection) { $pythonForDetection = Get-Command python -ErrorAction SilentlyContinue }
if ($pythonForDetection) {
    try {
        $discovery = & $pythonForDetection.Source $script:nodeScript info 2>&1
        if ($LASTEXITCODE -ne 0) { throw (($discovery | Out-String).Trim()) }
        $details = ($discovery | Out-String | ConvertFrom-Json -ErrorAction Stop)
        $script:deploymentRoot = [string]$details.deploymentRoot
        $script:instanceRoot = [string]$details.instancesRoot
        $script:detectedPlane = [string]$details.controlPlaneUrl
        $script:detectedMode = [string]$details.mode
    } catch {
        $script:detectionError = $_.Exception.Message
        # The short red status label previously truncated the only useful
        # error message. Fetch a READ-ONLY, copyable report of Docker ownership
        # and missing original files; never print runtime contents or secrets.
        try {
            $diagnosticOutput = & $pythonForDetection.Source $script:nodeScript diagnose 2>&1
            if ($diagnosticOutput) {
                $script:detectionError += "`r`n`r`n" + ($diagnosticOutput | Out-String).Trim()
            }
        } catch {
            $script:detectionError += "`r`nDiagnostic collection failed: " + $_.Exception.Message
        }
    }
} else {
    $script:detectionError = 'Python 3 is required to locate and manage the existing installation.'
}
$script:worker = $null
$script:currentOutput = ''
$script:currentError = ''
$script:running = $false
$script:items = @()

$form = [System.Windows.Forms.Form]::new()
$form.Text = 'Sparrow — Community Node instances'
$form.Size = [System.Drawing.Size]::new(935, 795)
$form.MinimumSize = [System.Drawing.Size]::new(885, 745)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'Sizable'
$form.TopMost = $false
function Label([string]$name, [int]$y, [int]$x = 20, [int]$w = 195) {
    $c = [System.Windows.Forms.Label]::new()
    $c.Text = $name; $c.Location = [System.Drawing.Point]::new($x, $y)
    $c.Size = [System.Drawing.Size]::new($w, 24)
    $form.Controls.Add($c)
    return $c
}
function Input([int]$y, [int]$x = 225, [int]$w = 650) {
    $c = [System.Windows.Forms.TextBox]::new()
    $c.Location = [System.Drawing.Point]::new($x, $y)
    $c.Size = [System.Drawing.Size]::new($w, 24)
    $c.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
    $form.Controls.Add($c)
    return $c
}
function Button([string]$name, [int]$x, [int]$y, [int]$w) {
    $c = [System.Windows.Forms.Button]::new()
    $c.Text = $name; $c.Location = [System.Drawing.Point]::new($x, $y)
    $c.Size = [System.Drawing.Size]::new($w, 31)
    $form.Controls.Add($c)
    return $c
}
$headline = Label 'Independent Community Nodes — one container set and identity per instance' 12 20 875
$headline.Font = [System.Drawing.Font]::new('Segoe UI', 11, [System.Drawing.FontStyle]::Bold)
Label 'Node display name' 49 | Out-Null
$name = Input 46
Label 'Reachability' 83 | Out-Null
$mode = [System.Windows.Forms.ComboBox]::new()
$mode.DropDownStyle = 'DropDownList'
$mode.Location = [System.Drawing.Point]::new(225, 80)
$mode.Size = [System.Drawing.Size]::new(225, 26)
[void]$mode.Items.AddRange([object[]]@('LAN', 'Public'))
# Match the installed Combined deployment's mode when available.
$existingCombinedConf = Join-Path $script:deploymentRoot 'control-plane\sparrow.conf'
$existingCombinedRuntime = Join-Path $script:deploymentRoot 'control-plane\.env.runtime'
$combinedIsPublic = $false
if ($script:detectedMode -eq 'public' -and
    (Test-Path -LiteralPath $existingCombinedConf -PathType Leaf)) {
    $combinedIsPublic = [bool](Select-String -LiteralPath $existingCombinedConf -Pattern '^MODE=public$' -Quiet)
}
$mode.SelectedIndex = if ($combinedIsPublic) { 1 } else { 0 }
$form.Controls.Add($mode)
$automatic = [System.Windows.Forms.CheckBox]::new()
$automatic.Text = 'Automatic sslip.io public hostname'
$automatic.Location = [System.Drawing.Point]::new(465, 79)
$automatic.Size = [System.Drawing.Size]::new(330, 24)
$automatic.Enabled = $mode.SelectedIndex -eq 1
$form.Controls.Add($automatic)
Label 'Public node hostname' 117 | Out-Null
$hostName = Input 114
$hostName.Enabled = $false
Label 'Existing Control Plane URL' 151 | Out-Null
$plane = Input 148
# Ordinary node setup uses the existing Combined Control Plane. Signed
# directory configuration is optional and tucked away; verification pins are
# inherited only from an independently trusted, matching configuration.
$directoryOptions = [System.Windows.Forms.CheckBox]::new()
$directoryOptions.Text = 'Advanced: signed directory URL (optional)'
$directoryOptions.Location = [System.Drawing.Point]::new(20, 184)
$directoryOptions.Size = [System.Drawing.Size]::new(385, 24)
$form.Controls.Add($directoryOptions)
$directoryLabel = Label 'Directory Server HTTPS URL' 219
$directory = Input 216
$directoryLabel.Visible = $false
$directory.Visible = $false
$directoryOptions.Add_CheckedChanged({
    $directoryLabel.Visible = $directoryOptions.Checked
    $directory.Visible = $directoryOptions.Checked
})
$hint = Label 'Use your existing Control Plane. No new Control Plane or directory signing key is needed.' 251 20 880
$hint.ForeColor = [System.Drawing.Color]::DimGray
if ($script:detectionError) {
    $hint.Text = 'Could not verify existing deployment. See copyable details below.'
    $hint.Width = 665
    $hint.ForeColor = [System.Drawing.Color]::DarkRed
}
# Preselect the already running Combined Control Plane when available.
$combinedConf = Join-Path $script:deploymentRoot 'control-plane\sparrow.conf'
$combinedRuntime = Join-Path $script:deploymentRoot 'control-plane\.env.runtime'
function Read-NodeProperties([string]$path) {
    $values = @{}
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        foreach ($row in @(Get-Content -LiteralPath $path)) {
            if ($row -match '^([^#=]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
        }
    }
    return $values
}
$combinedSettings = Read-NodeProperties $combinedConf
$combinedRuntimeSettings = Read-NodeProperties $combinedRuntime
function Existing-PlaneUrl {
    if ($mode.SelectedIndex -eq 1 -and $combinedSettings['MODE'] -eq 'public' -and $combinedSettings['PUBLIC_DOMAIN']) {
        return 'https://' + $combinedSettings['PUBLIC_DOMAIN']
    }
    if ($mode.SelectedIndex -eq 0 -and $combinedSettings['MODE'] -eq 'lan' -and $combinedRuntimeSettings['ADVERTISED_CONTROL_PLANE_URLS']) {
        return ([string]$combinedRuntimeSettings['ADVERTISED_CONTROL_PLANE_URLS']).Split(',')[0].Trim()
    }
    return ''
}
$plane.Text = if ($script:detectedPlane) { $script:detectedPlane } else { Existing-PlaneUrl }
$automatic.Checked = $true
$mode.Add_SelectedIndexChanged({
    $isPublic = $mode.SelectedIndex -eq 1
    $automatic.Enabled = $isPublic
    $hostName.Enabled = $isPublic -and -not $automatic.Checked
    if (-not $plane.Text.Trim()) { $plane.Text = Existing-PlaneUrl }
})
$automatic.Add_CheckedChanged({ $hostName.Enabled = $mode.SelectedIndex -eq 1 -and -not $automatic.Checked })
# Always offer a direct clipboard action instead of relying on a MessageBox
# (which can be difficult to select/copy in the Windows desktop UI).
# A moved original installation still has Docker labels and bind mounts pointing
# to its OLD path. Explicitly create a junction only after the user selects the
# actual moved configured folder; the Python helper checks the live proxy first.
$relink = Button 'Relink moved folder...' 505 279 190
$relink.Visible = [bool]$script:detectionError
$relink.Add_Click({
    $folder = [System.Windows.Forms.FolderBrowserDialog]::new()
    $folder.Description = 'Select your MOVED original configured Sparrow installation, not a freshly extracted installer.'
    try {
        if ($folder.ShowDialog($form) -ne [System.Windows.Forms.DialogResult]::OK) { return }
        $answer = [System.Windows.Forms.MessageBox]::Show(
            'Create a Windows junction from the original Docker deployment path to this folder? The files, Docker containers and volumes will not be replaced. Only use this for your MOVED original installation. Restart the Node Manager after success.',
            'Relink moved Sparrow installation',
            [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -eq [System.Windows.Forms.DialogResult]::Yes) {
            Run-NodeCommand @('relink', '--moved-to', $folder.SelectedPath)
        }
    } finally { $folder.Dispose() }
})
$copyDetails = Button 'Copy error / details' 700 248 177
$copyDetails.Enabled = [bool]$script:detectionError
$copyDetails.Add_Click({
    $details = if ($output.Text.Trim()) { $output.Text } else { $script:detectionError }
    if (-not $details) { return }
    try {
        [System.Windows.Forms.Clipboard]::SetText($details)
        $activity.Text = 'Details copied to clipboard.'
    } catch {
        $activity.Text = 'Clipboard failed: ' + $_.Exception.Message
    }
})
$add = Button 'Add Node' 20 279 125
$refresh = Button 'Refresh list' 157 279 125
$saveDirectory = Button 'Save Directory URL' 295 279 195
$saveDirectory.Visible = $false
$directoryOptions.Add_CheckedChanged({ $saveDirectory.Visible = $directoryOptions.Checked })
Label 'Installed / configured instances' 324 20 860 | Out-Null
$list = [System.Windows.Forms.ListBox]::new()
$list.Location = [System.Drawing.Point]::new(20, 349)
$list.Size = [System.Drawing.Size]::new(857, 135)
$list.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($list)
$install = Button 'Install / Start' 20 494 135
$start = Button 'Start' 166 494 85
$stop = Button 'Stop' 261 494 85
$update = Button 'Update' 356 494 95
$status = Button 'Status' 462 494 90
$logs = Button 'Logs' 564 494 85
$remove = Button 'Remove (keep data)' 659 494 218
$activity = Label 'Ready. Removing an instance never deletes its identity or Docker volumes.' 537 20 880
$activity.ForeColor = [System.Drawing.Color]::DimGray
$output = [System.Windows.Forms.TextBox]::new()
$output.Location = [System.Drawing.Point]::new(20, 568)
$output.Size = [System.Drawing.Size]::new(857, 158)
$output.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Bottom -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$output.Multiline = $true; $output.ReadOnly = $true; $output.ScrollBars = 'Vertical'
$output.WordWrap = $true; $output.Font = [System.Drawing.Font]::new('Consolas', 9)
$form.Controls.Add($output)
if ($script:detectionError) {
    # A normal TextBox also supports Ctrl+A / Ctrl+C and does not clip the
    # underlying failure, even when a Docker label contains a very long path.
    $output.Text = $script:detectionError
}

function Refresh-Instances {
    $previous = if ($list.SelectedIndex -ge 0) { [string]$script:items[$list.SelectedIndex].id } else { '' }
    $script:items = @()
    $list.Items.Clear()
    if (Test-Path -LiteralPath $script:instanceRoot -PathType Container) {
        foreach ($dir in @(Get-ChildItem -LiteralPath $script:instanceRoot -Directory | Sort-Object Name)) {
            $file = Join-Path $dir.FullName 'node-instance.json'
            if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue }
            try {
                $item = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
                $script:items += $item
                $installed = Test-Path -LiteralPath (Join-Path $dir.FullName '.env.runtime') -PathType Leaf
                $endpoint = if ($item.mode -eq 'public') { 'https://' + $item.hostname } else { 'LAN port ' + $item.port }
                [void]$list.Items.Add("$($item.name)  [$($item.id)]  $(if ($installed) { 'installed' } else { 'configured' })  $endpoint")
                if ($item.id -eq $previous) { $list.SelectedIndex = $list.Items.Count - 1 }
            } catch { $output.AppendText("Cannot parse $file : $($_.Exception.Message)`r`n") }
        }
    }
    if ($list.SelectedIndex -lt 0 -and $list.Items.Count -gt 0) { $list.SelectedIndex = 0 }
}
function Quote-PowerShell([string]$value) { return "'" + $value.Replace("'", "''") + "'" }
function Run-NodeCommand([string[]]$arguments) {
    if ($script:running) { return }
    $python = Get-Command python3 -ErrorAction SilentlyContinue
    if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
    if (-not $python) {
        $output.Text = 'Python 3 is required for this revision of the multi-node manager. Install Python 3 (and cryptography when using signed directory discovery).'
        $activity.Text = 'Cannot run Node Manager: see copyable details below.'
        $copyDetails.Enabled = $true
        return
    }
    $logsRoot = Join-Path $root 'logs'
    New-Item -ItemType Directory -Path $logsRoot -Force | Out-Null
    $session = [guid]::NewGuid().ToString('N')
    $script:currentOutput = Join-Path $logsRoot "sparrow-nodes-$session.log"
    $script:currentError = Join-Path $logsRoot "sparrow-nodes-$session-error.log"
    $allArgs = @((Quote-PowerShell $script:nodeScript))
    foreach ($argument in $arguments) { $allArgs += (Quote-PowerShell $argument) }
    $command = '& ' + (Quote-PowerShell $python.Source) + ' -u ' + ($allArgs -join ' ')
    $encoded = [Convert]::ToBase64String([System.Text.Encoding]::Unicode.GetBytes($command))
    try {
        $script:worker = Start-Process -FilePath 'powershell.exe' -ArgumentList @(
            '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded
        ) -WorkingDirectory $root -PassThru -WindowStyle Hidden -RedirectStandardOutput $script:currentOutput -RedirectStandardError $script:currentError
        $script:running = $true
        $activity.Text = 'Running: ' + ($arguments -join ' ')
        $output.Clear()
        $add.Enabled = $false; $install.Enabled = $false; $start.Enabled = $false; $stop.Enabled = $false
        $update.Enabled = $false; $remove.Enabled = $false; $refresh.Enabled = $false; $saveDirectory.Enabled = $false
    } catch {
        $output.Text = $_.Exception.ToString()
        $activity.Text = 'Node Manager failed to start: see copyable details below.'
        $copyDetails.Enabled = $true
    }
}
function Selected-NodeId {
    if ($list.SelectedIndex -lt 0) { return '' }
    return [string]$script:items[$list.SelectedIndex].id
}
$add.Add_Click({
    if ($script:detectionError) {
        $output.Text = $script:detectionError
        $output.Focus()
        $output.SelectAll()
        $activity.Text = 'Deployment detection failed; copy the full details below.'
        $copyDetails.Enabled = $true
        return
    }
    $args = @('add', '--name', $name.Text.Trim(), '--mode', $(if ($mode.SelectedIndex -eq 1) { 'public' } else { 'lan' }))
    if ($hostName.Text.Trim()) { $args += @('--node-domain', $hostName.Text.Trim()) }
    if ($automatic.Checked -and $mode.SelectedIndex -eq 1) { $args += '--auto-dns' }
    if ($plane.Text.Trim()) { $args += @('--control-plane-url', $plane.Text.Trim()) }
    if ($directoryOptions.Checked -and $directory.Text.Trim()) { $args += @('--directory-url', $directory.Text.Trim()) }
    Run-NodeCommand $args
})
$refresh.Add_Click({ Refresh-Instances })
$list.Add_SelectedIndexChanged({
    if ($list.SelectedIndex -lt 0) { return }
    $id = Selected-NodeId
    $conf = Join-Path (Join-Path $script:instanceRoot $id) 'sparrow.conf'
    if (Test-Path -LiteralPath $conf -PathType Leaf) {
        $values = @{}
        foreach ($row in @(Get-Content -LiteralPath $conf)) {
            if ($row -match '^([^#=]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
        }
        $directory.Text = [string]$values['CONTROL_PLANE_DIRECTORY_URL']
    }
})
$saveDirectory.Add_Click({
    $id = Selected-NodeId
    if (-not $id) { return }
    if (-not $directory.Text.Trim()) {
        [System.Windows.Forms.MessageBox]::Show('Enter an HTTPS Directory Server URL to save for this node.', 'Sparrow') | Out-Null
        return
    }
    $args = @('configure-directory', '--id', $id, '--directory-url', $directory.Text.Trim())
    Run-NodeCommand $args
})
$install.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('install', '--id', $id) } })
$start.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('start', '--id', $id) } })
$stop.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('stop', '--id', $id) } })
$update.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('update', '--id', $id) } })
$status.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('status', '--id', $id) } })
$logs.Add_Click({ $id = Selected-NodeId; if ($id) { Run-NodeCommand @('logs', '--id', $id) } })
$remove.Add_Click({
    $id = Selected-NodeId
    if (-not $id) { return }
    $choice = [System.Windows.Forms.MessageBox]::Show('Stop and unpublish this Community Node? Its files, signing identity, credentials and Docker volumes will be kept. You can start it again later.', 'Remove node without deleting data', [System.Windows.Forms.MessageBoxButtons]::YesNo)
    if ($choice -eq [System.Windows.Forms.DialogResult]::Yes) { Run-NodeCommand @('remove', '--id', $id) }
})
$timer = [System.Windows.Forms.Timer]::new()
$timer.Interval = 300
$timer.Add_Tick({
    if (-not $script:running -or $null -eq $script:worker) { return }
    if (-not $script:worker.HasExited) { return }
    $script:worker.Refresh()
    $exitCode = $script:worker.ExitCode
    $script:worker.Dispose(); $script:worker = $null
    $script:running = $false
    $text = @()
    foreach ($file in @($script:currentOutput, $script:currentError)) {
        if ($file -and (Test-Path -LiteralPath $file -PathType Leaf)) { $text += (Get-Content -LiteralPath $file -Raw -ErrorAction SilentlyContinue) }
    }
    $output.Text = ($text -join "`r`n").Replace("`n", "`r`n").Replace("`r`r`n", "`r`n")
    $copyDetails.Enabled = [bool]$output.Text
    $activity.Text = if ($exitCode -eq 0) { 'Completed.' } else { "Failed (exit $exitCode). Review the output below; installed node data was not intentionally deleted." }
    $add.Enabled = $true; $install.Enabled = $true; $start.Enabled = $true; $stop.Enabled = $true
    $update.Enabled = $true; $remove.Enabled = $true; $refresh.Enabled = $true; $saveDirectory.Enabled = $true
    Refresh-Instances
})
$timer.Start()
Refresh-Instances
$form.Add_FormClosing({
    if ($script:running) {
        $answer = [System.Windows.Forms.MessageBox]::Show('A Docker operation is running. Closing this manager will not cancel the child process. Close anyway?', 'Sparrow Nodes', [System.Windows.Forms.MessageBoxButtons]::YesNo)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { $_.Cancel = $true }
    }
})
[System.Windows.Forms.Application]::Run($form)
$timer.Stop(); $timer.Dispose()
