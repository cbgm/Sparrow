# Sparrow Server — unified cross-platform bundle (Windows GUI; Linux/macOS terminal manager)

The unified bundle contains the Community Node and Control Plane as **independent Compose projects**, plus a separate shared Caddy proxy only when Public mode is chosen. Extract to a new, protected directory. `Start-SparrowServer.cmd` opens one WinForms manager. Defaults: Combined + LAN. The Node-only choice opens a separate multi-instance manager; it never installs an extra Control Plane. Docker work runs in a separate PowerShell worker process so the manager remains movable and minimizable. Node-only instances require an existing Control Plane URL or an HTTPS signed Directory URL with an independently pinned public key; they do not require installing a new Control Plane. Combined installations allow the same optional directory input **as well as** the local Control Plane (LAN) or its configured public hostname (Public). Without a wider directory, Combined mode does not discover independent Control Planes automatically.

## Install / Start pulls backend updates from GitHub Container Registry

The manager has **no separate Update button or bundle picker**. In its ORIGINAL
installation directory, select Node, Control Plane or Combined and click
**Install / Start**. The first run creates the normal generated configuration,
secrets, identity and named volumes. Subsequent runs validate the existing
Docker Compose ownership, pull only the selected Sparrow backend service images
from the configured GHCR image prefix/tag, then apply changed backend images
with `docker compose up -d --no-deps` (without re-bootstrap). The existing
PostgreSQL/Redis/Caddy containers, volumes and identities are not intentionally
recreated or pulled during such an update. The Windows manager deliberately
shows no redundant Restart, Preflight, Attach cutover or Verify lifecycle buttons.
Install / Start handles starting and backend updates; Stop, Status and Logs
remain available. Backup and migration diagnostics are advanced scripts, not
normal installation steps. Linux/macOS: the `install` command is equivalent to Install /
Start; `start` remains the no-pull lifecycle command.

The bundled manager scripts are distinct from backend Docker images. This
change does not silently update an open Windows Forms executable from GitHub.
Distribute new installer-script versions via the single unified GitHub release,
and update the *original* installation using the separately guarded source
upgrade procedure; do not extract a fresh bundle over installed config/secrets.
For valuable deployments, take and verify an offline backup before a new image
version; image updates may run incompatible database migrations and are not
an automatic rollback guarantee. An existing deployment owned by another folder
cannot be taken over by Install / Start.

### Node-only instances and the signed Control Plane directory

Select **Community Node only — manage instances** in the Windows installer.
Enter a display name and LAN or Public mode; when Combined is already installed
in this same runtime folder, its existing Control Plane URL is preselected.
The normal setup requires no directory URL or public-key input. An optional
signed-directory URL is available under **Advanced** and reuses only a matching,
independently configured trust pin; unknown custom directories must have their
key provisioned through the advanced CLI. Choose **Add Node** and then
**Install / Start**. Each new
instance receives a distinct Compose project, node signing identity, secret
files, cache and named volumes. You can install multiple instances under one
Docker host; they are **not** multiple unrelated nodes in one container.
The manager offers Start, Stop, Update, Status, Logs, Remove (keep data) and
Save Directory URL per selected node. Public node hostnames share a single
Caddy process and distinct virtual host routes. Linux/macOS users can run
`./Start-SparrowServer.sh nodes --help` for equivalent commands. Node-only
Windows management requires Python 3; signed directory lookups additionally
require the `cryptography` package. Read `README_RUNTIME.md` for commands,
identity/volume preservation, external Control Plane selection and limits.

The independent Directory Server lists and authenticates **Control Planes**;
only the selected existing Control Plane registers and publishes the new node.
A signed directory public key must be pinned independently, never fetched from
the service being verified. An outage retains the previously verified cache
and manual origins. Status reports Control Plane publication as best effort,
without claiming cryptographic verification of `/v1/nodes` by the status UI.

The Windows GUI output pane now normalizes LF/CRLF and split CR progress output,
keeps split UTF-8 characters intact, and wraps long log lines.

**Missing `.env.runtime` in a fresh test installation:** Install / Start
now automatically replaces abandoned Docker containers and volumes for the
selected component(s) when the local runtime is missing, then generates new
configuration and secrets. No recovery/backup/reset choice or confirmation popup
appears. This is intentionally destructive to abandoned **test** identities,
databases, queues and (for a fully orphaned Combined deployment) shared proxy
TLS volumes. If Docker identifies an intact original installation directory
with `.env.runtime`, an attached migration, ambiguous ownership or unexpected
secret files, cleanup is refused rather than deleting that installation.

**Existing installed server:** when `.env.runtime` exists in the original
installation folder, Install / Start updates the selected backend images in
place and does **not** clear containers, DB/queue volumes or generated secrets.
Do not extract the installer into a new folder to update a deployment whose
original runtime still exists elsewhere; run the installed manager instead.

The Windows main screen contains only **Install / Start**, **Stop**, **Status**,
**Logs**, and **Cancel task**. Preflight, Attach cutover, Restart, Offline backup
and Verify lifecycle are not buttons in the routine UI. Advanced backup and
migration scripts are still included for recovery when actually needed.

## Current safety boundaries

This is **not yet an in-place migration tool**. Do not point it at an existing standalone public deployment or copy existing credentials/identity files into a fresh template by hand. If an original installed runtime is recoverable, automatic deletion is blocked. Abandoned TEST Docker resources with no recoverable runtime are discarded by Install / Start as described above; an existing standalone Public deployment is blocked from automatic switching to shared Caddy. In both cases keep the existing installation unchanged until a dedicated backed-up cutover has been verified. The install/stop/restart commands do not remove volumes or run `docker compose down -v`. Cancellation can leave already-created containers running; the start command is resumable.

## Public mode

Use either two manually configured **different, DNS-resolvable hostnames** pointing at the same public IP or the **Caddy + automatic free public hostnames** option described below (sslip.io DNS, no owned domain). Forward 80/TCP and 443/TCP (optional 443/UDP) to this host. The shared Caddy imports each component's existing route definitions and forwards by hostname directly to the component's service aliases on the `sparrow-public-edge` Docker network. Component Caddy remains available locally for diagnostics. The component projects keep their existing named volumes, identity directories, PostgreSQL/Redis instances, and localhost-only diagnostic ports. The proxy has its own persistent TLS certificate volumes. No automatic migration or copying of legacy standalone Caddy TLS volumes occurs. Run external HTTPS/WebSocket and DNS checks before advertising newly installed public endpoints.

## Optional Firebase

For Control Plane installs, optionally select an administrator-owned Google service-account JSON. The importer checks that it is structurally a service account, stores it below `%LOCALAPPDATA%\Sparrow\Server\Secrets\<control-plane-id>`, then attaches it read-only via `docker-compose.firebase.yml`. It does **not** authorize that account on the Sparrow Android target Firebase project or verify a real device delivery. Authorization must be set up separately by an operator with the proper privileges. Without the credential file, the push HTTP service continues running but FCM sending is disabled. Removing the file after installation requires a controlled restart/reconfiguration; do not remove data volumes.

## Source and testing

Build a fresh distributable from the repository root on Windows by running `server\unified\Build-SparrowServer.cmd`. The wrapper runs `New-SparrowServerBundle.ps1` with `powershell.exe -NoProfile -ExecutionPolicy Bypass` for **that process only**, avoiding the unsigned-script error under a normal `RemoteSigned`/`Restricted` process policy. If you call the PowerShell script directly instead, use `powershell.exe -NoProfile -ExecutionPolicy Bypass -File server/unified/New-SparrowServerBundle.ps1`. GitHub Actions uses the same wrapper in `.github/workflows/sparrow-server-bundle.yml` (manual `workflow_dispatch`); download the single `sparrow-server-unified` workflow artifact for development. For distributing new manager scripts, publish a `server-v*` Git tag to create a GitHub Release containing `sparrow-server.zip` and its SHA-256 file. Organization-enforced policies or AppLocker/WDAC may still block execution and should not be circumvented. The resulting `dist/sparrow-server.zip` contains no service-account keys and has no deployment data. Apply the changed-file patch to the source tree **before** running this builder. Windows PowerShell 5.1, Docker Desktop, Compose 2.24.4+, DNS, public ingress, and FCM account authorization require integration verification on the operator's machine. This development patch is not suitable for unattended upgrades of existing deployments.

## Management and deployment preflight (Step 7 / Step 8a)

The optional `Invoke-SparrowServer.ps1 -Action Preflight` diagnostic (not a button in the normal Windows manager) is a read-only inspection. It reports the component installation folders and Compose project names, matching Docker named-volume names (metadata only), container states and published ports. It can also inspect an operator-selected existing Node/Control Plane folder for required state files without copying its contents. This is **not** a backup or a migration; it does not inspect/verify database contents, credentials, identities or certificates. In particular, an online filesystem copy of live PostgreSQL or Redis volumes is not a reliable rollback point.

**Status** shows the selected component's Compose containers, configured endpoint and whether a Control Plane service-account file exists; it does not claim that the public endpoint or FCM delivery is functioning. **Logs** also includes shared proxy logs if the proxy is configured. **Restart** brings up a previously stopped selected component without recreating its containers; stopping a single component leaves the shared proxy and other component alone. Restarting both after Stop Combined also starts the already-configured shared proxy.

The manager guards against starting a second copy of a running Compose project from another deployment folder. A Public-to-LAN switch or an existing Public hostname change is blocked until proxy/TLS cutover is explicitly implemented. Preserve the original installation folder and named volumes. Do **not** run the new installer against existing Public production servers yet. The dedicated, transactional migration/certificate-preservation work, true deployment backup/restore and the end-to-end Windows/Docker/FCM test matrix remain outstanding.

## Step 8b — Offline snapshot before any migration

The optional `Backup-SparrowDeployment.ps1` script provides an explicitly selected offline backup action; the normal Windows manager does not expose migration/backup buttons. It prompts for the ORIGINAL installation directories and a new backup destination. It does not stop, start, migrate, or overwrite running services. Before creating a snapshot, stop the selected original Compose projects (and the shared proxy if already in use); stop any other containers mounting their data volumes. If anything is running, the snapshot fails closed. **Do not stop a production server just to test this intermediate development patch.**

`Backup-SparrowDeployment.ps1` preserves a read-only archive of each selected project's Compose-labeled local Docker volumes (including databases, identities, queues, and the component's Caddy certificate state), the component's configuration, Compose files, secrets, and any configured external FCM service-account JSON. If the installation already uses the shared proxy, its original Compose project, Caddyfile, and TLS volumes must also be selected. Backup output receives a restrictive Windows ACL; store it securely because it contains live encryption keys and credentials. The script does not touch or delete original Docker resources. If the snapshot fails, its output retains `INCOMPLETE-DO-NOT-RESTORE.txt` and must not be used for recovery.

Example for an existing **standalone** Community Node (run after stopping its original Docker project):

```powershell
powershell.exe -NoProfile -File .\Backup-SparrowDeployment.ps1 `
  -NodeDirectory 'C:\OriginalSparrow\community-node' `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-20260922-node'

powershell.exe -NoProfile -File .\Test-SparrowOfflineSnapshot.ps1 `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-20260922-node' -CheckTarContents
```

To snapshot a combined/shared-Public installation, provide `-NodeDirectory`, `-ControlPlaneDirectory`, `-IncludeSharedProxy`, and `-ProxyDirectory` pointing to their **original** directories. The backup is not an automated restore or proof that a database or server has been successfully recovered. Verify the hashes and tar readability with `Test-SparrowOfflineSnapshot.ps1`; a restore rehearsal and public HTTPS/TLS cutover remain necessary before live migration. The existing Public-mode automatic migration guard remains in place; do not start a fresh bundle against your old volumes. No deployment has been migrated in this step.

## Step 8c — recovery rehearsal and legacy Public cutover preflight

The **Offline backup...** manager action now passes its selected component folders and destination to the backup script correctly. Previous bundles used `@args` instead of `@backupParameters`, so a successful UI-triggered backup could not be relied upon. A newly created snapshot must be checked with `Test-SparrowOfflineSnapshot.ps1`; do not assume that previous attempts produced a usable archive.

Once a complete offline snapshot exists, `Test-SparrowRestoreRehearsal.ps1 -BackupDirectory <path>` verifies checksums and tar readability, extracts each archived Docker volume into a fresh randomly named disposable Docker volume, and removes **only** the newly created volumes afterwards. No Sparrow services start and no original volumes, directories or credentials are changed. An interrupted process can leave temporary `sparrow-rehearsal-*` volumes containing sensitive data; identify them by their `sparrow.rehearsal.id` Docker label and remove **only the verified disposable volumes**. Successful extraction is not a database or service recovery test. This operation requires Docker and the `ubuntu:24.04` image.

For a backed-up **legacy standalone Public** installation, `Test-SparrowPublicCutover.ps1 -BackupDirectory <path>` inspects the verified snapshot and verifies that its original Caddy data/config volumes and a certificate directory for each backed-up hostname are present. It does not validate certificate expiry, live DNS, port ownership or HTTPS reachability. No automatic shared-Caddy cutover is enabled by this step; preserve the original installation and its stopped containers and volumes.

This step is preparation, not completion of migration. Restore of services and transactionally switching Public traffic with tested rollback are still outstanding. No Windows/Docker runtime verification was performed in the authoring environment.

## Step 8d — stage original Caddy TLS certificates (not a live cutover)

`Stage-SparrowPublicTls.ps1` now performs an **offline, non-destructive TLS data import**, rather than only checking that an archive contains a certificate directory. It first re-verifies the completed offline backup and the legacy Public hostname/certificate paths. It then creates new, Compose-named `sparrow-public-proxy_caddy-data` and `sparrow-public-proxy_caddy-config` Docker volumes, imports original legacy Caddy `/data` archives into the NEW data volume, and verifies that a certificate file remains for each backed-up Public hostname. A conflicting file from two old Caddy stores aborts the import rather than choosing one or overwriting certificate state. The new `/config` volume starts empty: Caddy's runtime config is reconstructed from the generated Caddyfile; **both original `/config` volumes and their archives remain untouched**.

**This operation requires the original installations to remain offline.** Nothing is switched to the staged volumes by the staging command: it does not start Caddy, stop servers, remove or attach the original Docker volumes, change DNS, change host ports, or rewrite the original Compose projects. An existing shared-proxy data/config volume (including an earlier staging attempt) causes a fail-closed error rather than an overwrite. Choose a new protected stage directory outside the backup:

```powershell
powershell.exe -NoProfile -File .\Stage-SparrowPublicTls.ps1 `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-offline-node' `
  -StageDirectory 'D:\ProtectedBackups\sparrow-tls-stage-node'
powershell.exe -NoProfile -File .\Test-SparrowTlsStage.ps1 `
  -StageDirectory 'D:\ProtectedBackups\sparrow-tls-stage-node'
```

The stage directory contains `tls-stage.json` recording which verified snapshot and newly created Docker volumes belong together. Stage failures remove only newly created, labeled staging volumes when safe; check warnings for any volumes that could not be removed. Protect the new volumes: they contain private TLS keys. **Do not delete or repurpose the original volumes, even after a successful stage.** This is **not** a completed installation adoption, certificate-expiry test, functional rollback, server recovery or Public cutover. The existing standalone-Public migration guard remains enabled until traffic switching and rollback have been implemented and tested.


## Step 8e — verify the original installations before any cutover

`Test-SparrowCutoverReadiness.ps1` adds a **read-only readiness gate** for an existing standalone Public installation that has already been stopped, backed up and staged using Steps 8b–8d. It compares the **original** configuration, `.env.runtime`, secrets, Caddyfile, Compose files and any saved external FCM credentials against their individual snapshot hashes. It verifies that the original Compose projects still have their original working-directory labels, are stopped, and retain their original named volumes without live mounts; it also checks that the staged TLS snapshot matches these original components and hostnames. It refuses any existing shared-proxy project or occupied host TCP 80/443. It generates a candidate shared Caddyfile from the new bundle's actual component routes and runs the real `caddy:2-alpine` syntax validator in a temporary container with no network and no published ports.

This creates **only** a new protected planning directory containing `Caddyfile.candidate` and `cutover-readiness.json` (hashes and source paths, not secret contents). It does not adopt or move a deployment, install/reconfigure Docker services, change DNS or ports, or mount/change staged TLS volumes. A failed Caddy validation leaves an `INCOMPLETE-DO-NOT-CUTOVER.txt` marker. The candidate route validator checks Caddy syntax, **not** actual DNS, TLS validity, network routing, external WebSocket connectivity, database recovery, or push delivery.

For example, **only after an offline snapshot and TLS staging already exist**:

```powershell
powershell.exe -NoProfile -File .\Test-SparrowCutoverReadiness.ps1 `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-offline-node' `
  -StageDirectory 'D:\ProtectedBackups\sparrow-tls-stage-node' `
  -PlanDirectory 'D:\ProtectedBackups\sparrow-cutover-plan-node'
```

The original deployments must remain stopped throughout the entire offline migration workflow. Readiness is **time-sensitive**, not permission to start the new installer against the original project or a rollback implementation. Source volumes are still the originals, and rerunning an installer from a different Compose working directory can re-create containers and change bind mounts. **Public traffic cutover, working-directory adoption and executable rollback are NOT implemented** in this step. The existing Public-mode migration guard stays in place. Do not test this intermediate step on a running production installation.

## Step 8f — explicitly applied, in-place Public traffic cutover with a rollback record

`Invoke-SparrowPublicCutover.ps1` introduces the first **actual activation** path for
an existing **standalone Public** node and/or Control Plane that already passed
Steps 8b–8e. Unlike the separate fresh-install manager, it keeps the ORIGINAL
component installation directories as the Docker Compose working directories,
keeps each original Compose project name and original data/identity/database
volumes, and copies only the shared-proxy Compose overlay into each original
directory. It records the transaction *before* modifying original configuration.
The proxy's Compose file, verified candidate Caddyfile and copies of the
ORIGINAL index pages live inside the protected cutover plan directory. The
previously staged TLS volumes are used as the shared proxy's `/data` and `/config`.
Original component Caddy volumes remain in place, unmodified.

The cutover is deliberately narrow: the original Caddy route files must be
byte-for-byte identical to those in this bundle, the original deployment
files and ownership must still pass a **new** readiness check, and the original
shared-proxy overlay must not already exist. Other legacy revisions require a
specific, separately reviewed migration; do not bypass these guards or copy
this script into an unverified deployment. No Docker named data volumes are
renamed, deleted, restored into a live database, or initialized afresh.

**Do not run on a live production installation merely to test this development
step.** After a separately verified backup, recovery rehearsal, TLS staging and
readiness plan, the offline cutover is invoked explicitly (never automatically
by the fresh-install manager):

```powershell
powershell.exe -NoProfile -File .\Invoke-SparrowPublicCutover.ps1 `
  -PlanDirectory 'D:\ProtectedBackups\sparrow-cutover-plan-node' -Apply
```

During activation, the component projects start with their original
`docker-compose.yml`, release and production overrides plus the new shared
proxy overlay. Only the new shared Caddy publishes public 80/443; the original
component Caddy containers continue to use their original data volumes while
publishing loopback HTTP only. Activation verifies **local** HTTPS through the
shared proxy, using the actual hostname and certificate chain without `-k`.
Successful activation records
`ACTIVE_LOCAL_HTTPS_VERIFIED_EXTERNAL_PENDING`; it does **not** prove DNS,
external HTTPS/WebSocket reachability, FCM delivery, database recovery, or
Windows/Docker restart behavior. A failed activation attempts a non-destructive
rollback and leaves a persistent transaction record and any failed-recovery
instructions in the plan directory.

If explicit rollback is needed, keep the plan, backup and original folders
accessible:

```powershell
powershell.exe -NoProfile -File .\Invoke-SparrowPublicCutover.ps1 `
  -PlanDirectory 'D:\ProtectedBackups\sparrow-cutover-plan-node' -Rollback
```

Rollback **stops** the shared proxy, stops the original projects, restores only
the original configuration from the verified snapshot and removes only the
cutover-owned overlay (after checking both files' expected hashes). It never
runs `docker compose down -v`, never deletes either set of Caddy volumes, and
never deletes original data/identity volumes. For a **single** original Public
project it attempts to restart that original project; for **two** originals it
leaves both stopped because they cannot simultaneously bind the same host
80/443. If an original file was edited outside the cutover, recovery stops
rather than overwriting that change. A terminated PowerShell process may need
explicit `-Rollback`; inspect `cutover-transaction.json` before doing anything
else. Do not run concurrent installers or migration processes against these
projects.

**Following steps:** Step 8g adds explicit attachment of original working
directories; Step 8h adds independent attached component/proxy management and
read-only lifecycle verification. Windows/Docker/live external tests are still
required. An unverified legacy Public server remains blocked by the fresh-
install path; no automatic adoption has been enabled.


## New: Public Caddy without an owned domain (fresh installation only)

In the Windows manager, select **Public** and keep **Caddy + automatic free public hostnames** checked. Leave both hostname fields empty: the background worker obtains the externally observed public IPv4 through HTTPS, constructs distinct `node-<IPv4-dashed>.sslip.io` and `control-<IPv4-dashed>.sslip.io` DNS names and checks that each resolves to the observed IP *before any installation change*. The free hostname is supplied by the independent **sslip.io DNS service**; **Caddy is the shared HTTPS/WSS reverse proxy and certificate manager**, not a DNS provider. Manual, user-owned hostnames remain available by clearing the checkbox.

After Install / Start, the manager displays the **installed Control Plane and Node HTTPS addresses** with individual **Copy** buttons. **Copy all (incl. WSS)** includes the signed `/v1/nodes` directory and `wss://.../v1/gateway` address; **Status** refreshes them from the installed `sparrow.conf` and `.env.runtime`. These are configured URLs, **not proof** of outside connectivity or successful ACME certificate issuance. Always test from a different internet connection, with real HTTPS certificate checks and WebSocket messaging. Caddy requires incoming TCP 80/443 (or matching forwarded ports); a public IP reported by a lookup service does not prove that the Fritz!Box has a globally reachable IPv4 (CGNAT/DS-Lite can prevent IPv4 port forwarding). Public DNS services and certificate authorities have availability/rate limits. Caddy's local/internal certificate is NOT a substitute for a public, Android-trusted certificate.

A changed WAN IPv4 makes generated sslip.io URLs stale. Install / Start **reuses the installed hostnames**, never silently rewrites TLS identities, signed-directory advertisements or config. A hostname/IP change or existing LAN-to-Public conversion needs a separately reviewed, backed-up cutover. An existing LAN deployment cannot simply be reinstalled or replaced with the fresh-Public installer: preserve its `.env.runtime`, identities, TLS assets, queues, databases, and volumes. Running a second copy of the same Compose projects alongside that local installation on the same Docker host is not supported by this installer.

On macOS/Linux, the equivalent fresh-install option is:

```bash
bash Start-SparrowServer.sh install --component combined --mode public --auto-public-hostnames
bash Start-SparrowServer.sh status --component combined
```

The CLI prints copyable configured HTTPS and WSS addresses. The Control Plane directory field remains available for additional trusted registries; a Node-only installation still needs an existing Control Plane directory URL.

## Step 8g — explicit attachment of an existing in-place Public cutover

The guarded CLI attachment procedure (`Invoke-SparrowServer.ps1 -Action Attach` with a verified Step 8f plan) exists for advanced migration only and is not a normal installer button. This is **not** a new
installation or migration: select the original *protected cutover plan folder*
created by Step 8f (the folder containing `cutover-transaction.json`), not the
original component directory, not a snapshot, and not the fresh-install ZIP.
Attachment is accepted only after Step 8f reached
`ACTIVE_LOCAL_HTTPS_VERIFIED_EXTERNAL_PENDING`. The original transaction,
readiness, backup manifest, TLS stage and proxy Caddyfile hashes must match;
the original component settings, runtime, Compose files, other backed-up
configuration and secrets must remain unchanged except for the *exact* Step
8f configuration and shared-proxy overlay. Docker must still report every
component and proxy under its original Compose project and working directory,
with the recorded named volumes still present. The manager shares the Step
8f transaction lock so rollback and management cannot overlap. A customized
installation or incomplete/rolled-back cutover is rejected, not guessed at.

Only the plan folder and transaction SHA-256 are stored in the **new manager
bundle** as `.sparrow-attached.json`; it stores no credentials, private keys,
server identity, or copies of the original installation. Attachment requires
a clean, writable manager bundle with no local installed component or proxy.
Preserve the original plan directory, original component folders, backups,
secrets, staged certificate volumes and Docker engine/context. **Never copy
`.sparrow-attached.json` to another machine and assume it has been verified.**
Each subsequent action re-verifies the transaction hash, source files, volume
existence and container working-directory ownership before calling Compose.
Do not modify `sparrow.conf`, `.env.runtime`, the original Compose files or
secrets using the new installer; an intentional change requires a separately
reviewed management/migration path, not bypassing these guards.

From a clean unified manager bundle on Windows, either click **Attach
cutover...** and select the original plan, or explicitly run:

```powershell
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 `
  -Action Attach -PlanDirectory 'D:\ProtectedBackups\sparrow-cutover-plan-node'

powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 `
  -Action Status -Component Combined
```

For attached projects, **Start existing**, Stop, Restart, Status and Logs use
the **original** component working directories, project names, `.env.runtime`,
base/release/production and exact shared-proxy overlay; the original FCM
override is included when configured. Start uses `docker compose start`, not
bootstrap, `up` or `--force-recreate`, so it cannot initialize a parallel
project, create replacement containers or reinitialize a missing volume.
`Combined` acts on *all* components in the transaction and the transaction's
shared proxy; a single-component Start/Stop/Restart leaves the proxy and other
component alone. Status/Logs include the existing proxy. An unavailable or
mismatched Docker project, modified source, or edited transaction fails
closed. The attached manager intentionally disables its fresh-install
config/FCM controls and its new-install offline-backup button: for an updated
offline snapshot, use the dedicated backup script with the **original**
directories and explicit shared proxy after stopping all affected services.

**This source-only step has not been tested on Windows/PowerShell, Docker
Desktop, real migrated volumes, DNS, WebSocket, FCM, or a rollback rehearsal.**
The Step 8f cutover remains **not production-certified**; do not run on a
live production system merely to test attachment. See Step 8h below for
independent management and the remaining non-production test requirements.

## Step 8h — independently manage attached services and verify container lifecycle

**Only for a completed, explicitly attached Step 8f cutover; it does not extend
fresh-install or legacy standalone management.** In the Windows manager select
Community Node, Control Plane, **Shared public proxy only**, or Combined. The
proxy is its *own* selection, so proxy-only Start/Stop/Restart/Status/Logs do
not stop, start or recreate either original application project. Component-only
Start/Stop/Restart leaves the proxy and the other component alone. Combined
stops the proxy before the components and starts the components before the
proxy. A proxy Stop/Restart interrupts **both** public hostnames while it is
stopped (the UI asks for confirmation); component-only Stop can make that
component's route temporarily unavailable without interrupting the other.

The manager revalidates the completed transaction, original source/config,
Compose working directories, backed-up secrets and persistent volume ownership
under the existing cutover lock. It also requires the original, expected
container for **every** required service; an incomplete, duplicated or
unknown service set is rejected without attempting `docker compose up` or
reinstalling anything. The optional Control Plane `registry-signing-init`
profile container is not started/stopped by normal lifecycle actions. The
manager issues `docker compose start/stop` against named **existing**
containers, then verifies Docker container `running`/stopped state. Its
**Verify lifecycle** button is read-only; Docker `running` is NOT the same as
an application's `healthy`, readiness of PostgreSQL/Redis, external TLS, DNS,
WebSocket, federation, push delivery or verified recoverability.

CLI examples from the attached manager bundle:

```powershell
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 -Action Verify -Component Combined
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 -Action Verify -Component Proxy -ExpectedState Running
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 -Action Verify -Component Node -ExpectedState Stopped
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 -Action Restart -Component Node
powershell.exe -NoProfile -File .\Invoke-SparrowServer.ps1 -Action Status -Component Proxy
```

Only use Stop/Restart in an authorized non-production rehearsal or with a
planned outage: the CLI's explicit `-Component Proxy`/`Combined` is the
operator's interruption request. After each rehearsal transition, verify the
selected project's state, ensure the unselected component's container IDs and
states have not changed, and test both real HTTPS/WebSocket endpoints through
the shared proxy. When checking independent proxy lifecycle, compare both
components' states before/after `-Action Stop -Component Proxy` and
`-Action Start -Component Proxy`; then repeat for Node and Control Plane
separately. Re-run `-Action Verify -Component Combined -ExpectedState Running`
after a complete start. **Do not run a destructive container/volume fault
injection on an original deployment.** Any missing required container or
changed ownership fails closed and requires a separate, backup-backed recovery
plan; the manager will not silently `up`, restore databases or reinitialize
identities.

**Verification status:** source review and patch integrity only in this
execution environment. Windows PowerShell 5.1 parser/UI, Docker Desktop
lifecycle transitions, isolated PostgreSQL/Redis restoration, live external
DNS/TLS/WebSocket, authorized FCM delivery and interrupted-operation recovery
have NOT been exercised. These tests are required before production use.


## Step 8i — isolated recovery of snapshot data and read-only rollback inventory

Use a **protected offline backup on an authorized non-production clone**. From
the *patched unified bundle*, first check the snapshot as before, then run:

```powershell
powershell.exe -NoProfile -File .\Test-SparrowIsolatedRecovery.ps1 `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-offline'
```

The command re-verifies every archived file checksum and every tar archive,
checks that the component/volume inventory is complete, then extracts each
archive into a NEW randomly named, labeled disposable volume. PostgreSQL
`17-alpine` starts against **copies** of the four expected data volumes with
`--network none`, no published TCP ports, and a local Unix socket; it runs a
local SQL query and schema-only dump. Redis `7.4-alpine` loads the restored
persistence, responds to local Unix-socket PING/DBSIZE, and has no exposed
port or network. Identity file presence and its SHA-256 match between archive
and disposable volume are checked without displaying identity contents. Caddy
TLS/config and gateway blob archives are extracted only; their runtime
behavior is NOT checked. No application containers are booted and no real
Compose projects, original volumes, original folders or live secrets are
used. Only new, invocation-labeled volumes are eligible for cleanup; a
failed cleanup prints the exact name and leaves sensitive recovered data
for an operator to inspect and clean up. Requires locally available
`ubuntu:24.04`, `postgres:17-alpine`, and `redis:7.4-alpine` images.

For a *completed* Step 8f cutover, optionally inspect the preserved originals
before planning a rollback **without performing one**:

```powershell
powershell.exe -NoProfile -File .\Test-SparrowRollbackReadiness.ps1 `
  -PlanDirectory 'D:\ProtectedBackups\sparrow-cutover-plan'
```

This command locks the original transaction plan, verifies its recorded
backup/stage/Caddy hashes, the offline snapshot, source file and Compose
owner/volume ownership, and reports the two-original-Public port conflict.
It refuses incomplete/changed/rolled-back transactions. It does not call
`Invoke-SparrowPublicCutover.ps1 -Rollback`, start or stop any deployment,
copy data back into the original volumes, or demonstrate that application
services and external routes recover correctly. A read-only PASS must **not**
be interpreted as permission to carry out an untested production rollback.

**Verification status:** the commands above have not been run against Docker
or Windows PowerShell in the authoring environment. The original step 8f
public cutover remains unverified for production. On a disposable clone,
perform real service-level recovery, a full apply/rollback rehearsal, and
external HTTPS/WebSocket/federation/FCM checks before revising that status.

## Step 8j — external Public DNS / HTTPS / WebSocket probe (read-only)

Run `Test-SparrowPublicEndpoints.ps1` on a **separately networked Windows
client** (for example a laptop using an independent mobile connection), **not**
on the Docker host, its LAN, a loopback/hosts-file override, a VPN tunneling to
the Docker host, or a local reverse proxy. Do not copy the protected cutover
plan, offline snapshots, private keys or Firebase credentials to the external
client. This script needs only the public DNS hostnames and *expected public
IP addresses* of the authorized installation. It never logs in, sends Sparrow
messages, registers an identity, accesses Docker, or alters the installation.

The sample below is for a completed, two-component shared-proxy cutover. Replace
all bracketed placeholders with the actual values from the protected Step 8f
plan and your independently confirmed public WAN IP(s); supply **every**
expected DNS A/AAAA address as separate values. Do not use `--resolve`, curl
`-k`/`--insecure`, or an internal DNS name in place of this probe.

```powershell
powershell.exe -NoProfile -File .\Test-SparrowPublicEndpoints.ps1 `
  -Phase AfterCutover `
  -NodeHostname '<node.example.org>' `
  -ControlPlaneHostname '<control.example.org>' `
  -ExpectedPublicAddresses '<WAN_IPv4>','<WAN_IPv6_IF_PUBLISHED>' `
  -ReportPath 'C:\ProtectedReports\sparrow-public-after-cutover.json'
```

For Node-only or Control-Plane-only deployments, omit the absent component's
hostname. If your DNS publishes only an A record, provide only its expected
IPv4 address; likewise for IPv6-only deployments. The default public DNS
resolver is `1.1.1.1`; use `-PublicDnsResolver '<OTHER_PUBLIC_DNS_IP>'` if
necessary. The probe independently queries that public DNS resolver and the
external client's OS DNS resolver, refuses private/loopback answers, and
requires **both** sets to match the operator-provided IPs. It then performs
GET requests over HTTPS with the OS default certificate-chain and hostname
validation, no HTTP proxy, no redirects, and expects HTTP 200 from all
published component health paths and the Node gateway information route. For
a Node, it also opens `wss://<node-hostname>/v1/gateway` over OS-trusted TLS
and checks the WebSocket upgrade, without sending an authenticated message.
Use `-TimeoutSeconds 30` on a higher-latency connection if required.

After a Step 8f rollback, two former standalone Public components cannot both
bind the same 80/443 ports. First confirm the rollback transaction and
operator-selected original project; only when **one** original Public project
is running, use `-Phase AfterRollback` and specify **only that project's**
hostname and its original public DNS IP. A PASS for that one original says
nothing about the other stopped original or whether the rollback preserved
messages and identities. If DNS has not yet been changed back, treat the DNS
mismatch as failure, rather than bypassing the check with a local override.

The optional JSON report contains hostnames, public IP addresses, endpoint
status and diagnostic errors but no application secrets. Choose an existing
protected directory. The script refuses to overwrite an existing report and
throws on any failed check; a missing external component is **not** checked
and cannot be counted as passed. Public DNS resolver answers are not an
authoritative/DNSSEC verification, and the script cannot automatically prove
that it ran outside the server's network. **Record the actual client/network
used**, then check the server's original Compose identity/volume ownership and
run separate authenticated Sparrow messaging, multi-node federation, FCM
(where authorized), DB continuity, interrupted-operation and rollback tests.
An HTTP 200 and a WSS handshake alone are not application-level recovery.

**Verification status:** This implementation has not been executed on an
independent Windows client or a live Docker deployment in the authoring
environment; neither cutover nor rollback is production-certified.

## Step 8k — optional Control Plane Firebase push preflight (no live send)

`Test-SparrowFirebasePush.ps1` is a **read-only** verification helper for an
already-installed, running Control Plane. Run it on the **Windows Docker host**
with `-ControlPlaneDirectory` set to the *original* Control Plane directory. If
the installation was attached with Step 8g, obtain that original path from the
verified Step 8f transaction; do **not** point this helper at the empty
fresh-install template inside the new manager bundle. It does not import,
rotate, copy or print Firebase credentials, register devices, change Compose,
run bootstrap, create volumes, enqueue wake-ups, or send messages.

```powershell
powershell.exe -NoProfile -File .\Test-SparrowFirebasePush.ps1 `
  -ControlPlaneDirectory 'C:\OriginalSparrow\control-plane' `
  -ExpectedMode Enabled `
  -ExpectedAndroidProjectId 'sparrow-a9048' `
  -ReportPath 'C:\ProtectedReports\sparrow-fcm-preflight.json'
```

For an installation **intentionally without** Google credentials, use
`-ExpectedMode Disabled` instead; this fails if a Firebase credential path,
container environment or mount is unexpectedly configured. In Enabled mode,
the script checks the original config and service-account JSON structure
without exporting private keys or account identifiers, verifies that exactly
one existing `sparrow-control-plane` push container belongs to the original
Compose working directory and is running, compares its configured Android
Firebase target-project ID, and checks that the configured original credential
is mounted **read-only** at the path used by the running push service. These
checks are limited to Docker configuration and file presence; they do not
prove that the Firebase SDK initialized successfully or that Google's IAM
allows the service account to send to the *target Android project*. An
administrator's service account may originate in a different Google project
and needs separately authorized Firebase Cloud Messaging permissions on the
Android project's Google Cloud/Firebase configuration. Do not change the
Sparrow Android app or create an administrator-owned Firebase project per node.

To perform an **optional, non-delivering Google API authorization check**,
add `-ValidateOnly` to the Enabled-mode command. The script prompts privately
for a **short-lived OAuth 2.0 access token** from an independently authorized
service account and a **test-only Android device's FCM registration token**.
It calls the HTTPS Firebase HTTP v1 `messages:send` endpoint with
`validate_only=true`, with no Sparrow `wakeUpId` and no actual device send.
Obtain the OAuth token through your organization's approved Google Cloud
credential workflow with Firebase Messaging scope/permissions; do not paste
OAuth, registration, JSON private-key or internal API tokens into shell
arguments, a ticket, a log or any JSON report. The request uses normal TLS
verification; no HTTP proxy, redirect or trust bypass is enabled. A PASS means
**only that Google accepted the supplied OAuth/test-token validation request**.
It cannot prove that the running service account and the supplied OAuth token
refer to the same credential, or that a real notification will reach Android.
The test is never run unless explicitly requested. The script refuses to
overwrite its report; the report contains test-state summaries, not tokens,
private keys, client emails, credential paths or push database contents.

### Separate real-device verification (operator-driven, non-production)

Use an independently authorized Android test device and a **disposable
non-production installation**, never a real user's registration token or an
existing message as a probe. Confirm the actual Control Plane/Firebase project
is the one configured in the installed Android app. Check the Control Plane's
health through the normal trusted endpoint (`/health/push`; on Public use the
independent external probe from Step 8j) and locally inspect the push-service
`/health` diagnostic from the same Docker network to see `fcmEnabled=true` for
enabled mode or `false` for disabled mode. `/health/push` is merely readiness:
it does **not** expose `fcmEnabled`, and an SDK object being present does not
prove Google authorization. With the device registered **through Sparrow's
normal flow**, put it in the app's ordinary inactive/background state, send a
legitimate **encrypted test message** from a separate test identity, observe
the FCM wake-up and eventual message delivery/ack through the app's normal
behavior, and confirm that the same test conversation is still readable after
reconnect. Repeat the message test with FCM intentionally unconfigured and
with the app online to establish that push is optional and encrypted messaging
and pending envelopes continue to work. Do **not** invoke internal wake-up,
acknowledgement or envelope-deletion routes as test shortcuts: these mutate
real pending state. No sender token, device token, encrypted contents, identity
key or credential belongs in a verification report. A live test must be
performed and its results recorded separately; the authoring environment has
not executed one.

**Verification status:** The Step 8k helper is not a Google/Firebase integration
certificate. Windows PowerShell 5.1, Docker Desktop, the imported credential,
Google authorization, and actual Android device delivery remain to be tested
on an operator-controlled non-production setup. None of Steps 8f–8k is
certified for production migration.


## Step 8l — final implementation step: non-production messaging rehearsal

**There is no automatic end-to-end success claim.** `Test-SparrowMessagingRehearsal.ps1`
adds a four-phase, read-only server inspector and explicit *operator-attested*
Android test-device checklist for an already completed **two-component Step 8f
cutover**. This step closes the source-side verification tooling sequence, not
the real-world release gate. It does not create messages or test identities,
read queued payloads, acknowledge envelopes, change runtime configuration,
recreate containers, delete volumes, run cutover/rollback, or claim that an
operator's answer demonstrates cryptographic E2EE.

From the **patched unified bundle** on the authorized NON-PRODUCTION Windows
Docker host, select an absolute protected local `SessionDirectory` outside
the original installation, backed-up snapshots and cutover plan. Do NOT put
secrets, test tokens, plaintext message content or screenshots into the
resulting JSON evidence. Use two TEST Android installations A and B, both
connected to this disposable clone; never use customer accounts. Execute
these four commands **in order**, completing the Android/lifecycle actions
BEFORE invoking the corresponding command. Then type `PASS` only when those
observations are already complete (`FAIL` stops without recording the phase).
The inspector holds the exclusive cutover lock while it asks the question, so
DO NOT try to restart the server from the attached manager while the question
is on-screen:

```powershell
$plan = 'D:\ProtectedBackups\cutover-plan-clone'
$session = 'D:\ProtectedReports\sparrow-rehearsal-01'

powershell.exe -NoProfile -File .\Test-SparrowMessagingRehearsal.ps1 `
  -PlanDirectory $plan -SessionDirectory $session -Phase Baseline
# Disconnect TEST device B completely, then send two messages + attachment from A.
powershell.exe -NoProfile -File .\Test-SparrowMessagingRehearsal.ps1 `
  -PlanDirectory $plan -SessionDirectory $session -Phase OfflineQueued
# Keep B offline; use the ATTACHED manager to restart Node, then Control Plane
# separately, without touching the proxy or recreating any containers.
powershell.exe -NoProfile -File .\Test-SparrowMessagingRehearsal.ps1 `
  -PlanDirectory $plan -SessionDirectory $session -Phase AfterRestart
# Reconnect B, check both texts + attachment exactly once; reply from B to A.
powershell.exe -NoProfile -File .\Test-SparrowMessagingRehearsal.ps1 `
  -PlanDirectory $plan -SessionDirectory $session -Phase Delivered
```

At every phase the script revalidates the Step 8f transaction, exact original
Compose ownership/files, recorded named volumes and the presence/running
state of all expected service containers under the **same exclusive cutover
lock** as the attached manager. It compares on-volume Node and Control Plane
identity SHA-256 fingerprints between phases (including three Node service
views) and checks that original service container IDs did not change since Baseline.
It reads only aggregate row counts from the actual PostgreSQL
`mailbox_envelopes`, `federation_outbound_envelopes` and `pending_envelopes`
tables. Aggregate counts may move because of unrelated traffic, expiry or
acknowledgements: they cannot establish that a particular message was queued
or delivered. The operator must check test-device behavior independently.
The four local JSON files include identity **hashes** (not private identity
contents), transaction hash, container IDs, counts and the explicit
`OPERATOR_ATTESTED_NOT_AUTOMATED` flag. Store this session locally with
restricted permissions, do not publish these fingerprints in support tickets.
No observed message text, recipient IDs, device tokens or credentials are
recorded. A failed phase does not create a PASS record; start a new session
if you need to repeat an already recorded phase.

**External checks (separate, not automated by this script):** Run Step 8j's
DNS/HTTPS/WSS probe from an independently networked client before and after
each lifecycle change, with both original hostnames and expected PUBLIC
addresses. Confirm 8k's real-device push wake-up using an authorized test
registration token and optional-FCM-disabled fallback; `validate_only` does
not send a notification. Compare test Android identity/trust and old chat
history before/after the restart. A visual end-to-end delivery test does not
prove that a plaintext never appeared on a wire: independently inspect client
encryption and the protocol if that assurance is required.

**Release gate / outstanding real-world work:** Windows PowerShell 5.1 parser
and Windows Forms move/minimize/Alt+Tab/nonblocking UI; Docker Compose config
and actual fresh LAN/Public installs; legacy one-Node/one-CP/two-component
cutovers; shared proxy routing and external DNS/HTTPS/WSS; manual two-device
online/offline/after-restart/attachment delivery; optional live FCM; isolated
PostgreSQL/Redis application-level recovery; real interrupted-operation and
rollback rehearsal including the two-standalone-public 80/443 conflict.
The 8i isolated restore is only a **data-file** recovery rehearsal; it does
not verify restored Sparrow services. Observe that FAILED/incomplete
transactions and externally modified deployments are rejected without
manual intervention. Never perform destructive fault injection on the
original installation. **No production migration approval is implied.**

Verification status: source and package integrity only in the authoring
environment; no Windows, Docker, Google, external network or Android test
was run here.

## Single cross-platform GitHub bundle (Windows, Linux, macOS)

The **single** `.github/workflows/sparrow-server-bundle.yml` builds **one** `dist/sparrow-server.zip`
(on Windows using `server/unified/Build-SparrowServer.cmd`) and uploads exactly one
`sparrow-server-unified` artifact. The same ZIP is then smoke-checked on Windows,
Ubuntu and macOS; the workflow does **not** build separate Community Node or Control
Plane installer ZIPs. This does **not** replace the container-image publication
pipeline: the server images referenced by `SPARROW_IMAGE_PREFIX` / `SPARROW_IMAGE_TAG`
must already be available from the appropriate registry, for the chosen Docker CPU
architecture. Linux/macOS still require actual Docker and service integration tests.

The ZIP contains `Start-SparrowServer.cmd` for the original **Windows GUI**, and
`Start-SparrowServer.sh` / `Start-SparrowServer.command` plus `Invoke-SparrowServer.py`
for the **macOS/Linux terminal manager**. There is no native macOS/Linux GUI in
this release. Install Python 3, Bash and Docker Desktop/Engine with Compose 2.24.4+.
Unzip into a **new empty directory** (not your current deployment folder); on macOS
open Terminal in that directory and run `bash Start-SparrowServer.command`. Running
with no arguments prints the usage examples. Linux uses:

```bash
bash Start-SparrowServer.sh install --component combined --mode lan
bash Start-SparrowServer.sh status --component combined
bash Start-SparrowServer.sh stop --component node
bash Start-SparrowServer.sh start --component node
```

For a **new** Public Combined installation on either system, configure distinct
working public hostnames resolving to the same public IP, ensure that 80/443 are
available and accessible, then use:

```bash
bash Start-SparrowServer.sh install --component combined --mode public \
  --node-domain node.example.com --control-domain control.example.com
bash Start-SparrowServer.sh status --component combined
```

Replace the example names with your own domains; the manager creates a **shared**
Caddy entry point. `--component node` installs the Node alone only when you provide
`--directory-url` for an existing trusted Control Plane. `--component control-plane`
installs only the Control Plane. `--component proxy` is a lifecycle selection for
an **already configured** Public proxy, not a stand-alone installation.
Do not change an installed Public hostname or move an installation folder through
these commands. To add a Public component to an existing proxy, plan the route/TLS
change separately; automatic in-place Public reconfiguration is not covered here.

The Linux/macOS CLI cannot run the Windows PowerShell legacy migration, offline
backup/restore, or cutover-attachment workflows. It deliberately rejects other
Compose working directories and orphaned named volumes instead of attempting
migration. Do **not** infer cross-platform migration support from cross-platform
fresh installation support. Never run a fresh bundle against volumes/containers
from the older standalone installers. Protect `.env.runtime`, `sparrow.conf`, the
`secrets/` directories, the proxy's Caddyfile, and the original Docker volumes.

The project archive supplied to implement this workflow did not contain any
repository-root `.github/workflows/` entries. If GitHub has additional legacy
standalone installer workflows in the real repository, remove or disable those
*specific workflow files* after comparing them with their current repository
versions; this patch does not guess their filenames or delete unrelated image
builds, tests, or release workflows.


### Windows Docker template error: `function "com" not defined`

PowerShell 5.1 can strip embedded double quotes inside native Docker Go-template
arguments. This bundle reads Compose ownership labels from Docker's JSON
inspection output instead. Rebuild the bundle and use the rebuilt installer;
no Docker volumes or original server directories need to be deleted for this fix.
A `#< CLIXML` progress record in a redirected PowerShell stderr log is diagnostic
serialization, not a Docker deployment failure. The GUI now suppresses child
PowerShell progress records and displays the worker's error separately.

### Install / Start, updates, and live node-directory changes

The routine Windows screen has only **Install / Start**, **Stop**, **Status**,
**Logs**, and **Cancel task**. Install / Start pulls the selected backend images
from GHCR (no separate Update button). An installed node can also change its
Control Plane directory URL through the existing field: Install / Start fetches
the JSON directory, updates `CONTROL_PLANE_DIRECTORY_URL` and the three
Control Plane routing/advertisement values in the installed `.env.runtime`, then
reloads the existing mailbox, federation, and gateway containers with
`--pull never` for this configuration-only change. No new secrets, identities,
PostgreSQL/Redis volumes, or Compose project names are created.

For a Combined LAN installation, `http://localhost:8390` is valid **on the
Windows host**, but it must not be returned to an Android phone by the node's
`/v1/control-planes` endpoint. Fresh bootstrap and in-place discovery refresh
advertise the node's existing LAN address (for example
`http://192.168.178.60:8390`) while preserving Docker-internal routing.
Run `Invoke-RestMethod http://<node-ip>:8490/v1/control-planes` to verify the
**actual running gateway**; checking the Control Plane's `/v1/nodes` only proves
that the Node registered, not which Control Plane URLs the Node advertises.
If the supplied JSON directory *itself* still lists an old IP, correct its
content at the source: the installer must not silently rewrite a trusted
operator-provided directory into a different network.

A successful bundle build is **not** a successful GHCR image publication. The
single GitHub workflow publishes six separate backend images (three for each
component) from `server/Dockerfile` before publishing its single unified ZIP
release. The update's `docker compose pull` must succeed for all selected
backend images; the manager now displays Docker's real stderr/exit code, not
just a generic failure. If the GHCR package is private, the operator must have
pull authorization. A failed pull does not mean that existing Docker volumes
should be deleted or that an image update succeeded.

On Windows, preserve the existing installation directory and its `sparrow.conf`,
`.env.runtime`, `secrets/` and Docker volumes. A bundle's source files are not
runtime settings: do not copy a fresh-install ZIP over those files. To install
a patched manager version into an already extracted bundle, copy only the
patched manager/bootstrap scripts to their original corresponding locations.
The GitHub workflow creates a new unified distribution for **fresh** installs;
backend image updates through Install / Start do not automatically replace the
Windows manager's own PowerShell scripts.

For a truly fresh disposable test installation with matching orphaned Docker
resources but no recoverable `.env.runtime`, the Windows Install / Start path
replaces only the selected orphaned TEST resources automatically, per the
existing test-reset restrictions. **Never use this as an update mechanism**:
it deletes those orphaned databases and identities. An intact installation with
its original runtime settings takes the non-destructive update path instead.

### Windows manager operation logs

Every Windows manager action writes a separate operation record into the installed
`<sparrow-server>/logs/` directory. The `*-output.log` and `*-errors.log` files
retain the worker output and native/PowerShell diagnostics; on a fatal error, the
`*-failure.log` file contains the plain-text failure reason, even if the redirected
PowerShell error stream contains CLIXML. The manager displays that failure in its
log panel. Logs are deployment-local diagnostics and may contain sensitive paths
or runtime data: keep them private and do not include this directory in installer
distribution bundles. An error log in an older installation may still reside in
`%TEMP%`; this change applies to manager actions started after installing the patch.

The installer now checks/creates the shared Docker network before it considers
removing abandoned test resources. A missing `sparrow-public-edge` is not an
installation failure; it is created as needed. **This does not authorize automatic
cleanup of any existing deployment**: retain the original installation folder,
server identities and volumes; use an isolated test deployment for destructive tests.
