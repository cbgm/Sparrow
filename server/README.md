# Federated SecureChat server

The `server/` directory contains the first runnable implementation of the federated node architecture.
The obsolete standalone relay application has been removed; client WebSockets are served by `:server:gateway`.

## Modules

| Module | Purpose | Default port |
|---|---|---:|
| `:server:protocol` | Shared serialized API models only | - |
| `:server:security` | Node signing, replay protection, and constant-time internal authentication | - |
| `:server:persistence` | Bounded idempotency and environment infrastructure | - |
| `:server:observability` | Shared request IDs, Prometheus metrics, and health probes | - |
| `:server:node-registry` | Signed descriptors, heartbeats, compatible node directory | 8090 |
| `:server:presence-directory` | Signed, expiring device routes with generation checks | 8091 |
| `:server:mailbox` | Capability-protected, expiring encrypted envelopes | 8092 |
| `:server:federation` | Presence lookup, authenticated forwarding, mailbox fallback | 8093 |
| `:server:gateway` | Client WebSockets and local connection delivery | 8094 |
| `:server:push` | Durable FCM tokens and opaque wake-up identifiers | 8095 |

No service application imports another service application's implementation. Communication crosses
the `server:protocol` contracts and HTTP interfaces.

## Run the local network

Create `server/.env` once. This file is ignored by Git:

```dotenv
FIREBASE_ADMIN_CREDENTIALS=C:/secure/chat-project-firebase-adminsdk.json
PUSH_DATABASE_PASSWORD=replace-for-non-local-deployments
MAILBOX_DATABASE_PASSWORD=replace-for-non-local-deployments
PRESENCE_REDIS_PASSWORD=replace-for-non-local-deployments
NODE_REGISTRY_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_INTERNAL_API_TOKEN=replace-with-a-different-random-token
GATEWAY_INTERNAL_API_TOKEN=replace-with-a-different-random-token
PUSH_INTERNAL_API_TOKEN=replace-with-a-different-random-token
# Used only by docker-compose.multinode.yml:
MAILBOX_B_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_B_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_B_INTERNAL_API_TOKEN=replace-with-a-different-random-token
GATEWAY_B_INTERNAL_API_TOKEN=replace-with-a-different-random-token
COMPOSE_PARALLEL_LIMIT=1
```

Start the network from the repository root:

```powershell
docker compose -f server/docker-compose.yml up --build
```

Configure the Android emulator to use:

```text
securechat.registry.baseUrl=http://10.0.2.2:8090
securechat.relay.httpBaseUrl=http://10.0.2.2:8095
```

The registry URL is used for signed WebSocket node discovery. The push URL remains separate. Leave
`securechat.registry.authorityNodeId` empty for local trust on first use, or pin the registry ID
returned by `/v1/nodes`. The push health response must contain `fcmEnabled=true`.

## Run the two-node federation test

The multi-node override adds a completely separate node B with its own identity, gateway,
federation service, mailbox, PostgreSQL databases, and persistent volumes. Both nodes share only
the central node registry, presence directory, and push service. Node A advertises port 8094 and
node B advertises port 8294. Clients obtain both direct endpoints from the signed registry response;
the former port-8194 Caddy failure-injection edge is no longer used.

Start both nodes from the repository root:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    up -d --build --remove-orphans
```

Wait for both signed descriptors to be registered, then verify the topology:

```powershell
curl.exe http://localhost:8090/health
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
curl.exe http://localhost:8294/health
curl.exe http://localhost:8095/health
```

The registry must report `nodes=2`. Before opening the apps, both gateway health responses should
report `connections=0`.

Build the discovery-enabled app once:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

.\gradlew.bat :androidApp:assembleDebug `
    --no-configuration-cache

& $adb -s emulator-5554 install -r `
    ".\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
```

Install the same APK on the second emulator. Each installation has a different routing ID and chooses
a stable starting node from the verified directory:

```powershell
& $adb -s emulator-5556 install -r `
    ".\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
```

Open both apps and verify that the gateway connection counts add up to two while the shared presence
directory contains two signed routes:

```powershell
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
curl.exe http://localhost:8294/health
```

Node selection is derived from each installation's random routing ID, so the two gateway counts may
be `1 + 1` or `2 + 0`. With three emulators, a split is normally visible immediately. Send a message
between clients connected to different gateways. The compatibility `send_envelope` frame enters
federation whenever the recipient is not connected to the sender's gateway. Confirm the
node-to-node and destination-gateway requests:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m federation federation-b gateway gateway-b |
    Select-String -Pattern "/v1/federation/envelopes|/internal/v1/envelopes"
```

Typing events use a separate ephemeral federation path. They are signed between nodes but are not
stored in PostgreSQL, mailbox, or push. Type from a node A client to the node B client and verify:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m federation federation-b gateway gateway-b |
    Select-String -Pattern "federation/typing-events|internal/v1/typing-events"
```

After both updated clients have connected once, each mutual contact owns a separate expiring mailbox
capability. Confirm that provisioning occurred (with three mutual contacts, the two mailbox counts
together should normally be at least three):

```powershell
curl.exe http://localhost:8092/health
curl.exe http://localhost:8192/health
```

For background delivery, close the receiving app normally without Android's Force stop action, then
send a message from the other node. The federation service stores only the encrypted federated
envelope in the recipient-selected mailbox. The mailbox asks push to send a wake-up identifier; the
Android worker then retrieves, processes, and acknowledges the mailbox envelope. Legacy clients that
have not exchanged a signed mailbox route still use the compatibility push inbox.

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m mailbox mailbox-b push federation federation-b |
    Select-String -Pattern "v1/mailboxes|internal/v1/wake-ups|FCM wake-up|/stored"
```

Mailbox capabilities are revoked when a contact is blocked, a direct conversation is deleted, or a
local or accepted remote identity changes. The client authenticates
`DELETE /v1/mailboxes/{mailboxId}` with the retrieval capability, removes the peer's cached delivery
route, and records an offline revocation for retry on the next foreground connection. Routes last 30
days and are replaced during the final three days; the old mailbox is revoked before the replacement
is committed and shared.

To verify revocation, note the mailbox counts, block a mutual contact, and inspect the mailbox logs:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m mailbox mailbox-b |
    Select-String -SimpleMatch "DELETE - /v1/mailboxes/"

curl.exe http://localhost:8092/health
curl.exe http://localhost:8192/health
```

At least the blocking device's mailbox count must decrease. Once the peer processes the signed
direct-chat authorization revocation, its own per-contact mailbox is revoked as well. A repeated
authenticated delete is idempotent and returns `204 No Content`.

To test failover of node B's gateway and federation processes, keep the apps open and stop them:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    stop gateway-b federation-b
```

Every client connected to node B temporarily blacklists the failed descriptor and selects node A
from the cached signed directory. Within the normal reconnect window, gateway A should own the
migrated connections and messages must continue in both directions. After the 90-second registry
heartbeat grace period, registry health reports `nodes=1`.

Restore node B with:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    up -d federation-b gateway-b
```

The app refreshes the directory every minute, rejects invalid authority or node signatures, rejects
expired and incompatible descriptors, and keeps failed nodes out of selection for 30 seconds. If
a registry is briefly unavailable, the last verified directory has a five-minute grace period.
Clients do not configure a static WebSocket URL. They bootstrap from the signed registries exposed by
the configured control planes, which are the source of compatible gateway endpoints and failover
candidates. Release registries use an offline-root -> registry-authority -> rotating-directory-signer
certificate chain, so the pinned root stays stable while online signing keys rotate automatically.

The trusted registry ID is deliberately retained across app restarts. If local testing deletes the
`registry-identity` Docker volume, clear the app data once before trusting the newly generated local
authority. Production clients must never reset that trust automatically.

The gateway accepts the existing gateway WebSocket frames. Current clients fetch `/v1/gateway`, create
a connection ID, and attach a signed, expiring presence route to the initial `register` frame. They
refresh that route every 30 seconds while the WebSocket remains connected. Older clients still work
locally through the compatibility registration, but they do not publish a cross-node presence route.
Current routing IDs begin with `scrouting1_` and are derived from the device signing public key, not
from its phone number. The presence service verifies that key-to-ID binding before accepting a route.
This is an addressing migration: update and open every test client once so each FCM token is moved to
its new routing ID. Cached contact mappings are replaced automatically from exchanged signing keys.
Legacy pending envelopes addressed to `scphone1_` IDs are not rewritten.

## Run standalone community nodes

The production-shaped deployment is split into two independent packages:

```text
server/control-plane/    registry, presence, push, PostgreSQL, Redis, Caddy
server/community-node/   gateway, federation, mailbox, PostgreSQL, Caddy
```

The packages share no Docker network or volume. A community node reaches the control plane only
through `CONTROL_PLANE_URL`, and all privileged remote requests are signed with the node's persisted
Ed25519 identity. Existing colocated deployments retain the internal-token endpoints for backward
compatibility; community nodes use `/v1/routes/**` and `/v1/node-push/**` instead.

Run the complete local isolation proof from the repository root:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 `
    -BuildImages
```

The script starts one control-plane project and two instances of the community-node project. It
waits for every service, requires two distinct signed registry entries, registers node-owned presence
routes through the public control plane, sends a real encrypted envelope in both directions, and
checks that each destination can read only its routed push queue. It also proves that all three
Compose projects have disjoint Docker networks and volumes, then restarts node A and confirms that
its node identity remains unchanged. It removes the test projects and volumes unless `-KeepRunning`
is supplied.

For manual and production deployment, copy the corresponding `.env.example` and follow:

- [`control-plane/README.md`](control-plane/README.md)
- [`community-node/README.md`](community-node/README.md)

The `Standalone Community Node Smoke Test` GitHub Actions workflow runs the same signed,
bidirectional federation and deployment-isolation proof for pull requests that change the server.

## Automated network smoke test

`Test-SecureChatNetwork.ps1` verifies the complete local Compose topology without changing stored
data. It waits for every PostgreSQL, Redis, and Kotlin container to become healthy, validates every
service health response, checks the expected registry node count, and verifies gateway request-ID
propagation and Prometheus metrics.

Run it against the already running two-node network:

```powershell
.\server\scripts\Test-SecureChatNetwork.ps1 `
    -MultiNode `
    -RequireFcm
```

After a restore, exact durable counts can be asserted as part of the same test:

```powershell
.\server\scripts\Test-SecureChatNetwork.ps1 `
    -MultiNode `
    -RequireFcm `
    -ExpectedNodes 2 `
    -ExpectedMailboxCountA 2 `
    -ExpectedMailboxCountB 2 `
    -ExpectedPushDevices 3
```

Use `-Start` when the script should start the Compose network first. Add `-BuildImages` to rebuild
application images before startup; image rebuilding is never implicit. The smoke test validates the
server topology and durable counts. Signed cross-node messages, typing events, and real FCM delivery
still require the Android client test described above.

The `Server Smoke Test` GitHub Actions workflow runs the same check for pull requests targeting
`develop` when server, Gradle, or workflow files change. CI uses an intentionally invalid local
Firebase credential, so it verifies push-service startup and persistence with `fcmEnabled=false`
without storing a Firebase secret. It builds both nodes, waits for both signed registrations, asserts
empty fresh databases, uploads Compose state and complete service logs on failure, and always removes
the CI containers and volumes afterward. Real FCM delivery remains covered by the local emulator test.

## Security behavior

- A node identity is generated once and persisted in the shared node identity volume.
- `nodeId` is the SHA-256 digest of the Ed25519 public key.
- Node descriptors and heartbeats are signed.
- Federation requests are signed over method, path, timestamp, nonce, and body hash.
- Request nonces are retained for a bounded window to reject replays.
- Gateway, federation, and push internal endpoints use separate credentials and constant-time checks.
- Presence routes are accepted only with a valid client signature and non-stale generation.
- A presence routing ID must match the SHA-256-derived ID of the signing public key in its proof.
- Mailbox IDs and capabilities are random. Only capability hashes are retained.
- Retrieval capabilities can revoke their mailbox; revocation deletes every queued envelope.
- Failed client revocations remain locally marked as pending and are retried after reconnect.
- Encrypted envelope IDs are deduplicated and expired entries are removed.
- Public mailbox creation, push registration, node registration/heartbeat, and federation writes are
  rate-limited by client address with bounded in-memory tracking.
- Mailbox creation also enforces atomic PostgreSQL quotas globally and per hashed client address;
  raw client addresses are not persisted.
- Firebase Admin credentials are mounted only into the push container.

The default development Compose file publishes diagnostic ports only on `127.0.0.1`. They remain
available to the Android emulator through `10.0.2.2`, but are not reachable through the host's LAN
address.

### Abuse protection

Excess requests return `429 Too Many Requests` with a JSON `RATE_LIMITED` error and a
`Retry-After` header. Per-client mailbox quota exhaustion also returns `429`; global mailbox
capacity exhaustion returns `507 Insufficient Storage`. The default limits are:

| Operation | Environment variables | Default |
|---|---|---:|
| Mailbox creation per client | `MAILBOX_CREATION_RATE_LIMIT_REQUESTS`, `MAILBOX_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS` | 30/hour |
| Active mailboxes per client | `MAILBOX_MAXIMUM_MAILBOXES_PER_CLIENT` | 100 |
| Active mailboxes globally | `MAILBOX_MAXIMUM_MAILBOXES` | 100,000 |
| Push device registration per client | `PUSH_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS`, `PUSH_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS` | 60/minute |
| Node registration per client | `NODE_REGISTRY_REGISTRATION_RATE_LIMIT_REQUESTS`, `NODE_REGISTRY_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS` | 30/hour |
| Node heartbeat per client | `NODE_REGISTRY_HEARTBEAT_RATE_LIMIT_REQUESTS`, `NODE_REGISTRY_HEARTBEAT_RATE_LIMIT_WINDOW_MILLISECONDS` | 180/minute |
| Inbound federation writes per client | `FEDERATION_INCOMING_RATE_LIMIT_REQUESTS`, `FEDERATION_INCOMING_RATE_LIMIT_WINDOW_MILLISECONDS` | 1,200/minute |

Each service bounds its rate-limit key map at 100,000 clients by default. Configure the bound with
the service's `*_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS` variable shown in the Compose file. These
fixed-window request limits are process-local. If a service is horizontally replicated, enforce an
additional distributed or edge limit so the deployment-wide allowance does not multiply by the
number of replicas.

`TRUST_PROXY_HEADERS` is false by default. The production override enables it only on public
services whose direct host ports are removed and whose traffic enters through Caddy. Do not enable
it when a service is directly reachable, because an untrusted caller could supply a forged
`X-Forwarded-For` address.

Run the focused automated tests on Windows with:

```powershell
.\gradlew.bat `
    :server:security:test `
    :server:mailbox:test `
    :server:node-registry:test `
    :server:federation:test `
    :server:push:test `
    --no-configuration-cache
```

To inspect a real `429` response, temporarily set the mailbox creation rate to one request per
minute and recreate only that service. The single test mailbox expires automatically after one
hour:

```powershell
$env:MAILBOX_CREATION_RATE_LIMIT_REQUESTS = "1"
$env:MAILBOX_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS = "60000"
docker compose -f server/docker-compose.yml up -d --build --force-recreate mailbox

$expires = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + 3600000
$body = @{nodeId="rate-test"; nodeEndpoint="http://mailbox:8092"; expiresAtEpochMilliseconds=$expires} |
    ConvertTo-Json -Compress
curl.exe -i -H "Content-Type: application/json" -d $body http://localhost:8092/v1/mailboxes
curl.exe -i -H "Content-Type: application/json" -d $body http://localhost:8092/v1/mailboxes
```

The second response must be `429` and include `Retry-After: 60`. Restore defaults afterward:

```powershell
Remove-Item Env:MAILBOX_CREATION_RATE_LIMIT_REQUESTS
Remove-Item Env:MAILBOX_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS
docker compose -f server/docker-compose.yml up -d --force-recreate mailbox
```

## Observability

Every Kotlin service exposes the same operational endpoints in addition to its existing detailed
`/health` response:

| Endpoint | Meaning |
|---|---|
| `/health/live` | The process is running and can serve HTTP requests. |
| `/health/ready` | The service can reach its primary PostgreSQL or Redis storage, where applicable. |
| `/metrics` | Prometheus text-format Ktor/JVM metrics tagged with the service name. |

Compose uses `/health/ready` for application health checks and waits for healthy dependencies before
starting downstream services. The probe runs with the Java runtime already in each image, so the
runtime image does not need `curl` or another package.

Clients may send `X-Request-ID` using 1-128 ASCII letters, digits, dots, underscores, or hyphens.
The server echoes a valid value or generates a UUID when it is missing or invalid. Logs include the
same value as `request_id=...`, which lets an operator follow one request across service logs.

Verify the contract from PowerShell after starting Compose:

```powershell
curl.exe -i -H "X-Request-ID: local-check-1" http://localhost:8094/health/live
curl.exe -i http://localhost:8094/health/ready
curl.exe http://localhost:8094/metrics |
    Select-String -Pattern 'ktor_http_server_requests|service="gateway"'

docker compose -f server/docker-compose.yml logs --since=2m gateway |
    Select-String -SimpleMatch "request_id=local-check-1"

docker compose -f server/docker-compose.yml ps
```

Repeat the readiness request on ports `8090` through `8095` to inspect each local service. In the
two-node topology, node B also exposes mailbox, federation, and gateway readiness on ports `8192`,
`8193`, and `8294`. Prometheus and health endpoints are bound to loopback in development and are not
proxied by Caddy in production; attach a trusted collector to the backend Docker network instead of
publishing `/metrics` to the internet.

## Backup and disaster recovery

`Backup-SecureChat.ps1` creates a timestamped ZIP without stopping message delivery. PostgreSQL
produces consistent custom-format dumps inside each database container and `docker cp` transfers
them without passing binary data through PowerShell. The archive contains:

- node-registry, mailbox, federation, and push PostgreSQL dumps;
- the registry signing identity and node identity;
- node B's databases and identity when `-MultiNode` is selected;
- a versioned manifest with a SHA-256 checksum for every payload.

Create a local single-node backup from the repository root:

```powershell
.\server\scripts\Backup-SecureChat.ps1
```

Enable automatic deletion of backup archives older than fourteen days:

```powershell
.\server\scripts\Backup-SecureChat.ps1 -RetentionDays 14
```

Back up the two-node topology:

```powershell
.\server\scripts\Backup-SecureChat.ps1 -MultiNode
```

For production, use an encrypted backup destination outside the repository:

```powershell
.\server\scripts\Backup-SecureChat.ps1 `
    -Production `
    -OutputDirectory "D:\SecureChatBackups" `
    -RetentionDays 30
```

Retention is disabled by default. A backup never contains `.env`, Compose secret files, or the
Firebase Admin credential. Back those up separately in an encrypted secret store. The database
dumps contain FCM registrations, capability hashes, routing metadata, and encrypted envelopes, so
the backup archive itself is sensitive even though it contains no message plaintext.

For unattended production backups on Windows, register the command with Task Scheduler. This
example runs every day at 03:00; adapt the repository and encrypted destination paths first:

```powershell
$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument '-NoProfile -ExecutionPolicy Bypass -File "C:\SecureChat\server\scripts\Backup-SecureChat.ps1" -Production -OutputDirectory "D:\SecureChatBackups" -RetentionDays 30'
$trigger = New-ScheduledTaskTrigger -Daily -At 03:00

Register-ScheduledTask `
    -TaskName "SecureChat daily backup" `
    -Action $action `
    -Trigger $trigger `
    -Description "Back up SecureChat databases and identities"
```

Validate the newest archive without changing Docker data:

```powershell
$backup = Get-ChildItem .\server\backups\SecureChat-backup-*.zip |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

.\server\scripts\Restore-SecureChatBackup.ps1 `
    -Archive $backup.FullName `
    -ValidateOnly
```

### Fresh-volume restore drill

The restore command is intentionally blocked unless `-ConfirmDataLoss` is present. With
`-RecreateVolumes`, it removes the Compose project's current containers and volumes, starts clean
PostgreSQL volumes, restores every dump and identity, and waits until all application services are
healthy:

```powershell
.\server\scripts\Restore-SecureChatBackup.ps1 `
    -Archive $backup.FullName `
    -RecreateVolumes `
    -ConfirmDataLoss
```

Add `-MultiNode` when restoring an archive created with `-MultiNode`. Add `-Production` for the
production override; `server/.env.production`, `server/secrets`, and the Firebase credential must
already be present because they are deliberately not included in the archive. Add `-BuildImages`
when the source or health-probe implementation changed since the current Compose images were built.
If startup fails, the restore command now prints the state, recent health-probe results, and logs of
each failing application service automatically.

After a local two-node restore, verify the durable state with exact pre-backup counts:

```powershell
.\server\scripts\Test-SecureChatNetwork.ps1 `
    -MultiNode `
    -RequireFcm `
    -ExpectedNodes 2 `
    -ExpectedMailboxCountA 2 `
    -ExpectedMailboxCountB 2 `
    -ExpectedPushDevices 3
```

The mailbox and push counts should match the values captured before the backup. Node counts can be
temporarily zero because restored heartbeats are time-limited; the federation agents re-register
their restored identities and the registry returns to two nodes. Redis presence routes are not
backed up because they expire within minutes and connected clients republish them automatically.

Run a fresh-volume restore drill regularly and after changing PostgreSQL major versions. Merely
creating archives does not prove that they remain restorable.

## Production deployment

Production uses [`docker-compose.production.yml`](docker-compose.production.yml) as an override.
It adds a Caddy TLS edge, removes every direct host port from the Kotlin services, PostgreSQL, and
Redis, separates edge and backend networks, mounts credentials as per-service Compose secrets, and
runs the Kotlin containers with a read-only root filesystem, no Linux capabilities, and
`no-new-privileges`.

Requirements:

- Docker Compose 2.24.4 or newer because the override uses `!reset`;
- a public DNS `A` or `AAAA` record for the SecureChat domain;
- inbound TCP ports 80 and 443, plus UDP 443 for HTTP/3;
- the Firebase Admin JSON file already used by the push service.

Generate independent random database passwords and internal service tokens on the production host:

```powershell
.\server\scripts\New-ProductionSecrets.ps1 `
    -Domain "chat.example.com" `
    -FirebaseAdminCredentials "C:\secure\chat-project-firebase-adminsdk.json"
```

The script does not overwrite existing secrets. It creates ignored files under `server/secrets/`
and `server/.env.production`. Protect and back up those files; losing database passwords prevents a
replacement container from opening the existing database volumes.

Review the merged configuration before starting it:

```powershell
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    config
```

Start production:

```powershell
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    up -d --build
```

Caddy automatically obtains and renews the public certificate. Only Caddy publishes host ports in
the merged production configuration. Public traffic is restricted to these protocol routes:

| Public route | Service |
|---|---|
| `/relay` | Gateway WebSocket |
| `/v1/gateway` | Gateway node information used for signed client routes |
| `/push/*` | Push registration and opaque wake-up retrieval |
| `/v1/federation/*` | Signed node-to-node envelope delivery |
| `/v1/mailboxes/*` | Capability-protected mailbox operations |
| `/v1/nodes/*` | Signed node registry |

Configure clients with:

```text
securechat.registry.baseUrl=https://chat.example.com
securechat.registry.authorityNodeId=<authorityNodeId from /v1/nodes>
securechat.relay.httpBaseUrl=https://chat.example.com
```

Production builds must pin `authorityNodeId`. Trust on first use is intended only to make local
development with a newly generated registry identity convenient.

Verify the public registry and TLS certificate:

```powershell
curl.exe https://chat.example.com/v1/nodes
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    ps
```

Do not copy the local development tokens into production. The production override removes those
environment values and makes every Kotlin service load only the secret files granted to it.

## Push persistence

The push service uses its own PostgreSQL database when `PUSH_DATABASE_URL` is configured. Docker
Compose configures this automatically through the private `push-database` service and retains its
data in the `push-database-data` volume.

The following data survives a push-service or complete Compose restart:

- FCM device-token registrations;
- pending encrypted transport envelopes;
- short-lived opaque wake-up mappings.

Pending envelopes expire after seven days by default. Wake-up mappings expire after fifteen
minutes. Expired rows are removed during normal store access. The limits can be configured with
`PUSH_MAXIMUM_ENVELOPES`, `PUSH_ENVELOPE_RETENTION_MILLISECONDS`, and
`PUSH_WAKE_UP_LIFETIME_MILLISECONDS`.

To verify restart durability, register the Android clients and check that `devices` is non-zero:

```powershell
curl.exe http://localhost:8095/health
docker compose -f server/docker-compose.yml restart push
curl.exe http://localhost:8095/health
```

Both responses must include `persistence=postgresql` and the same device count. Do not use
`docker compose down --volumes` because that explicitly deletes the database volume.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d push-database
$env:PUSH_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5435/securechat_push"
$env:PUSH_TEST_DATABASE_USER = "securechat_push"
$env:PUSH_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:push:test
```

## Mailbox persistence

The mailbox service has its own PostgreSQL database when `MAILBOX_DATABASE_URL` is configured.
Docker Compose configures it through `mailbox-database` and retains the database in the
`mailbox-database-data` volume. Capability hashes, mailbox expiry, and queued encrypted envelopes
survive mailbox-container and complete Compose restarts. Raw send and retrieval capabilities are
never stored.

`DELETE /v1/mailboxes/{mailboxId}` requires the retrieval capability and atomically removes the
mailbox plus its queued envelopes. Deleting an already absent mailbox returns `204`; a wrong
capability returns `401`.

The health endpoint reports both the active adapter and mailbox count:

```powershell
curl.exe http://localhost:8092/health
docker compose -f server/docker-compose.yml restart mailbox
curl.exe http://localhost:8092/health
```

Both responses must contain `persistence=postgresql`. Mailbox and envelope expiry cleanup happens
during normal access. Limits can be configured with `MAILBOX_MAXIMUM_ENVELOPE_BYTES` and
`MAILBOX_MAXIMUM_MAILBOX_BYTES`.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d mailbox-database
$env:MAILBOX_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5436/securechat_mailbox"
$env:MAILBOX_TEST_DATABASE_USER = "securechat_mailbox"
$env:MAILBOX_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:mailbox:test
```

## Presence persistence

The presence directory uses Redis when `PRESENCE_REDIS_URL` is configured. Docker Compose starts a
private `presence-redis` service with append-only persistence and retains its data in the
`presence-redis-data` volume. Active signed routes therefore survive a presence-service or short
Compose restart while their original expiration time is still valid.

Registration generation checks, replacement of older generations, and TTL cleanup are executed
atomically inside Redis. The health endpoint reports the active adapter and unexpired route count:

```powershell
curl.exe http://localhost:8091/health
docker compose -f server/docker-compose.yml restart presence-directory
curl.exe http://localhost:8091/health
```

Both responses must contain `persistence=redis`. Routes expire after at most two minutes by default;
configure this with `PRESENCE_MAXIMUM_TTL_MILLISECONDS`.

The Compose gateway advertises a 90-second route lifetime and refreshes at 30 seconds. After
rebuilding and reconnecting all Android clients, active routes should match active WebSockets:

```powershell
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
```

For example, three connected clients should report `routes=3` and `connections=3`. A route can lag a
new WebSocket by a few seconds only when local identity keys are not yet available; the client retries
signed registration every five seconds.

The optional Redis integration test can be enabled against the Compose instance:

```powershell
docker compose -f server/docker-compose.yml up -d presence-redis
$env:PRESENCE_TEST_REDIS_URL = "redis://:local-development-password@localhost:6380"
.\gradlew.bat :server:presence-directory:test
```

## Node registry persistence

The node registry uses its own PostgreSQL database when `NODE_REGISTRY_DATABASE_URL` is configured.
Docker Compose provides `node-registry-database` and retains its data in the
`node-registry-database-data` volume. Signed node descriptors, heartbeat timestamps, and accepted
heartbeat nonces survive registry-container and complete Compose restarts. Persisting nonces keeps
replay protection effective across restarts.

For the local development Compose stack, registry trust material remains in the separate
`registry-identity` volume. Release control planes instead mount only the root-certified authority
secret and persist rotating directory signers in the dedicated `registry-signing` volume; the offline
root is never mounted into the running release registry. The health endpoint reports the active adapter
and currently healthy node count:

```powershell
curl.exe http://localhost:8090/health
docker compose -f server/docker-compose.yml restart node-registry
curl.exe http://localhost:8090/health
```

Both responses must contain `persistence=postgresql`. A node is included only while its signed
descriptor is valid and its last heartbeat remains inside the configured grace period.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d node-registry-database
$env:NODE_REGISTRY_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5437/securechat_registry"
$env:NODE_REGISTRY_TEST_DATABASE_USER = "securechat_registry"
$env:NODE_REGISTRY_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:node-registry:test
```

## Federation outbound persistence

The federation service uses its own PostgreSQL database when `FEDERATION_DATABASE_URL` is
configured. Docker Compose provides `federation-database` and retains its data in the
`federation-database-data` volume. Pending encrypted envelopes, delivery attempt counts, and the
next retry time survive federation-container and complete Compose restarts.

A retry worker starts with the federation service, immediately loads due rows, and retries online
delivery followed by the recipient-selected mailbox fallback. Failed attempts use exponential
backoff. Delivered and expired envelopes are not retried. Configure the worker with
`FEDERATION_RETRY_POLL_INTERVAL_MILLISECONDS`, `FEDERATION_RETRY_BASE_DELAY_MILLISECONDS`,
`FEDERATION_RETRY_MAXIMUM_DELAY_MILLISECONDS`, and `FEDERATION_RETRY_BATCH_SIZE`.

The health endpoint reports the active adapter and number of pending outbound envelopes:

```powershell
curl.exe http://localhost:8093/health
docker compose -f server/docker-compose.yml restart federation
curl.exe http://localhost:8093/health
```

Both responses must contain `persistence=postgresql`. If the destination remains unavailable, the
same non-zero pending count remains after restart. Do not use `docker compose down --volumes`
because that explicitly deletes every service database.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d federation-database
$env:FEDERATION_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5438/securechat_federation"
$env:FEDERATION_TEST_DATABASE_USER = "securechat_federation"
$env:FEDERATION_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:federation:test
```

All service-owned production persistence adapters are now wired: PostgreSQL for node registry,
mailbox, federation, and push; Redis for presence. Each service still falls back to its in-memory
adapter when its persistence URL is absent, keeping isolated tests and development outside Docker
Compose simple. No service accesses another service's database tables.

## Verification

```bash
./gradlew \
  :feature:transport:allTests \
  :server:protocol:test \
  :server:security:test \
  :server:persistence:test \
  :server:node-registry:test \
  :server:presence-directory:test \
  :server:mailbox:test \
  :server:federation:test \
  :server:gateway:test \
  :server:push:test
```
