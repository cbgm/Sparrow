# Steps 8g/8h: guarded attachment and independent existing-container lifecycle.
# Dot-sourced by Invoke-SparrowServer.ps1; never bootstraps, copies, or rewrites
# an original deployment. Only a plan pointer is stored in the manager bundle.
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'Get-SparrowDockerLabel.ps1')

function Attached-Absolute([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not [System.IO.Path]::IsPathRooted($Path)) {
        throw 'Attachment requires absolute, existing Windows paths.'
    }
    return [System.IO.Path]::GetFullPath($Path).TrimEnd([char[]]@([char]92,[char]47))
}
function Attached-SamePath([string]$Left, [string]$Right) {
    return (Attached-Absolute $Left).Equals((Attached-Absolute $Right), [StringComparison]::OrdinalIgnoreCase)
}
function Attached-Hash([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Attachment file is missing: $Path" }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Attached-AssertHash([string]$Path, [string]$Expected) {
    if ($Expected -notmatch '^[a-fA-F0-9]{64}$' -or (Attached-Hash $Path) -ne $Expected) {
        throw "An attached deployment file differs from its cutover record: $Path. Management disabled; inspect the original installation."
    }
}
function Attached-ReadJson([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Attachment record is missing: $Path" }
    return (Get-Content -LiteralPath $Path -Raw -ErrorAction Stop | ConvertFrom-Json -ErrorAction Stop)
}
function Attached-ReadProperties([string]$Path) {
    $result = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing deployment settings: $Path" }
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') {
            if ($result.ContainsKey($Matches[1])) { throw "Duplicate settings key in $Path" }
            $result[$Matches[1]] = $Matches[2].Trim().Trim([char]34).Trim([char]39)
        }
    }
    return $result
}
function Attached-DockerResult([string[]]$Arguments) {
    $output = @(& docker @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "Cannot verify attached Docker ownership ($($Arguments[0]) $($Arguments[1]))." }
    return $output
}
function Attached-Workdir([string]$Value) {
    $clean = $Value.Trim().TrimEnd([char[]]@([char]92,[char]47))
    if ($clean -match '^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$') {
        $clean = "$($Matches[1]):\$($Matches[2].Replace('/', '\'))"
    }
    return Attached-Absolute $clean
}
function Attached-AssertOwner([string]$Project, [string]$Directory) {
    $ids = @(Attached-DockerResult @('ps','--all','--filter',"label=com.docker.compose.project=$Project",'--format','{{.ID}}'))
    if ($ids.Count -eq 0) { throw "No original containers found for $Project. Cannot safely attach/start a new project." }
    foreach ($id in $ids) {
        $labels = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir')
        if ($labels.Count -ne 1 -or -not $labels[0] -or $labels[0] -eq '<no value>' -or
            -not (Attached-Workdir ([string]$labels[0])).Equals((Attached-Absolute $Directory),[StringComparison]::OrdinalIgnoreCase)) {
            throw "Docker project $Project has unexpected container ownership ($id); refusing management."
        }
    }
}

# Step 8h: inspect the *existing* service set before any lifecycle command.
# In particular, `compose start` must never silently succeed with a partial
# project after a container was removed outside this manager.
function Attached-ProjectInventory([string]$Project, [string]$Directory) {
    $expected = switch ($Project) {
        'sparrow-community-node' { @('caddy','mailbox-database','mailbox','federation-database','federation','gateway') }
        'sparrow-control-plane' { @('caddy','node-registry-database','node-registry','presence-redis','presence-directory','push-database','push') }
        'sparrow-public-proxy' { @('caddy') }
        default { throw "Unknown attached project: $Project" }
    }
    $ids = @(Attached-DockerResult @('ps','--all','--filter',"label=com.docker.compose.project=$Project",'--format','{{.ID}}'))
    if ($ids.Count -lt $expected.Count) { throw "Incomplete attached project $Project: expected $($expected.Count) existing service containers, found $($ids.Count). No project will be recreated." }
    $seen = @{}
    $states = @{}
    foreach ($id in $ids) {
        $owner = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.project.working_dir')
        if ($owner.Count -ne 1 -or -not $owner[0] -or $owner[0] -eq '<no value>' -or
            -not (Attached-Workdir ([string]$owner[0])).Equals((Attached-Absolute $Directory),[StringComparison]::OrdinalIgnoreCase)) {
            throw "Unexpected container ownership in $Project ($id). No lifecycle action was run."
        }
        $service = @(Get-SparrowDockerLabel -Resource container -Id $id -Key 'com.docker.compose.service')
        if ($service.Count -ne 1 -or -not $service[0]) { throw "Missing service identity for $Project container $id." }
        $name = [string]$service[0]
        # Original Control Plane may have a finished, profile-gated, one-shot
        # ownership initializer. It must NEVER be started by this manager.
        if ($Project -eq 'sparrow-control-plane' -and $name -eq 'registry-signing-init') {
            $initializerState = @(Attached-DockerResult @('inspect','--format','{{.State.Status}}',$id))
            if ($initializerState.Count -ne 1 -or $initializerState[0] -notin @('exited','created')) {
                throw 'The optional registry-signing-init container is unexpectedly active or unreadable; normal lifecycle management is blocked.'
            }
            continue
        }
        if ($name -notin $expected -or $seen.ContainsKey($name)) {
            throw "Unexpected/duplicate attached service $name in $Project. Inspect it manually."
        }
        $state = @(Attached-DockerResult @('inspect','--format','{{.State.Status}}',$id))
        if ($state.Count -ne 1 -or $state[0] -notin @('running','created','exited','paused','restarting','dead')) {
            throw "Cannot read current container state for $Project/$name."
        }
        $seen[$name] = $id
        $states[$name] = [string]$state[0]
    }
    foreach ($name in $expected) {
        if (-not $seen.ContainsKey($name)) { throw "Attached $Project is missing original service $name. Refusing to create replacement containers." }
    }
    return [PSCustomObject]@{ Project=$Project; Services=$expected; States=$states; Ids=$seen }
}
function Attached-ShowLifecycle([object]$Inventory, [string]$ExpectedState = 'Any') {
    $running = 0
    foreach ($service in $Inventory.Services) {
        $state = [string]$Inventory.States[$service]
        Write-Host "  $($Inventory.Project)/$service : $state"
        if ($state -eq 'running') { $running++ }
    }
    if ($ExpectedState -eq 'Running' -and $running -ne $Inventory.Services.Count) {
        throw "$($Inventory.Project): only $running/$($Inventory.Services.Count) services are running. No containers or volumes were recreated. Check status/logs and dependencies."
    }
    if ($ExpectedState -eq 'Stopped' -and @($Inventory.States.Values | Where-Object { $_ -in @('running','restarting','paused') }).Count -gt 0) {
        throw "$($Inventory.Project) did not stop completely. Check Docker status; no data volumes were removed."
    }
    Write-Host "  Lifecycle inventory: $running/$($Inventory.Services.Count) running (not an application health or external reachability test)."
}
function Attached-ProxyArgs([string]$ProxyDirectory) {
    return ,@('compose','-p','sparrow-public-proxy','--project-directory',$ProxyDirectory,
        '-f',(Join-Path $ProxyDirectory 'docker-compose.yml'))
}
function Attached-ComposeArgs([object]$Part) {
    $folder = [string]$Part.OriginalDirectory
    $args = @('compose','-p',[string]$Part.Project,'--project-directory',$folder,
        '--env-file',(Join-Path $folder '.env.runtime'),
        '-f',(Join-Path $folder 'docker-compose.yml'),
        '-f',(Join-Path $folder 'docker-compose.release.yml'),
        '-f',(Join-Path $folder 'docker-compose.production.yml'),
        '-f',(Join-Path $folder 'docker-compose.shared-proxy.yml'))
    if ($Part.Component -eq 'control-plane') {
        $runtime = Attached-ReadProperties (Join-Path $folder '.env.runtime')
        if ($runtime['FIREBASE_ADMIN_CREDENTIALS']) {
            # Preserve the ORIGINAL override selected at cutover, not a newly
            # imported credential from the fresh-install manager UI.
            if (-not (Test-Path -LiteralPath $runtime['FIREBASE_ADMIN_CREDENTIALS'] -PathType Leaf)) {
                throw 'Attached Control Plane Firebase credential is missing; configuration must be reviewed manually.'
            }
            $args += @('-f',(Join-Path $folder 'docker-compose.firebase.yml'))
        }
    }
    return ,$args
}
function Attached-Validate([string]$Plan, [string]$RecordedHash = '') {
    $plan = Attached-Absolute $Plan
    if (-not (Test-Path -LiteralPath $plan -PathType Container)) { throw "Cutover plan not found: $plan" }
    $transactionPath = Join-Path $plan 'cutover-transaction.json'
    $actualHash = Attached-Hash $transactionPath
    if ($RecordedHash) { Attached-AssertHash $transactionPath $RecordedHash }
    $record = Attached-ReadJson $transactionPath
    $readiness = Attached-ReadJson (Join-Path $plan 'cutover-readiness.json')
    if ($record.FormatVersion -ne 1 -or $readiness.FormatVersion -ne 1 -or
        $record.State -ne 'ACTIVE_LOCAL_HTTPS_VERIFIED_EXTERNAL_PENDING' -or
        $readiness.State -ne 'READINESS_ONLY_NOT_AUTHORIZED_TO_CUT_OVER' -or
        -not (Attached-SamePath ([string]$record.PlanDirectory) $plan) -or
        -not (Attached-SamePath ([string]$record.ProxyDirectory) (Join-Path $plan 'proxy')) -or
        -not (Attached-SamePath ([string]$record.BackupDirectory) ([string]$readiness.Snapshot)) -or
        -not (Attached-SamePath ([string]$record.TlsStageDirectory) ([string]$readiness.TlsStage))) {
        throw 'Only an unchanged, completed Step 8f transaction can be attached. An incomplete/rolled-back cutover is not manageable.'
    }
    Attached-AssertHash (Join-Path ([string]$record.BackupDirectory) 'manifest.json') ([string]$readiness.SnapshotManifestSha256)
    Attached-AssertHash (Join-Path ([string]$record.TlsStageDirectory) 'tls-stage.json') ([string]$readiness.TlsStageManifestSha256)
    Attached-AssertHash (Join-Path $plan 'Caddyfile.candidate') ([string]$readiness.CandidateCaddySha256)
    Attached-AssertHash (Join-Path ([string]$record.ProxyDirectory) 'Caddyfile') ([string]$readiness.CandidateCaddySha256)
    Attached-AssertHash (Join-Path ([string]$record.ProxyDirectory) 'docker-compose.yml') (Attached-Hash (Join-Path $PSScriptRoot 'public-proxy/docker-compose.yml'))
    $snapshot = Attached-ReadJson (Join-Path ([string]$record.BackupDirectory) 'manifest.json')
    $tls = Attached-ReadJson (Join-Path ([string]$record.TlsStageDirectory) 'tls-stage.json')
    if ($tls.FormatVersion -ne 1 -or $tls.State -ne 'TLS_STAGED_NOT_CUT_OVER' -or
        $tls.SharedProxyProject -ne 'sparrow-public-proxy' -or
        $tls.StagedDataVolume -ne 'sparrow-public-proxy_caddy-data' -or
        $tls.StagedConfigVolume -ne 'sparrow-public-proxy_caddy-config' -or
        $tls.SourceManifestSha256 -ne $readiness.SnapshotManifestSha256 -or
        $snapshot.FormatVersion -ne 1 -or $snapshot.Status -ne 'CompleteOfflineSnapshot' -or
        -not (Attached-SamePath ([string]$tls.SourceBackup) ([string]$record.BackupDirectory))) {
        throw 'Cutover snapshot or TLS stage no longer matches the completed transaction.'
    }
    $parts = @($record.Components)
    if ($parts.Count -lt 1 -or $parts.Count -gt 2 -or @($readiness.OriginalComponents).Count -ne $parts.Count -or
        @($snapshot.Components).Count -ne $parts.Count) { throw 'Ambiguous attached component set.' }
    $names = @{}
    foreach ($part in $parts) {
        $name = [string]$part.Component
        if ($name -notin @('community-node','control-plane') -or $names.ContainsKey($name) -or
            [string]$part.Project -ne "sparrow-$name") { throw 'Unexpected or duplicate attached Compose project.' }
        $names[$name] = $true
        $source = Attached-Absolute ([string]$part.OriginalDirectory)
        if ((Attached-SamePath $source $PSScriptRoot) -or
            $source.StartsWith(((Attached-Absolute $PSScriptRoot) + [System.IO.Path]::DirectorySeparatorChar),[StringComparison]::OrdinalIgnoreCase)) {
            throw 'Original component folder cannot be inside the fresh manager bundle.'
        }
        $matching = @($readiness.OriginalComponents | Where-Object { $_.Component -eq $name })
        $snapPart = @($snapshot.Components | Where-Object { $_.Name -eq $name })
        if ($matching.Count -ne 1 -or $snapPart.Count -ne 1 -or
            -not (Attached-SamePath ([string]$matching[0].OriginalDirectory) $source) -or
            -not (Attached-SamePath ([string]$snapPart[0].OriginalDirectory) $source) -or
            $matching[0].Project -ne $part.Project -or $snapPart[0].ComposeProject -ne $part.Project -or
            $matching[0].Hostname -ne $part.Hostname) { throw "Original component ownership changed: $name" }
        Attached-AssertHash (Join-Path $source 'sparrow.conf') ([string]$part.CutoverConfigSha256)
        Attached-AssertHash (Join-Path $source 'docker-compose.shared-proxy.yml') ([string]$part.OverlaySha256)
        $conf = Attached-ReadProperties (Join-Path $source 'sparrow.conf')
        $runtime = Attached-ReadProperties (Join-Path $source '.env.runtime')
        $projectKey = if ($name -eq 'community-node') { 'COMMUNITY_NODE_PROJECT_NAME' } else { 'CONTROL_PLANE_PROJECT_NAME' }
        if ($conf['MODE'] -ne 'public' -or $conf['SHARED_PROXY'] -ne 'true' -or
            $conf['PUBLIC_DOMAIN'] -ne $part.Hostname -or $runtime[$projectKey] -ne $part.Project) {
            throw "Original $name settings no longer match the completed cutover."
        }
        # All original files other than the exact, recorded cutover config must
        # still match the protected offline snapshot. Includes secrets, runtime,
        # original Compose files, Caddyfile and external FCM credentials.
        $prefix = "components/$name/"
        $files = @($snapshot.Files | Where-Object {
            ([string]$_.Backup).StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase) -or
            ($name -eq 'control-plane' -and ([string]$_.Backup).StartsWith('external-secrets/control-plane/',[StringComparison]::OrdinalIgnoreCase))
        })
        if (@($files | Where-Object { $_.Backup -eq "${prefix}.env.runtime" }).Count -ne 1 -or
            @($files | Where-Object { $_.Backup -eq "${prefix}docker-compose.yml" }).Count -ne 1 -or
            @($files | Where-Object { $_.Backup -eq "${prefix}docker-compose.release.yml" }).Count -ne 1 -or
            @($files | Where-Object { $_.Backup -eq "${prefix}docker-compose.production.yml" }).Count -ne 1) {
            throw "Original $name snapshot does not record the complete Compose/runtime set."
        }
        foreach ($file in $files) {
            $original = Attached-Absolute ([string]$file.Original)
            $backedUp = Join-Path ([string]$record.BackupDirectory) ([string]$file.Backup)
            Attached-AssertHash $backedUp ([string]$file.Sha256)
            if ($file.Backup -eq "${prefix}sparrow.conf") {
                if (-not (Attached-SamePath $original (Join-Path $source 'sparrow.conf')) -or
                    $file.Sha256 -ne $part.OriginalConfigSha256) { throw 'Original configuration snapshot was substituted.' }
            } else {
                Attached-AssertHash $original ([string]$file.Sha256)
            }
        }
        # Always use the original Compose project and exact cutover file set.
        $args = Attached-ComposeArgs $part
        [void](Attached-DockerResult ($args + @('config','--quiet')))
        [void](Attached-ProjectInventory ([string]$part.Project) $source)
        $volumes = @($snapshot.Volumes | Where-Object { $_.Project -eq $part.Project })
        if ($volumes.Count -eq 0) { throw "Original $name volumes are not recorded." }
        foreach ($volume in $volumes) {
            $v = @(Attached-DockerResult @('volume','inspect',[string]$volume.OriginalVolume,'--format','{{.Name}}'))
            if ($v.Count -ne 1 -or $v[0] -ne $volume.OriginalVolume) { throw "Original $name volume is missing." }
            $volumeProject = @(Get-SparrowDockerLabel -Resource volume -Id ([string]$volume.OriginalVolume) -Key 'com.docker.compose.project')
            if ($volumeProject.Count -ne 1 -or $volumeProject[0] -ne $part.Project) {
                throw "Original $name volume has unexpected Compose ownership."
            }
        }
    }
    # The cutover proxy binds the plan-owned static pages, not fresh-install
    # pages in this bundle. Check both copies before managing its container.
    foreach ($name in @('community-node','control-plane')) {
        $part = @($parts | Where-Object { $_.Component -eq $name })
        $originalIndex = if ($part.Count -eq 1) { Join-Path ([string]$part[0].OriginalDirectory) 'index.html' } else { Join-Path $PSScriptRoot "$name/index.html" }
        Attached-AssertHash (Join-Path $plan "$name/index.html") (Attached-Hash $originalIndex)
    }
    # Reject a proxy that was replaced or rebound to a different Compose owner.
    $proxy = [string]$record.ProxyDirectory
    [void](Attached-ProjectInventory 'sparrow-public-proxy' $proxy)
    [void](Attached-DockerResult @('compose','-p','sparrow-public-proxy','--project-directory',$proxy,
        '-f',(Join-Path $proxy 'docker-compose.yml'),'config','--quiet'))
    foreach ($name in @('sparrow-public-proxy_caddy-data','sparrow-public-proxy_caddy-config')) {
        $found = @(Attached-DockerResult @('volume','inspect',$name,'--format','{{.Name}}'))
        if ($found.Count -ne 1 -or $found[0] -ne $name) { throw "Staged shared-proxy volume is missing: $name" }
        $labels = @("$(Get-SparrowDockerLabel -Resource volume -Id $name -Key 'com.docker.compose.project')/$(Get-SparrowDockerLabel -Resource volume -Id $name -Key 'sparrow.tls.stage')")
        if ($labels.Count -ne 1 -or $labels[0] -ne 'sparrow-public-proxy/offline-import') {
            throw "Staged TLS volume has unexpected owner: $name"
        }
    }
    return [PSCustomObject]@{ Plan=$plan; TransactionSha256=$actualHash; Components=$parts; ProxyDirectory=$proxy }
}
function Attached-Invoke([string]$Action, [string]$Component, [string]$PlanDirectory, [string]$ExpectedState = 'Any') {
    $pointerPath = Join-Path $PSScriptRoot '.sparrow-attached.json'
    # Hold the SAME plan lock used by Step 8f Apply/Rollback throughout
    # validation and the selected Docker operation. A concurrent rollback
    # must not change file ownership between our check and our action.
    $planForLock = if ($Action -eq 'Attach') { Attached-Absolute $PlanDirectory } else {
        $candidatePointer = Attached-ReadJson $pointerPath
        Attached-Absolute ([string]$candidatePointer.PlanDirectory)
    }
    $lockPath = Join-Path $planForLock 'cutover.lock'
    if (-not (Test-Path -LiteralPath $lockPath -PathType Leaf)) { throw 'Original cutover lock is missing; attachment refused.' }
    $lock = [System.IO.File]::Open($lockPath,[System.IO.FileMode]::Open,[System.IO.FileAccess]::Read,[System.IO.FileShare]::None)
    try {
    if ($Action -eq 'Attach') {
        if (Test-Path -LiteralPath $pointerPath) { throw 'A cutover is already attached to this manager. Attachment was not overwritten.' }
        foreach ($folder in @('community-node','control-plane')) {
            if (Test-Path -LiteralPath (Join-Path $PSScriptRoot "$folder/.env.runtime") -PathType Leaf) {
                throw 'The manager bundle already contains a locally installed component. Use a clean bundle for explicit attachment.'
            }
        }
        if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'public-proxy/Caddyfile') -PathType Leaf) {
            throw 'The manager bundle already has a locally configured shared proxy; attachment refused.'
        }
        $validated = Attached-Validate $PlanDirectory
        $pointer = [PSCustomObject]@{ FormatVersion=1; PlanDirectory=$validated.Plan; TransactionSha256=$validated.TransactionSha256 }
        $temp = "${pointerPath}.pending"
        try {
            [System.IO.File]::WriteAllText($temp,($pointer | ConvertTo-Json -Depth 3),[System.Text.UTF8Encoding]::new($false))
            Move-Item -LiteralPath $temp -Destination $pointerPath -ErrorAction Stop
        } finally { if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Force } }
        Write-Host "Attached verified in-place cutover: $($validated.Plan)"
        Write-Host 'Only the plan pointer was stored. Original folders, credentials, volumes, Compose identities and TLS were not changed.'
        return
    }
    $pointer = Attached-ReadJson $pointerPath
    if ($pointer.FormatVersion -ne 1 -or $pointer.TransactionSha256 -notmatch '^[a-fA-F0-9]{64}$') {
        throw 'Unknown manager attachment record. No deployment command was run.'
    }
    $validated = Attached-Validate ([string]$pointer.PlanDirectory) ([string]$pointer.TransactionSha256)
    $parts = @($validated.Components)
    if ($Component -in @('Node','ControlPlane')) {
        $requested = if ($Component -eq 'Node') { 'community-node' } else { 'control-plane' }
        $parts = @($parts | Where-Object { $_.Component -eq $requested })
        if ($parts.Count -ne 1) { throw "No $Component is present in this attached cutover." }
    } elseif ($Component -eq 'Proxy') {
        $parts = @()
    } elseif ($Component -ne 'Combined') {
        throw "Unknown attached component selection: $Component"
    }
    $proxySelected = $Component -in @('Combined','Proxy')
    $proxyArgs = Attached-ProxyArgs $validated.ProxyDirectory
    Write-Host "Attached cutover verified: $($validated.Plan). Action=$Action, selection=$Component."
    if ($Action -eq 'Preflight') {
        foreach ($part in $parts) { Write-Host "Original $($part.Component): $($part.OriginalDirectory) | $($part.Project) | $($part.Hostname)" }
        Write-Host "Shared proxy: $($validated.ProxyDirectory) | sparrow-public-proxy"
        foreach ($part in $parts) {
            Attached-ShowLifecycle (Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory))
        }
        if ($proxySelected) { Attached-ShowLifecycle (Attached-ProjectInventory 'sparrow-public-proxy' $validated.ProxyDirectory) }
        return
    }
    if ($Action -eq 'Backup') {
        throw 'Use the protected offline backup procedure with the ORIGINAL attached directories and proxy. This manager does not silently back up the empty fresh-install bundle.'
    }
    # Validate the entire selected inventory BEFORE stopping or starting any
    # service. Verification of one component never changes the other component.
    foreach ($part in $parts) {
        [void](Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory))
    }
    if ($proxySelected) { [void](Attached-ProjectInventory 'sparrow-public-proxy' $validated.ProxyDirectory) }
    if ($Action -in @('Status','Verify','Logs')) {
        foreach ($part in $parts) {
            $args = Attached-ComposeArgs $part
            Write-Host "==== Original $($part.Component) ($($part.Project)) ===="
            if ($Action -eq 'Logs') {
                Invoke-Docker -Arguments ($args + @('logs','--no-color','--tail','120'))
            } else {
                Attached-ShowLifecycle (Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory)) $ExpectedState
                if ($Action -eq 'Status') { Invoke-Docker -Arguments ($args + @('ps','--all')) }
            }
        }
        if ($proxySelected) {
            Write-Host "==== Original shared public proxy ($($validated.ProxyDirectory)) ===="
            if ($Action -eq 'Logs') {
                Invoke-Docker -Arguments ($proxyArgs + @('logs','--no-color','--tail','120'))
            } else {
                Attached-ShowLifecycle (Attached-ProjectInventory 'sparrow-public-proxy' $validated.ProxyDirectory) $ExpectedState
                if ($Action -eq 'Status') { Invoke-Docker -Arguments ($proxyArgs + @('ps','--all')) }
            }
        }
        if ($Action -eq 'Verify') { Write-Host 'Docker lifecycle verification only. External DNS, TLS, HTTPS, WebSocket, FCM, database recovery and queue continuity remain unverified.' }
        return
    }
    if ($Action -notin @('Start','Stop','Restart')) { throw "Unexpected attached action: $Action" }
    $stopProxyFirst = $proxySelected -and $Action -in @('Stop','Restart')
    if ($stopProxyFirst) {
        Write-Warning 'Stopping/restarting the shared public proxy interrupts ALL hostnames served by this cutover, even when its component containers remain running.'
        Invoke-Docker -Arguments ($proxyArgs + @('stop','caddy'))
        Attached-ShowLifecycle (Attached-ProjectInventory 'sparrow-public-proxy' $validated.ProxyDirectory) 'Stopped'
    }
    foreach ($part in $parts) {
        $args = Attached-ComposeArgs $part
        $inventory = Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory)
        $services = @($inventory.Services)
        Write-Host "==== Original $($part.Component) ($($part.Project)): $Action ===="
        switch ($Action) {
            'Stop' { Invoke-Docker -Arguments ($args + @('stop') + $services) }
            'Start' { Invoke-Docker -Arguments ($args + @('start') + $services) }
            'Restart' {
                Invoke-Docker -Arguments ($args + @('stop') + $services)
                Attached-ShowLifecycle (Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory)) 'Stopped'
                Invoke-Docker -Arguments ($args + @('start') + $services)
            }
        }
        $expected = if ($Action -eq 'Stop') { 'Stopped' } else { 'Running' }
        Attached-ShowLifecycle (Attached-ProjectInventory ([string]$part.Project) ([string]$part.OriginalDirectory)) $expected
    }
    if ($proxySelected -and $Action -in @('Start','Restart')) {
        # Start only the existing Caddy container; never invoke `up`, install,
        # recreate, rebuild or modify the original TLS/Compose configuration.
        Invoke-Docker -Arguments ($proxyArgs + @('start','caddy'))
        Attached-ShowLifecycle (Attached-ProjectInventory 'sparrow-public-proxy' $validated.ProxyDirectory) 'Running'
    } elseif (-not $proxySelected) {
        Write-Host 'Shared proxy unchanged; the other attached hostname remains independently routable if its component is running.'
    }
    Write-Host 'Lifecycle command completed. Container-running state is not proof of service readiness or externally reachable HTTPS.'
    } finally { $lock.Dispose() }
}
