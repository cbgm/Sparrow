# Local development on Windows and macOS

This is the practical “bring everything to life” guide for the current repository **before the first official tagged release exists**.

For normal app/network testing use:

- one Control Plane;
- at least one Community Node;
- preferably two Community Nodes for federation/failover tests;
- two or more Android emulators/devices.

## 1. Configure the Android build

Repository-root `local.properties`:

```properties
controlPlaneDirectoryUrl=https://gist.githubusercontent.com/cbgm/26bb9651e7d2d3fd464df02e8808387f/raw/522436a432e48b9f53f3210b76278e2217f126f8/gistfile1.txt
```

Keep your normal `sdk.dir=...` line too.

The response may be `text/plain`; its body is parsed explicitly as JSON.

## Windows: easiest path

### Control Plane

The preferred operator experience is the generated bundle. If you have a CI/release image tag, create it from the repository root:

```powershell
.\server\scripts\New-ControlPlaneBundle.ps1 `
    -ImagePrefix ghcr.io/cbgm/sparrow `
    -ImageTag <image-tag>
```

Extract the generated ZIP and run:

```text
Start-SparrowControlPlane.cmd
```

The launcher shows saved values as prefilled configuration, starts Docker Desktop if needed, prepares secrets/state and waits for readiness.

Then open:

```text
http://<control-plane-host>:8390/index
```

### Community Node

Generate:

```powershell
.\server\scripts\New-CommunityNodeBundle.ps1 `
    -ImagePrefix ghcr.io/cbgm/sparrow `
    -ImageTag <image-tag>
```

Extract and run:

```text
Start-SparrowNode.cmd
```

Enter the same Control Plane **directory URL**, not a hardcoded list of planes. Then open:

```text
http://<node-host>:8490/index
```

### Android

Build:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

Run `androidApp` from Android Studio or install the APK under `androidApp/build/outputs/apk/debug/`.

## macOS: app + Community Node bundle + Control Plane from source

### Android

```bash
./gradlew :androidApp:assembleDebug
```

Android Studio can run the same `androidApp` target as on Windows.

### Control Plane from source

The friendly Control Plane release bundle is currently Windows-only. Install Docker Desktop and provide a valid Firebase Admin service-account JSON file because the current Control Plane Compose stack mounts it into the push service.

```bash
export FIREBASE_ADMIN_CREDENTIALS=/absolute/path/to/firebase-admin.json
docker compose -f server/control-plane/docker-compose.yml up -d --build
```

Watch startup:

```bash
docker compose -f server/control-plane/docker-compose.yml ps
docker compose -f server/control-plane/docker-compose.yml logs -f
```

Open:

```text
http://localhost:8390/index
```

If Android devices/emulators or another machine need the Control Plane, use the Mac's reachable LAN/public address in your directory JSON rather than `localhost`.

### Community Node release-style launcher

A release candidate/full release can provide a macOS/Linux Community Node package containing:

```text
Start-SparrowNode.command
start-sparrow-node.sh
```

After extracting:

```bash
chmod +x start-sparrow-node.sh bootstrap-community-node.sh
./start-sparrow-node.sh
```

The script configures the deployment and Control Plane directory. Open the resulting node `/index` page.

### Community Node directly from source (advanced)

If you do not have a launcher bundle/image tag yet, you can run the source Compose stack by supplying the runtime endpoints explicitly. Use an address reachable by your test clients/other nodes (`<LAN_IP>` below):

```bash
export CONTROL_PLANE_URL=http://host.docker.internal:8390
export CLIENT_ENDPOINT=ws://<LAN_IP>:8490/v1/gateway
export FEDERATION_ENDPOINT=http://<LAN_IP>:8490
export MAILBOX_ENDPOINT=http://<LAN_IP>:8490

docker compose -f server/community-node/docker-compose.yml up -d --build
```

Then open:

```text
http://<LAN_IP>:8490/index
```

For more than one node, use independent Compose project names/ports/volumes or the repository smoke-test tooling instead of starting identical projects on the same ports.

## Server-development stack

`server/docker-compose.yml` is useful for service development/smoke work because it exposes internal diagnostic ports directly. It is **not** the same operator-facing topology as the two Caddy launcher packages.

It requires a Firebase Admin credential path in `server/.env` or the shell environment:

```dotenv
FIREBASE_ADMIN_CREDENTIALS=/absolute/path/firebase-admin.json
```

Start:

```bash
docker compose -f server/docker-compose.yml up -d --build
```

For production-shaped app testing prefer the Control Plane/Community Node deployments above because they expose the same Caddy routes used by launchers/releases.

## Verify the topology

Control Plane:

```text
/index -> registry / presence / push / nodes
```

Community Node:

```text
/index -> gateway / advertised planes / federation / mailbox
```

Android Developer Settings should show:

- reachable Control Planes;
- verified nodes;
- the current node;
- live connection counts;
- recently failed nodes as `COOLDOWN` with `0` connections.

## Two-node smoke test

On Windows/PowerShell:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 -BuildImages
```

This is the fastest way to verify registration, two independent identities, federation and destination storage.

## Troubleshooting order

1. Open Control Plane `/index`.
2. Open Community Node `/index`.
3. Run `docker compose ps` for the failing deployment.
4. Inspect only the failing service logs first.
5. Check Android logcat around `DefaultTransportConnectionManager` and `DefaultWebSocketTransportClient`.
6. If Caddy returns 502, inspect the upstream container instead of changing routes immediately.

## Emulator networking

`localhost` inside an Android emulator is the emulator itself. `10.0.2.2` reaches the development host from the standard Android emulator, but Sparrow's normal flow should still use the configured Control Plane directory and signed node descriptors rather than a hardcoded gateway URL.
