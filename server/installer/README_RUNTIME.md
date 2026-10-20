# Sparrow server installer (Windows, Linux, macOS)

Open the installer:

- Windows: double-click `Start-SparrowServer.cmd`
- macOS: double-click `Start-SparrowServer.command`
- Linux: run `./Start-SparrowServer.sh`

The installer window is the same on all three systems. It needs Python 3.9+
(Windows: python.org installer; most Linux desktops need the `python3-tk`
package). On a machine without a screen, for example over SSH, the same options
appear as a text menu. Every action can also be run directly from the command
line (see below).

### Starting from the source code

The same launchers in `server/installer` of a source checkout open a short
**Create your Sparrow server** step instead (window or text menu): choose a new
or empty folder (default `~/Sparrow Server`), the installer creates it with
`server/installer/build.py --install <folder>` using the published server
images and opens there. Nothing is installed into the source tree. When a
Sparrow server already runs in Docker from another folder, the new folder shows
**Move server here** (see "Updating" below) and Install stays disabled until it
has been moved.

On Linux/macOS, install a fresh combined public server with shared Caddy and automatic sslip.io hostnames:

    ./Start-SparrowServer.sh install --component combined --mode public --auto-dns

Use the same `install` action in the original installed folder to start/update without clearing server identity, stored messages, databases, or volumes. To configure Firebase Admin (push and phone verification, see below) on that deployment:

    ./Start-SparrowServer.sh install --component combined --mode public --firebase-file /outside-the-installation/firebase-admin.json

Management:

    ./Start-SparrowServer.sh status --component combined
    ./Start-SparrowServer.sh stop --component combined
    ./Start-SparrowServer.sh start --component combined
    ./Start-SparrowServer.sh logs --component combined

The installed public addresses are printed by `status` and `install`. Operation logs are stored under ./logs in the installation folder. Public HTTPS and message delivery require external validation on the actual server.

### Dynamic public IPv4 and automatic sslip.io names

When an installation was created with **automatic sslip.io hostnames**, every **Start / apply settings** in the installer (window or text menu, all platforms) first detects the current WAN IPv4 (`app/public_ip.py`). If it changed, Sparrow rotates only its generated `node-<ip>.sslip.io`, `control-<ip>.sslip.io`, managed `node-<id>-<ip>.sslip.io`, and marked `directory-<ip>.sslip.io` endpoints. Existing signing identities, secrets, databases, FCM credentials and Docker volumes are retained. Nothing changes when the new IP cannot be detected or its `control-<ip>.sslip.io` name does not resolve to it yet; the server then starts with its current addresses. Custom/manual DNS names are never rewritten; backups of the previous settings are kept under `backups/public-ip-<timestamp>/`. The Control Plane then re-registers the same identity at its new endpoint and Community Nodes re-advertise their new endpoints.

If the **independent Directory itself** is hosted on the same dynamic residential IP and Android knows it only through an old `directory-<old-ip>.sslip.io` bootstrap URL, the client has no stable address from which it can discover the new Directory URL. For unattended recovery, host the Directory under a stable DNS/DDNS name (or on stable external infrastructure). Endpoint rotation preserves the Directory signing identity; this limitation is only about finding the new network address.

Only the explicit `reinstall-public --component combined --mode public --confirm-delete-data` action deletes data and generates fresh server identities. Never use this for a server whose stored state must be retained. Never extract a new distribution ZIP over a configured deployment; update the manager scripts in its original folder and keep runtime files, secrets, and volumes.

Combined Public installation always installs both services. The Community Node-only manager installs no Control Plane. In Combined Public mode, set the
editable **Directory Server URL** to the operator-managed HTTPS base address;
it is saved in each component's `sparrow.conf`, so it survives normal updates.
When the bundle is built with `CONTROL_PLANE_RELEASE_DIRECTORY_URL` (or
`CONTROL_PLANE_DIRECTORY_URL`) it prepopulates this field. Do not put a legacy
static JSON-list URL there. The independent directory's separately authenticated
Ed25519 X.509 DER **public** key must be provisioned at bundle build time via
`CONTROL_PLANE_DIRECTORY_PUBLIC_KEY` (unpadded base64url), or set manually in
both components' `sparrow.conf` before installation. Changing the editable
Directory Server URL does **not** change the trusted public key. Never fetch
this trust pin from the directory you are trying to verify.

A public server always starts its own paired Control Plane without needing
online directory access. When Python 3 and its `cryptography` package are
available, the signed directory client verifies `/v1/control-planes`, persists
its last verified snapshot as `community-node/.control-plane-directory-verified.json`
and uses that signed cache if the directory is offline. The cached information
is a discovery hint, not authorization to change a previously pinned Control
Plane identity; an expired snapshot must not authorize new trust decisions.
If the signer pin or crypto dependency is absent, no unsigned directory list
is consumed; the paired Control Plane remains available. The installer itself
needs Python 3 on every system; the host-side signed directory helper
additionally needs the `cryptography` package.

This is an incremental implementation: Control Plane registration and the
signed Install/Start cache exist, and Public mode additionally starts an
independent background synchronizer. Android authenticated discovery and dynamic federation routing have source implementations,
but Android/Kotlin compilation and multi-plane live verification are outstanding;
full identity-rotation recovery is not yet implemented.

The independent Directory Server itself and all its admin credentials are
**not** in the public server bundle.


### Independent Community Nodes — Node only and multi-node

Click **Nodes...** in the installer window (text menu: **Community Nodes**),
or use the `nodes` subcommand on Linux/macOS (`app/Manage-SparrowNodes.py` on
every system). Signed directory lookups additionally require the Python
`cryptography` package; explicit manually configured Control Plane URLs require
Python 3 only. A Node-only install does **not** create a Control Plane or register
with the independent central directory server; it registers each node with
its configured existing Control Plane using the existing node protocol.

For example, to create two publicly addressable nodes on one Docker host:

    ./Start-SparrowServer.sh nodes add --name "Node A" --mode public --auto-dns \
      --control-plane-url https://existing-control.example.org
    ./Start-SparrowServer.sh nodes add --name "Node B" --mode public --auto-dns \
      --control-plane-url https://existing-control.example.org
    ./Start-SparrowServer.sh nodes list
    ./Start-SparrowServer.sh nodes install --id <node-a-id>
    ./Start-SparrowServer.sh nodes install --id <node-b-id>
    ./Start-SparrowServer.sh nodes status --id <node-a-id>

When an existing Combined Control Plane is installed in the same runtime
folder and the Control Plane URL is left empty, **Add...** uses its public
HTTPS origin (or its advertised LAN origin when applicable); no signing-key
field, second plane or separate directory is needed for the usual multi-node
setup. The window never asks for a verification key. A matching independently
provisioned trust pin is reused from the release bundle or existing Combined
configuration. It is **not**
downloaded from the directory URL; an untrusted custom URL is rejected rather
than accepted without signature verification.

For a custom independent directory, an advanced operator may use
`--directory-url` together with an **independently obtained**
`--directory-public-key` on the CLI, or provision a matching trusted URL/key
pair when building the bundle. Set `--control-plane-url` as well if you want
manual fallback addresses. The per-instance client authenticates
the signed directory and retains its last verified cache; the optional
per-instance Docker worker refreshes that cache without holding node identity
keys. To edit an existing instance's Directory URL without changing its
selected registration target or its signer pin:

    ./Start-SparrowServer.sh nodes configure-directory --id <node-id> \
      --directory-url https://directory.example.org

An installed node's signing identity, selected Control Plane registration
origin, existing database and named volumes are not replaced by this edit.
Changing a pinned directory signing key or switching its registered Control
Plane requires a **separate authenticated migration**, not an ordinary Update.
The central directory lists Control Planes (`/v1/control-planes`), whereas
an individual Control Plane publishes its registered nodes (`/v1/nodes`).

Use `nodes start|stop|update|status|logs|remove --id <node-id>` to manage
one node without touching another. **Remove** stops and unpublishes a node
but deliberately keeps the instance folder, credentials and all Docker
volumes; `start` restores it. Public nodes use a shared Caddy on TCP 80/443
with distinct TLS hostnames and independent routes per node. The public
proxy remains running when Combined is stopped if independent node routes
exist. The installer cannot take over an unrelated proxy that already binds
80/443. In LAN mode, each node advertises a separate local host port, starting
at 8500 and increasing by 5 for each additional managed instance. Check
that the chosen ports and the advertised LAN address are reachable from
clients. Public DNS, TLS/ACME, HTTPS/WSS, node registry publication, Firebase
wakeups, message delivery, and multi-host behavior still require live tests
on the target deployment.

Node-only instances are stored under `node-instances/<id>` in the **original
installation directory**. Never extract a new installer ZIP over this
configured folder or delete its named volumes as part of a routine update.

### Public Control Plane registration (directory opt-in)

With a configured HTTPS `CONTROL_PLANE_DIRECTORY_URL` and an independently
provisioned `CONTROL_PLANE_DIRECTORY_PUBLIC_KEY`, Combined Public Install/Start
attempts registration after starting the public HTTPS proxy. It uses the
**existing** `control-plane/secrets/registry-root.identity` without generating a
new key. A short-lived signed proof is exposed only at
`/.well-known/sparrow-directory-registration/<challengeId>`; the proof is removed
immediately after submission, even on failure. Its source directory is distinct
from `secrets/` and is read-only inside Caddy. A valid proposal is **approved automatically** after the central Directory Server
verifies domain/HTTPS endpoint ownership and Ed25519 proof of signing-key possession.
Revocations and identity/endpoint changes still require separately authorized
operator action. The directory administrator token, database and signing key are
never part of this bundle.

Registration now runs in a dedicated, one-shot Docker image (built on the first
public Install/Start) on Windows, Linux, and macOS. The host installer needs no
`cryptography` package for registration. Only the existing Control Plane root
identity FILE is mounted read-only for this short-lived registration job; it has
no directory admin credentials or Docker socket. The public proof directory is
mounted separately and is cleaned up even if registration fails. Docker image
build/network failure defers registration without interrupting the paired server.
The legacy host-based initial directory-refresh helper still requires Python 3
and `cryptography` where present; the separate background sync worker runs its
own Python/cryptography image without host dependencies. A directory
outage or registration rejection never blocks existing message delivery. Server
URL changes never change its trust key. Each Install/Start also maintains a separate last-good authenticated snapshot
in `control-plane/.control-plane-directory-verified.json`. Directory failure
never deletes either this snapshot or the node snapshot. Automatic 15-minute
background refresh now runs in an isolated optional Docker worker (its own
Python 3 + cryptography image is built on first public installation), without
requiring host Python on Windows for this polling. Android discovery requires deployment validation; do not advertise federation
discovery as release-ready until both server and Android flows are tested.

### Continuous directory synchronization (Step 2D)

Public Combined installations attempt to start `directory-sync`, an optional
separate Docker container in the shared-proxy project. It uses ONLY public
verification material; it has no Docker socket, private server identities,
directory operator credentials, or public listening port. The worker refreshes
independent **persistent named-volume** signed snapshots for the paired Control
Plane and Community Node at startup and approximately every 15 minutes. The
node's public gateway reads the worker's verified URL list for its discovery
endpoint on each HTTP request; directory additions/removals need no gateway
restart. The gateway always retains the local paired Control Plane even if
there is no verified list. Failures of the directory or worker do not affect
message transport. The signed snapshot may become stale during an outage; an
expired snapshot is only a discovery hint for previously verified identities.

The worker is a distinct PUBLIC CLIENT, NOT the central directory server. Its
Docker image requires a first-time online build of the pinned `cryptography`
package; if that build fails, installation continues with the paired server
and signed host caches, but autonomous polling is unavailable until started.
The worker does not install Python on the Windows host. Initial registration uses the separate one-shot Docker client without any
host Python cryptography dependency.

### Runtime routing from the verified directory (Step 2F)

The signed directory worker now atomically publishes public origins together
with each Control Plane's verified root identity. Gateway, Federation and
Mailbox mount this publication **read-only**. Newly approved/remotely removed
Control Planes enter/leave the dynamic endpoint pools without restarting the
Java backends; the local paired Control Plane and explicitly configured/manual
origins remain independently configured. Federation combines signed node
registries across all active Control Planes instead of stopping after the
first one. For newly discovered Control Planes it also binds the signed node
directory's registry root to the key in the verified worker publication.

The worker retains its persistent, verified last-good snapshot when the central
directory is down, so the publication remains available across restarts. A
verified revocation removes dynamically discovered origins and their cached
node descriptors, but does not silently delete any separately manually pinned
plane. A stale snapshot is a discovery hint for previously authenticated
identities, not authorization to enroll or rotate a new identity.

The signed directory worker is a local trust boundary: the Java consumers read
its read-only, identity-bearing publication but do **not** independently
verify the central snapshot signature. Do not give other containers write
access to the node directory cache. Windows/Linux/macOS Docker networking,
end-to-end delivery, CP registration propagation, identity recovery and Android
signed-directory integration still require live validation; the full network
is not production-ready simply because these source integrations exist.

### Registration client isolation (Step 2E)

`directory-register` is an opt-in **one-shot** Compose profile, not a running
server and not part of the signed-directory background worker. Only its
Dockerfile and public client sources are in the public bundle. The directory
server source (`server/control-plane-directory/`), directory operator/admin
credentials and deployment files are never bundled. No server key is copied into
its image; the only key mount is the existing root identity read-only, only for
the lifetime of the registration attempt. The verified background worker does
not have this mount. Docker image build errors cannot interrupt local messaging.

### Updating

The **Updates** card of the installer updates the installation in place. Every
update keeps `sparrow.conf`, `.env.runtime`, secrets, identities, databases and
Docker volumes; only manager code, Compose/bootstrap templates and backend
images change. The previous code is saved under `.sparrow-code-backups/`.

- **Update to latest release** downloads the newest published GitHub release
  for your platform, verifies it against `SHA256SUMS.txt`, installs its code and
  applies its server images.
- **Build from source...** (needs Python 3) asks once for your Sparrow source folder (it is
  remembered), builds all six server images from that working tree with Docker
  (`server/installer/build.py --local-images`) and applies them with
  `--pull never`. Docker still downloads build tools and dependencies that are
  not cached. Local builds update existing installations; install first.
- **Apply ZIP...** applies a `sparrow-server.zip` (release or local build).
- **Move existing server here...** (shown in a new, not yet installed folder;
  when a running server is found, a **Move server here** banner replaces the
  Updates card) finds the server already running in Docker
  (`app/move_installation.py`), copies its settings, secrets, caches, proxy
  routes and node instances into this folder, rewrites the paths inside them and
  recreates the same containers from here. Compose project names, Docker
  volumes (databases, identities, certificates) and the shared network are
  kept. If anything fails, the server is started again from the old folder.
  Click **Start / apply settings** once afterwards, check that messaging works,
  then the old folder can be deleted.

The manager reopens itself after an update so its new code is used. Never
extract a bundle over a configured installation. Applying a release bundle
removes a previously applied local build.

Command line equivalent:

    powershell -NoProfile -ExecutionPolicy Bypass -File app\Update-SparrowServer.ps1 -From Release
    powershell -NoProfile -ExecutionPolicy Bypass -File app\Update-SparrowServer.ps1 -From Source -Path C:\src\Sparrow
    powershell -NoProfile -ExecutionPolicy Bypass -File app\Update-SparrowServer.ps1 -From Zip -Path C:\Downloads\sparrow-server.zip
    powershell -NoProfile -ExecutionPolicy Bypass -File app\Update-SparrowServer.ps1 -From Folder -Path C:\Downloads\sparrow-server

`-From Folder` applies an extracted bundle and also migrates older installations
that still use the previous flat folder layout (`app\Update-SparrowServerBundle.ps1`).
Moving a running server: `python app\move_installation.py --from <old folder>`
in the new folder.

### Updating (Linux and macOS)

    ./Start-SparrowServer.sh update --release              latest GitHub release
    ./Start-SparrowServer.sh update --source ~/Sparrow     build images from your source folder
    ./Start-SparrowServer.sh update --zip sparrow-server.zip
    ./Start-SparrowServer.sh update --folder ~/sparrow-server --installation /srv/sparrow
                                                           move an old installation to the new layout
    python3 app/move_installation.py --from ~/old-sparrow-server   move a running server into this folder

Building from source needs Docker and Python 3 (as does the manager itself).

### Folder layout

    Start-SparrowServer.cmd / .sh / .command   launchers
    README.md
    app/              manager code (replaced by updates)
    control-plane/    Control Plane templates and its runtime state
    community-node/   Community Node templates and its runtime state
    public-proxy/     shared public Caddy proxy
    node-instances/   additional Community Node instances
    logs/             operation logs
    backups/          previous settings, e.g. before following a new public IP

### Exchange rates source

Each Community Node fetches the exchange rates for group expenses once a day for all apps (`GET
/v1/exchange-rates`). The default source is the Frankfurter API
(`https://api.frankfurter.dev/v2/rates?base=EUR`). To use another source in the same format, for
example a self-hosted Frankfurter instance, fill in the empty `EXCHANGE_RATES_URL=` line in the
node's `.env.runtime` and recreate its gateway: for the main node `community-node/.env.runtime` and
click Start; for a managed node `node-instances/<id>/.env.runtime` and **Update** it under
**Nodes...** (its Start does not recreate containers). New nodes get the empty line at installation,
and Start adds it to an existing main node; add it yourself to an older managed node. The installer
keeps the value. An empty value uses the default.

### Phone verification

Phone numbers are verified once in the app during sign-up (Firebase SMS), when
an identity is created or restored. Only the app's first route registration
carries the one-time Firebase ID token; later registrations carry none. The
Control Plane's `presence-directory` checks a token with the Firebase
service-account credentials (the same import as push, mounted as
`GOOGLE_APPLICATION_CREDENTIALS` through `docker-compose.firebase.yml`) and
stores nothing about it:

- no token, or no credentials imported: the registration is accepted;
- a blank token, an invalid or expired token, a token that is not a phone
  sign-in, or a phone alias that does not match the verified number: the
  registration is rejected with HTTP 403 `PHONE_VERIFICATION_FAILED`.

There is no server setting that makes verified phone numbers mandatory. The
Firebase project is `FIREBASE_TARGET_PROJECT_ID` (default `sparrow-a9048`); the
service account needs Firebase Authentication access to it in addition to FCM.
