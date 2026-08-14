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
- direct/group conversation state;
- packet creation and packet processing;
- end-to-end encryption/decryption;
- persistent outgoing outbox;
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

Apps and Community Nodes do not keep a hardcoded list of Control Plane addresses. They start from one directory
URL whose body looks like:

```json
{
  "controlPlanes": [
    "https://plane-a.example.com",
    "https://plane-b.example.com"
  ]
}
```

The HTTP `Content-Type` is irrelevant; the response body is explicitly parsed as JSON.

For local builds, the directory URL is stored in `local.properties` as `controlPlaneDirectoryUrl`. The shared
module exposes it through `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL`, and `AppViewModel` owns synchronization and
health maintenance.

## Read next

- [Installation](installation.md)
- [First build](first-build.md)
- [Project structure](project-structure.md)
- [Server overview](../server/overview.md)
- [Architecture](../architecture/overview.md)
