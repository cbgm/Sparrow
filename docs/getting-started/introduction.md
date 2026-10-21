# Introduction

Sparrow is a secure-messaging project with two major parts:

1. an Android-first Kotlin Multiplatform client;
2. a federated Kotlin server system split into Control Planes and Community Nodes.

The main design goal is to keep client chat logic, transport logic and server routing understandable as the
project grows. Direct chats and Group chats deliberately have separate code paths; common code is shared only
where the semantics are actually common.

## Current platform status

| Target | Status |
|---|---|
| Android | Usable development target and current product target |
| iOS | Project/Xcode/KMP source sets exist, but important runtime/platform functionality is not implemented yet |
| Server | JVM/Ktor services running in Docker |

Do not interpret the presence of `iosMain` source sets as feature parity. The iOS host currently exists mainly so
shared code can evolve toward multiplatform support.

## Client responsibilities

The client owns:

- local identity/key material;
- contacts and trust state;
- Direct/Group conversation state and typed message parts;
- attachment selection/blob encryption/transfer/cache/saved-copy management;
- packet creation and packet processing;
- end-to-end encryption/decryption;
- persistent outgoing outbox;
- local exact/semantic message search;
- optional local message-safety analysis;
- node discovery/failover;
- mailbox synchronization and Android push wake-ups.

The server does not receive message plaintext from the application protocol.

## Server responsibilities

A **Control Plane** provides:

- signed Community Node discovery (`node-registry`);
- temporary client presence routes (`presence-directory` + Redis);
- Android push/wake-up infrastructure (`push`).

A **Community Node** provides:

- WebSocket client connections (`gateway`);
- cross-node routing/retry (`federation`);
- recipient-selected offline ciphertext storage (`mailbox`).

Both deployments use Caddy as their public edge and expose an operator `/index` page.

## Control Plane directory

Apps and Community Nodes do not keep a hardcoded list of Control Plane addresses. They start from one
signed directory, configured as an HTTPS origin without a path (for example
`https://directory.example.com`). The app reads `/.well-known/sparrow-directory` (protocol, `keyId`,
Ed25519 `publicKey`) and then `/v1/control-planes`, a signed envelope:

```json
{
  "payload": {
    "protocol": "sparrow-control-plane-directory-v1",
    "version": 7,
    "generatedAtEpochMilliseconds": 1760000000000,
    "validUntilEpochMilliseconds": 1760086400000,
    "controlPlanes": [
      { "controlPlaneId": "<sha256 of publicKey>", "baseUrl": "https://plane-a.example.com", "publicKey": "<base64url DER>" }
    ],
    "revokedControlPlaneIds": []
  },
  "keyId": "<sha256 of the directory key>",
  "signature": "<base64url Ed25519 signature>"
}
```

`SignedControlPlaneDirectoryVerifier` checks the signature over the canonical payload, the timestamps
(at most 24 h validity), HTTPS origins and that each `controlPlaneId` matches its key.
`VerifiedControlPlaneDirectoryState` pins the directory key on the first verified snapshot and
refuses a different key later, rejects version rollbacks and reinstated revoked IDs, and keeps the
last verified snapshot for offline starts. Removing the directory in Settings forgets the pin. An
unsigned `{"controlPlanes": [...]}` list is only accepted as a one-time manual import in Settings
(JSON list).

For local builds, the directory URL is stored in `local.properties` as `CONTROL_PLANE_DIRECTORY_URL`
(release builds: `CONTROL_PLANE_RELEASE_DIRECTORY_URL`) and exposed as
`BuildKonfig.CONTROL_PLANE_DIRECTORY_URL`. It is applied only once, on first start, if no directory is
configured yet. Startup is task based: `LoadControlPlaneConfigurationStartupTask` applies that
default, `RestoreControlPlaneDirectoryStartupTask` restores the verified cached snapshot, and after
navigation `MaintainControlPlaneDirectoryStartupTask` refreshes the directory (every 5 minutes, retry
after 5 s), `MaintainControlPlaneHealthStartupTask` probes the Control Planes and
`ObserveControlPlaneRegistrationTargetsStartupTask` re-registers the push token when the reachable
Control Planes change.

## Read next

- [Installation](installation.md)
- [First build](first-build.md)
- [Project structure](project-structure.md)
- [Current feature status](../features/current-features.md)
- [Server overview](../server/overview.md)
- [Architecture](../architecture/overview.md)
