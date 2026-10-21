# Sparrow Server — cross-platform installer bundle (Windows, Linux, macOS)

The bundle contains the Community Node and Control Plane as **independent Compose projects**, plus a
separate shared Caddy proxy only when Public mode is chosen. Extract it to a new, protected directory.
`Start-SparrowServer.cmd` (Windows), `Start-SparrowServer.command` (macOS) or `./Start-SparrowServer.sh`
(Linux) opens the same installer window on all three systems (`app/manager_window.py`, Python 3.9+
with Tk; most Linux desktops need the `python3-tk` package). Without a desktop, for example over SSH,
the same options appear as a text menu (`app/manager_terminal.py`), and every action can also be run
from the command line (see `README_RUNTIME.md`). **What to install** chooses between the Combined
installation (Control Plane + Community Node; defaults: Public with automatic sslip.io hostnames for a
fresh installation) and **Community Node only**, for a machine whose nodes connect to an existing
Control Plane. Once Combined is installed, the choice stays Combined. **Nodes...** opens a separate
manager for additional independent Community Node instances; it lists the main node of the Combined
installation first and never installs an extra Control Plane. Docker work runs in a separate engine process
(PowerShell on Windows, Python on Linux/macOS) so the window stays responsive. Combined installations
accept an optional signed Directory URL **as well as** the local Control Plane (LAN) or its configured
public hostname (Public). Without a Directory, Combined mode does not discover independent Control
Planes automatically.

## Starting from the source code

`server/installer/Start-SparrowServer.*` can be started directly from a source checkout. The window
(or text menu) then only asks where to create your server folder (default `~/Sparrow Server`),
creates it with `python3 server/installer/build.py --install <folder>` (the folder must be new or
empty; the published server images are used) and opens the installer there. All server data lives in
that folder, never in the source tree. If a Sparrow server is already running in Docker from another
folder, the new folder offers **Move server here** instead of Install (see "Updating"
in `README_RUNTIME.md`).

## Install / Start pulls backend updates from GitHub Container Registry

Updating the manager itself (release, ZIP or local source build) is done from
the **Updates** card; see `README_RUNTIME.md`. In its ORIGINAL installation
directory, click **Install** / **Start / apply settings**. The first run creates the normal generated
configuration, secrets, identity and named volumes. Subsequent runs validate the existing
Docker Compose ownership, pull only the selected Sparrow backend service images
from the configured GHCR image prefix/tag, then apply changed backend images
with `docker compose up -d --no-deps` (without re-bootstrap). The existing
PostgreSQL/Redis/Caddy containers, volumes and identities are not intentionally
recreated or pulled during such an update. The window deliberately
shows no Restart, Preflight, Attach cutover or Verify lifecycle buttons.
Start handles starting and backend updates; Stop, Status and Logs
remain available. Backup and migration diagnostics are advanced Windows scripts, not
normal installation steps. Linux/macOS command line: the `install` command is equivalent to
Start; `start` remains the no-pull lifecycle command.

For valuable deployments, take and verify an offline backup before a new image
version; image updates may run incompatible database migrations and are not
an automatic rollback guarantee. Do not extract a fresh bundle over installed config/secrets. An
existing deployment owned by another folder cannot be taken over by Start; use **Move existing server
here...** for that.

### Node-only instances and the signed Control Plane directory

Click **Nodes...** in the installer window (text menu: **Community Nodes**), then **Add...**. On a
machine without the Combined installation, choose **Community Node only** under **What to install**;
its button **Add Community Node...** opens the same dialog. Enter a name, choose Public or LAN and an
automatic sslip.io or your own hostname. Leave the Control Plane URL empty to use the Control Plane of
this installation; Community Node only requires it. Then select the node and click **Install**. Each
new instance receives a distinct Compose project, node signing identity, secret
files, cache and named volumes. You can install multiple instances under one
Docker host; they are **not** multiple unrelated nodes in one container.
The node manager offers Install, Start, Stop, Update, Status, Logs and Remove (keep data) per selected
node. Its first row, **Main node (main installation)**, is the Community Node of the Combined
installation; it is started, stopped and updated in the main window.
Every Start of the main installation (also **Build from source**, **Update to latest release** and
**Apply ZIP**, which end with a Start) brings each running added node onto the main installation's
server images (a local build without pulling from the registry) and the current routes of the node
template. A stopped node is left alone; **Update** does the same for a single node. Public node hostnames share a single Caddy process and distinct virtual host routes. A signed
Directory URL with an independently obtained public key is set through the command line
(`./Start-SparrowServer.sh nodes --help`, `app/Manage-SparrowNodes.py`); signed directory lookups
require the Python `cryptography` package. Read `README_RUNTIME.md` for commands,
identity/volume preservation, external Control Plane selection and limits.

The independent Directory Server lists and authenticates **Control Planes**;
only the selected existing Control Plane registers and publishes the new node.
A signed directory public key must be pinned independently, never fetched from
the service being verified. An outage retains the previously verified cache
and manual origins. Status reports Control Plane publication as best effort,
without claiming cryptographic verification of `/v1/nodes` by the status UI.

**Missing `.env.runtime` in a fresh test installation:** Install
automatically replaces abandoned Docker containers and volumes for the
selected component(s) when the local runtime is missing, then generates new
configuration and secrets. No recovery/backup/reset choice or confirmation popup
appears. This is intentionally destructive to abandoned **test** identities,
databases, queues and (for a fully orphaned Combined deployment) shared proxy
TLS volumes. If Docker identifies an intact original installation directory
with `.env.runtime`, an attached migration, ambiguous ownership or unexpected
secret files, cleanup is refused rather than deleting that installation.

**Existing installed server:** when `.env.runtime` exists in the original
installation folder, Start updates the selected backend images in
place and does **not** clear containers, DB/queue volumes or generated secrets.
Do not extract the installer into a new folder to update a deployment whose
original runtime still exists elsewhere; run the installed manager instead, or move it with **Move
existing server here...**.

The installer window contains the **Settings** card (reachability, automatic hostnames, Directory
Server URL), **Push notifications and phone verification** (Firebase service account), the
**Server** card (installed addresses with Copy buttons, **Install** / **Start / apply settings**,
**Stop**, **Status**, **Logs**, **Nodes...**) and the **Updates** card. **Cancel** stops a running
task. **Advanced...** holds the image prefix/tag and **Reinstall from scratch...** (Public mode only;
erases every server database, identity, queued message and certificate). Preflight, Attach cutover,
Restart, Offline backup and Verify lifecycle are not buttons in the routine UI.

## Current safety boundaries

This is **not a migration tool for legacy standalone installations**. Do not point it at an existing
standalone public deployment or copy existing credentials/identity files into a fresh template by
hand. If an original installed runtime is recoverable, automatic deletion is blocked. Abandoned TEST
Docker resources with no recoverable runtime are discarded by Install as described above; an existing
standalone Public deployment is blocked from automatic switching to shared Caddy. In both cases keep
the existing installation unchanged until a dedicated backed-up cutover has been verified. The
install/stop/restart commands do not remove volumes or run `docker compose down -v`. Cancellation can
leave already-created containers running; the start command is resumable.

## Public mode

Use either two manually configured **different, DNS-resolvable hostnames** pointing at the same public IP or the **automatic free public hostnames** option described below (sslip.io DNS, no owned domain). Forward 80/TCP and 443/TCP (optional 443/UDP) to this host. The shared Caddy imports each component's existing route definitions and forwards by hostname directly to the component's service aliases on the `sparrow-public-edge` Docker network. Component Caddy remains available locally for diagnostics. The component projects keep their existing named volumes, identity directories, PostgreSQL/Redis instances, and localhost-only diagnostic ports. The proxy has its own persistent TLS certificate volumes. No automatic migration or copying of legacy standalone Caddy TLS volumes occurs. Run external HTTPS/WebSocket and DNS checks before advertising newly installed public endpoints.

## Optional Firebase (push notifications and phone verification)

For the Control Plane, optionally select an administrator-owned Google service-account JSON under
**Push notifications and phone verification** (command line: `--firebase-file`). The importer checks
that it is structurally a service account and stores it (Windows: below
`%LOCALAPPDATA%\Sparrow\Server\Secrets\<control-plane-id>`; Linux/macOS:
`control-plane/secrets/firebase-admin.json`), then attaches it read-only via
`docker-compose.firebase.yml` to two services:

- `push` sends FCM wake-ups with it;
- `presence-directory` verifies the Firebase phone sign-in token that the app sends once, with its
  first route registration at sign-up (`GOOGLE_APPLICATION_CREDENTIALS`).

Both use the Firebase project `FIREBASE_TARGET_PROJECT_ID` (default `sparrow-a9048`, the project of the
Android app). The account therefore needs Firebase Cloud Messaging **and** Firebase Authentication
access to that project. The importer does **not** grant this access or verify a real device delivery;
authorization must be set up separately by an operator with the proper privileges.

Without the credential file, the push HTTP service continues running but FCM sending is disabled, and
phone verification tokens are not checked (registrations are accepted with or without a token). With
credentials, a registration that carries a token is accepted only if the token is a valid Firebase
phone sign-in whose number matches the advertised phone alias; otherwise it is rejected with
`PHONE_VERIFICATION_FAILED`. Registrations without a token (every later route refresh) are always
accepted, and nothing about the verification is stored. Removing the file after installation
requires a controlled restart/reconfiguration; do not remove data volumes.

## Source and testing

Build a fresh distributable from the repository root with `python3 server/installer/build.py`
(Windows, Linux and macOS); `--local-images` builds the six server images from the source tree and
embeds them, `--install <folder>` creates a ready server folder instead of a ZIP. An installed manager
builds and applies local images itself with **Build from source...**. The resulting
`dist/sparrow-server.zip` contains no service-account keys and has no deployment data.
Organization-enforced policies or AppLocker/WDAC may still block execution and should not be
circumvented. Windows PowerShell 5.1, Docker Desktop, Compose 2.24.4+, DNS, public ingress, and FCM
account authorization require integration verification on the operator's machine.

Source-only tests live in `server/installer/tests/` (`Test-SparrowLocalImages.ps1`,
`Test-SparrowDirectoryRoutePreservation.ps1`); they are not part of the bundle.

## Advanced Windows scripts (not normal installation steps)

The following scripts are shipped in `app/` for recovery and legacy migration only. They are
PowerShell-only; the Linux/macOS engine has no equivalent. **Do not run them on a live production
installation merely to try them out.**

### Preflight

`app\Invoke-SparrowServer.ps1 -Action Preflight` (Linux/macOS: `preflight`) is a read-only
inspection. It reports the component installation folders and Compose project names, matching Docker
named-volume names (metadata only), container states and published ports. It can also inspect an
operator-selected existing Node/Control Plane folder for required state files without copying its
contents. This is **not** a backup or a migration; it does not inspect/verify database contents,
credentials, identities or certificates. In particular, an online filesystem copy of live PostgreSQL
or Redis volumes is not a reliable rollback point.

**Status** shows the Compose containers, configured endpoints and whether a Control Plane
service-account file exists; it does not claim that the public endpoint or FCM delivery is
functioning. **Logs** also includes shared proxy logs if the proxy is configured.

The manager guards against starting a second copy of a running Compose project from another
deployment folder. A Public-to-LAN switch or a change of a custom Public hostname is blocked until
proxy/TLS cutover is explicitly implemented (generated sslip.io names follow a changed public IP, see
below). Preserve the original installation folder and named volumes.

### Offline snapshot

`app\Backup-SparrowDeployment.ps1` creates an explicitly requested offline backup. It takes the
ORIGINAL installation directories (`-NodeDirectory`, `-ControlPlaneDirectory`, and for a shared-Public
installation `-IncludeSharedProxy -ProxyDirectory`) and a new `-BackupDirectory`. It does not stop,
start, migrate, or overwrite running services. Before creating a snapshot, stop the selected original
Compose projects (and the shared proxy if already in use); stop any other containers mounting their
data volumes. If anything is running, the snapshot fails closed.

The snapshot preserves a read-only archive of each selected project's Compose-labeled local Docker
volumes (including databases, identities, queues, and the component's Caddy certificate state), the
component's configuration, Compose files, secrets, and any configured external FCM service-account
JSON. Backup output receives a restrictive Windows ACL; store it securely because it contains live
encryption keys and credentials. The script does not touch or delete original Docker resources. If
the snapshot fails, its output retains `INCOMPLETE-DO-NOT-RESTORE.txt` and must not be used for
recovery.

```powershell
powershell.exe -NoProfile -File .\app\Backup-SparrowDeployment.ps1 `
  -NodeDirectory 'C:\OriginalSparrow\community-node' `
  -BackupDirectory 'D:\ProtectedBackups\sparrow-20260922-node'
```

The backup is not an automated restore or proof that a database or server can be recovered.

### Legacy standalone Public cutover

For an existing **standalone Public** node and/or Control Plane (one Caddy per component), three
scripts move public traffic to one shared Caddy without moving data volumes:

1. `app\Stage-SparrowPublicTls.ps1 -BackupDirectory <snapshot> -StageDirectory <new folder>` imports
   the original Caddy `/data` archives into NEW `sparrow-public-proxy_caddy-data` /
   `sparrow-public-proxy_caddy-config` volumes and records them in `tls-stage.json`. It starts and
   stops nothing; the original volumes and archives stay untouched, and an existing shared-proxy
   volume causes a fail-closed error.
2. `app\Invoke-SparrowPublicCutover.ps1 -PlanDirectory <plan> -Apply` keeps the ORIGINAL component
   directories, project names and volumes, adds the shared-proxy overlay and verifies **local** HTTPS
   through the shared proxy (`ACTIVE_LOCAL_HTTPS_VERIFIED_EXTERNAL_PENDING`). `-Rollback` stops the
   shared proxy, restores the original configuration from the snapshot and removes only the
   cutover-owned overlay; with two original Public projects both stay stopped because they cannot
   bind 80/443 at the same time. It never runs `docker compose down -v` or deletes volumes.
3. `app\Invoke-SparrowServer.ps1 -Action Attach -PlanDirectory <plan>` attaches a clean manager bundle
   to a completed cutover (stored as `.sparrow-attached.json`, no credentials). Attached projects are
   managed with `-Action Start|Stop|Restart|Status|Logs|Verify -Component Node|ControlPlane|Proxy|Combined`
   using `docker compose start/stop` on the existing containers only; each action re-verifies the
   transaction, source files, volumes and container ownership and fails closed on any mismatch.

`-Apply` requires a plan directory with `cutover-readiness.json` and a candidate Caddyfile. The
readiness check that produced this plan is **not** part of the current source tree, so a new cutover
plan cannot be prepared with this bundle alone. None of these steps is certified for production
migration; Windows/Docker/live external DNS, TLS, WebSocket and FCM tests remain outstanding.

## Public Caddy without an owned domain

Select **Public** and keep **Automatic public hostnames (free sslip.io names)** checked (command line:
`--auto-dns`). The engine obtains the externally observed public IPv4 through HTTPS, constructs
distinct `node-<IPv4-dashed>.sslip.io` and `control-<IPv4-dashed>.sslip.io` DNS names and checks that
each resolves to the observed IP *before any installation change*. The free hostname is supplied by
the independent **sslip.io DNS service**; **Caddy is the shared HTTPS/WSS reverse proxy and
certificate manager**, not a DNS provider. Manual, user-owned hostnames remain available by clearing
the checkbox.

After Install, the **Server** card displays the installed Control Plane, Community Node and gateway
(`wss://.../v1/gateway`) addresses with individual **Copy** buttons, read from the installed
`sparrow.conf`. These are configured URLs, **not proof** of outside connectivity or successful ACME
certificate issuance. Always test from a different internet connection, with real HTTPS certificate
checks and WebSocket messaging. Caddy requires incoming TCP 80/443 (or matching forwarded ports); a
public IP reported by a lookup service does not prove that the router has a globally reachable IPv4
(CGNAT/DS-Lite can prevent IPv4 port forwarding). Public DNS services and certificate authorities have
availability/rate limits. Caddy's local/internal certificate is NOT a substitute for a public,
Android-trusted certificate.

**Changed public IP:** every **Start / apply settings** of an automatic-hostname installation first
runs `app/public_ip.py`. When the WAN IPv4 changed and the new `control-<ip>.sslip.io` name resolves to
it, the generated `node-`, `control-`, managed node and marked `directory-` sslip.io names in
`sparrow.conf`, `.env.runtime` and the shared Caddyfile are moved to the new address; the previous
files are kept under `backups/public-ip-<timestamp>/`. Identities, secrets, databases and volumes stay
unchanged. **Custom DNS names are never changed.** If the IP cannot be detected or the new name does
not resolve yet, nothing changes and the server starts with its current addresses. Apps that only know
an old Directory sslip.io URL need the new one once in their settings (see `README_RUNTIME.md`).

An existing LAN deployment cannot simply be reinstalled or converted with the fresh-Public installer:
preserve its `.env.runtime`, identities, TLS assets, queues, databases, and volumes. Running a second
copy of the same Compose projects alongside that local installation on the same Docker host is not
supported by this installer.

## Command line (Linux and macOS)

Running `./Start-SparrowServer.sh` with arguments uses the engine directly:

```bash
./Start-SparrowServer.sh install --component combined --mode lan
./Start-SparrowServer.sh install --component combined --mode public --auto-dns
./Start-SparrowServer.sh install --component combined --mode public \
  --node-domain node.example.com --control-domain control.example.com
./Start-SparrowServer.sh status --component combined
```

Replace the example names with your own domains; the manager creates a **shared** Caddy entry point.
The public installer always installs both components; `--component node`, `control-plane` and `proxy`
are lifecycle selections for internal diagnostics. Independent nodes are managed with
`./Start-SparrowServer.sh nodes ...`. Do not change an installed custom Public hostname through these
commands; to add a Public component to an existing proxy, plan the route/TLS change separately.

The Linux/macOS engine cannot run the Windows PowerShell legacy migration, offline backup, or
cutover-attachment workflows. It deliberately rejects other Compose working directories and orphaned
named volumes instead of attempting migration. Never run a fresh bundle against volumes/containers
from the older standalone installers. Protect `.env.runtime`, `sparrow.conf`, the `secrets/`
directories, the proxy's Caddyfile, and the original Docker volumes.

## GitHub builds and releases

`.github/workflows/sparrow-server-bundle.yml` (pushes touching `server/**`, or manual
`workflow_dispatch`) publishes the six server images to GHCR with an internal tag, builds
`sparrow-server.zip` with `server/installer/build.py`, checks that the Directory server is not in it,
and uploads the internal artifacts `sparrow-server-internal-windows` and
`sparrow-server-internal-linux`. Only these internal artifacts additionally contain the operator-only
Directory installer under `directory/`.

`.github/workflows/release.yml` (tags `v*`, branches `release/**`) publishes the GitHub release with
`sparrow-server-<version>-windows.zip`, `sparrow-server-<version>-linux.zip` (both without the
Directory) and `SHA256SUMS.txt`; **Update to latest release** downloads from there. A successful
bundle build is **not** a successful GHCR image publication: the server images referenced by
`SPARROW_IMAGE_PREFIX` / `SPARROW_IMAGE_TAG` must be available from the registry for the chosen Docker
CPU architecture. If the GHCR package is private, the operator must have pull authorization. A failed
pull does not mean that existing Docker volumes should be deleted or that an image update succeeded.

### Windows Docker template error: `function "com" not defined`

PowerShell 5.1 can strip embedded double quotes inside native Docker Go-template
arguments. The engine reads Compose ownership labels from Docker's JSON
inspection output instead. Update the installation and use the updated installer;
no Docker volumes or original server directories need to be deleted for this fix.
A `#< CLIXML` progress record in a redirected PowerShell stderr log is diagnostic
serialization, not a Docker deployment failure.

### Start and live node-directory changes

An installed node can change its Control Plane directory URL through the **Directory Server URL**
field: Start fetches the directory, updates `CONTROL_PLANE_DIRECTORY_URL` and the three
Control Plane routing/advertisement values in the installed `.env.runtime`, then
reloads the existing mailbox, federation, and gateway containers with
`--pull never` for this configuration-only change. No new secrets, identities,
PostgreSQL/Redis volumes, or Compose project names are created.

For a Combined LAN installation, `http://localhost:8390` is valid **on the
host**, but it must not be returned to an Android phone by the node's
`/v1/control-planes` endpoint. Fresh bootstrap and in-place discovery refresh
advertise the node's existing LAN address (for example
`http://192.168.178.60:8390`) while preserving Docker-internal routing.
Query `http://<node-ip>:8490/v1/control-planes` to verify the
**actual running gateway**; checking the Control Plane's `/v1/nodes` only proves
that the Node registered, not which Control Plane URLs the Node advertises.
If the configured directory *itself* still lists an old IP, correct its
content at the source: the installer must not silently rewrite a trusted
operator-provided directory into a different network.

For a truly fresh disposable test installation with matching orphaned Docker
resources but no recoverable `.env.runtime`, Install
replaces only the selected orphaned TEST resources automatically, per the
existing test-reset restrictions. **Never use this as an update mechanism**:
it deletes those orphaned databases and identities. An intact installation with
its original runtime settings takes the non-destructive update path instead.

### Operation logs

The installer window shows each task's output in its activity log; **Open logs folder** opens
`<installation>/logs/`. On Linux/macOS the engine writes one `sparrow-server-<time>-<pid>.log` per
action into that folder (owner-only permissions). Logs are deployment-local diagnostics and may
contain sensitive paths or runtime data: keep them private and do not include this directory in
installer distribution bundles.

The installer checks/creates the shared Docker network before it considers
removing abandoned test resources. A missing `sparrow-public-edge` is not an
installation failure; it is created as needed. **This does not authorize automatic
cleanup of any existing deployment**: retain the original installation folder,
server identities and volumes; use an isolated test deployment for destructive tests.
