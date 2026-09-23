# Sparrow unified server installer (Windows, Linux, macOS)

The server bundle contains only the combined Control Plane + Community Node installer, component configuration, and runtime-management scripts. Source-only validation and recovery rehearsal scripts are not distributed.

Windows: Start-SparrowServer.cmd
Linux: ./Start-SparrowServer.sh --help
macOS: ./Start-SparrowServer.command (or ./Start-SparrowServer.sh --help)

On Linux/macOS, install a fresh combined public server with shared Caddy and automatic sslip.io hostnames:

    ./Start-SparrowServer.sh install --component combined --mode public --auto-dns

Use the same `install` action in the original installed folder to start/update without clearing server identity, stored messages, databases, or volumes. To configure Firebase Admin on that deployment:

    ./Start-SparrowServer.sh install --component combined --mode public --firebase-file /outside-the-installation/firebase-admin.json

Management:

    ./Start-SparrowServer.sh status --component combined
    ./Start-SparrowServer.sh stop --component combined
    ./Start-SparrowServer.sh start --component combined
    ./Start-SparrowServer.sh logs --component combined

The installed public addresses are printed by `status` and `install`. Operation logs are stored under ./logs in the installation folder. Public HTTPS and message delivery require external validation on the actual server.

Only the explicit `reinstall-public --component combined --mode public --confirm-delete-data` action deletes data and generates fresh server identities. Never use this for a server whose stored state must be retained. Never extract a new distribution ZIP over a configured deployment; update the manager scripts in its original folder and keep runtime files, secrets, and volumes.

The public installer always installs both services. In Public mode, set the
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
is consumed; the paired Control Plane remains available. On Windows the
federation-discovery helper also requires an installed Python 3 with
`cryptography`; local combined installations do not require Python.

This is an incremental implementation: Control Plane registration and the
signed Install/Start cache exist, and Public mode additionally starts an
independent background synchronizer. Android authenticated discovery and dynamic federation routing have source implementations,
but Android/Kotlin compilation and multi-plane live verification are outstanding;
full identity-rotation recovery is not yet implemented.

The independent Directory Server itself and all its admin credentials are
**not** in the public server bundle.

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
host Python cryptography dependency. ### Runtime routing from the verified directory (Step 2F)

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
