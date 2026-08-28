<div align="center">

# Sparrow

**End-to-end encrypted messaging built with Kotlin Multiplatform and a federated Kotlin server stack.**

[![Documentation](https://github.com/cbgm/Sparrow/actions/workflows/docs.yml/badge.svg?branch=master)](https://cbgm.github.io/Sparrow/)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
![Android](https://img.shields.io/badge/Android-API%2029+-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-Not%20usable%20yet-lightgrey?logo=apple)
![Server](https://img.shields.io/badge/Server-Ktor%20%2B%20Docker-blue)
![Architecture](https://img.shields.io/badge/Architecture-Clean-success)
![Compose UI](https://img.shields.io/badge/UI-Compose_Multiplatform-blue)
![Detekt](https://img.shields.io/badge/Quality-Detekt-success)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

## Project status

Sparrow is under active development. **Android is the usable client target.** The repository contains
iOS/Kotlin Multiplatform source sets and an Xcode host, but iOS is **not a supported or usable app target yet**:
important platform/runtime integrations are still missing, so feature parity with Android must not be assumed.

A complete release workflow now exists, but **there is currently no official tagged GitHub release with the
full downloadable package yet**. Until the first `v*` tag is published, build from source or use CI artifacts.

## What makes Sparrow different?

Sparrow combines client-side end-to-end encryption with a **federated, independently hostable transport
network**. Multiple Control Planes can advertise authorized Community Nodes, and clients can fail over between
nodes instead of depending permanently on one mandatory messaging server.

That is somewhat similar to Tor's community-relay philosophy, but Sparrow is **not an onion-routing anonymity
network**: a client uses one Community Node at a time and encrypted messages may federate to another recipient
node. The goal is infrastructure independence/resilience, not Tor-style source anonymity.

This also does **not** justify a blanket claim that Sparrow is cryptographically safer than Signal. Signal is a
much more mature and heavily scrutinized secure messenger. Sparrow's potential advantage is narrower: it can
reduce risks around **centralized routing infrastructure, single-operator outages, blocking, and mandatory trust in
one transport provider** while keeping normal message content encrypted end to end. WhatsApp also uses strong
end-to-end encryption; Sparrow's differentiator is the independently hostable/federated infrastructure model.

Read [What makes Sparrow different?](docs/why-sparrow.md) for the Tor/Signal/WhatsApp comparison, threat
boundaries, and the exact classes implementing discovery, failover, federation and encryption.

## What currently works on Android

The current codebase implements:

- onboarding, local identity creation, Android-protected private-key storage, identity sharing/import and QR/safety-number verification;
- device-contact import/linking, contact invitations, accept/decline/block, identity exchange and contact verification;
- Direct chats with encrypted messages, persistent outbox/retry, sent/delivered/read state, typing, unread state and two-day queued re-authorization handling;
- Group chats with invitations, membership activation, add/remove/promote/leave/admin-transfer flows, epoch security, typing and per-recipient delivery/read aggregation;
- typed message content across data/domain/presentation (`MessagePartDto` -> `MessagePart` -> `MessagePartUi`);
- encrypted blob attachments for images, videos, files, current location and shared contacts; selectable media/files support up to 8 attachments per message, while location/contact are single-shot attachment messages;
- gallery/camera/file selection, media thumbnails, non-autoplay video viewing, swipeable attachment viewing, file opening, current-location sharing and contact sharing;
- received attachment storage/management, per-conversation media/files storage and media export; location/contact payloads remain attachment blobs but are excluded from saved media/file copies;
- contact attachment selection through the existing Contacts UI; contact bubbles show name/number and can add the shared contact to device contacts after confirmation;
- local message search with exact matching plus optional on-device semantic search backed by a verified local MediaPipe text-embedding model;
- optional on-device message-safety analysis with structural checks plus the local embedding model, warning details and block action;
- Settings controls for semantic search/message safety, attachment storage management and a timestamped developer error log with clear action;
- signed Control Plane discovery, multiple Control Planes, health monitoring, node failover, cooldown diagnostics and automatic reconnect;
- WebSocket foreground delivery, mailbox-backed offline delivery and Android FCM wake-ups;
- Control Plane and Community Node launcher bundles, Docker deployment, health/readiness endpoints, metrics, request IDs and smoke tests;
- incremental release-candidate packaging plus full tagged GitHub releases.

See [Current feature status](docs/features/current-features.md), [Attachments](docs/features/attachments.md), [Chats](docs/features/chats.md), [Search](docs/features/search.md), [Message safety](docs/features/message-safety.md), and [Transport](docs/features/transport.md) for details and limitations.

## The system in one picture

```mermaid
flowchart LR
    A[Android app] -->|directory JSON| D[Control Plane directory]
    A -->|HTTPS| CP[Control Plane]
    A -->|WSS /v1/gateway| N1[Community Node]
    N1 -->|signed registration / presence / push| CP
    N1 -->|federation| N2[Community Node]
    N2 -->|mailbox wake-up| CP

    subgraph Control Plane
        CP --> R[Node Registry]
        CP --> P[Presence Directory / Redis]
        CP --> PU[Push / PostgreSQL / FCM]
    end

    subgraph Community Node
        N1 --> G[Gateway]
        G --> B[Encrypted attachment blobs]
        N1 --> F[Federation]
        N1 --> M[Mailbox / PostgreSQL]
    end
```

The Control Plane is **discovery/control infrastructure**. Community Nodes carry client WebSocket traffic,
federate messages between nodes, and host recipient-selected mailboxes. Caddy is the public HTTP edge for both
packages.

## Fastest way to bring it to life

### 1. Install prerequisites

Windows:

- Android Studio with Android SDK;
- JDK 17 for normal local development;
- Docker Desktop with Docker Compose 2.24.4+;
- Git.

macOS:

- Android Studio with Android SDK;
- JDK 17;
- Docker Desktop;
- Git;
- Xcode only if you want to inspect/build the unfinished iOS host.

### 2. Configure the Control Plane directory

Create or edit the repository-root `local.properties`:

```properties
CONTROL_PLANE_DIRECTORY_URL=https://gist.githubusercontent.com/cbgm/26bb9651e7d2d3fd464df02e8808387f/raw/522436a432e48b9f53f3210b76278e2217f126f8/gistfile1.txt
CONTROL_PLANE_RELEASE_DIRECTORY_URL=https://gist.githubusercontent.com/cbgm/26bb9651e7d2d3fd464df02e8808387f/raw/522436a432e48b9f53f3210b76278e2217f126f8/gistfile1.txt
```

The response may be served as `text/plain` or `application/json`; Sparrow reads the body as text and parses
its JSON content. The document format is:

```json
{
  "controlPlanes": [
    "https://plane-a.example.com",
    "https://plane-b.example.com"
  ]
}
```

The value is compiled into common KMP code as `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL`, so Android and future
future iOS builds use the same common build-time configuration path once the iOS runtime is completed.

### 3. Build the Android app

Windows CMD/PowerShell:

```text
gradlew.bat :androidApp:assembleDebug
```

macOS/Linux:

```bash
./gradlew :androidApp:assembleDebug
```

Or open the project in Android Studio and run `androidApp`.

### 4. Start a Control Plane

**Windows bundle:** generate or download the Control Plane bundle, extract it, then double-click:

```text
Start-SparrowControlPlane.cmd
```

The launcher starts Docker Desktop when necessary, creates runtime secrets, starts PostgreSQL/Redis/services,
and waits for readiness. When it is running, open:

```text
http://<control-plane-host>:8390/index
```

The `/index` page links to registry health, presence health, push health, and connected Community Nodes.

**macOS:** there is currently no macOS Control Plane GUI launcher bundle. For development, run the Control Plane
from source with Docker Compose; see [Local development](docs/development/local-development.md) and
[Control Plane operations](docs/server/control-plane.md).

### 5. Start a Community Node

Windows bundle:

```text
Start-SparrowNode.cmd
```

macOS bundle:

```text
Start-SparrowNode.command
```

or:

```bash
./start-sparrow-node.sh
```

The node asks for LAN/Public mode and the Control Plane directory URL. It can start with cached Control Plane
addresses while all planes are offline, and keeps retrying until a plane becomes reachable.

When running, open:

```text
http://<node-host>:8490/index
```

The node `/index` links to gateway health/connection count, gateway info, advertised Control Planes, federation
health/capabilities, and mailbox health.

### 6. Run the app

Use an Android emulator/device. The app loads the Control Plane directory in `AppViewModel`, verifies signed
node descriptors, selects a compatible node, establishes `/v1/gateway`, and automatically fails over when the
current node becomes unavailable.

## Project structure

```text
androidApp/                Thin Android application entry point and release build configuration
shared/                    Shared Compose app shell, AppViewModel, common DI, BuildKonfig value
startup/                   Startup UI/model
navigation/                Navigation graphs and destinations
core/                      Cross-cutting utilities
core/crypto/               Libsodium crypto implementations
core/embedding/            Shared local text-embedding runtime/model lifecycle
core/protocol/             Transport-independent packets, codec, outbox contracts
core/ui/                   Shared Compose components/theme/navigation primitives
data/database/             Room database, DAOs, entities, durable outbox
data/datastore/            Shared settings/key-value persistence
feature/identity/          Local identity lifecycle and sharing
feature/contacts/          Contacts, invitations, verification, identity exchange
feature/contactimport/     Device/QR contact and identity import flows
feature/chats/             Direct + Group conversation domain/data/UI paths
feature/attachments/       Attachment blob transfer, cache, storage, management and attachment UI models
feature/media/             Gallery/camera/file access, media rendering/viewing/export and file browser
feature/messaging/         Incoming/outgoing orchestration between protocol, crypto and transport
feature/search/            Exact + optional on-device semantic message search
feature/safety/            On-device message-safety analysis and warning/details UI
feature/transport/         Control Plane/node discovery, WebSocket, routing, mailbox/push gateways
feature/onboarding/        Onboarding flows
feature/settings/          User/developer/network settings and developer error log
notification/              Android notification and background work integration
server/                    Control Plane, Community Node and shared server modules
build-logic/               Gradle convention/architecture/quality plugins
quality/detekt-rules/      Project-specific Detekt rules
docs/                      MkDocs engineering documentation
```

Direct and Group chat behavior is intentionally separated. Start with
[Chats architecture](docs/architecture/chats.md) before changing chat behavior.

## Technology stack in plain English

- **Kotlin Multiplatform:** shares domain/application/UI code across platform targets.
- **Compose Multiplatform + Material 3:** declarative UI.
- **Koin:** dependency injection.
- **Room + SQLite:** durable client storage and outbox state.
- **Ktor:** HTTP/WebSocket client and Kotlin server framework.
- **libsodium:** identity keys, sealed-box transport encryption, Ed25519 signatures, and group AEAD.
- **Docker + Docker Compose:** packages and runs the server services and their dependencies.
- **Caddy:** public reverse proxy/edge; exposes one operator-friendly address and routes requests internally.
- **PostgreSQL:** durable server data (registry, mailbox, push, federation queue).
- **Redis:** short-lived presence/routing data that can be rebuilt when clients reconnect.
- **Firebase Cloud Messaging:** Android wake-up notification path for offline delivery.
- **Micrometer/Prometheus:** server metrics.
- **Detekt + ktlint:** static analysis and formatting/quality checks.
- **MediaPipe Text Embedder:** on-device embeddings for optional semantic message search and message-safety analysis.
- **BuildKonfig:** exposes the build-time Control Plane directory value to common KMP code.

Read [Technology stack](docs/technology-stack.md) for a beginner-friendly explanation.

## Build, quality and tests

```bash
./gradlew build
./gradlew qualityCheck
./gradlew allTests
```

Android device tests:

```bash
./gradlew connectedCheck
```

Generated architecture reference:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

Do not manually edit `docs/generated/`.

## Releases

Normal development uses feature branches and PRs into `develop`. `master` is the stable source for a release
line. Create release branches as `release/0.1`, `release/0.2`, and so on.

Every push to `release/**` runs change detection:

- app changes -> debug APK + signed/minified release APK;
- server-service changes -> only affected Docker images plus the corresponding launcher bundle;
- launcher/Caddy/Compose-only changes -> bundle only;
- docs-only changes -> no distributable package;
- the first commit of a release line and `v*` tags -> full build.

A tag such as `v0.1.0-alpha.1` on a commit belonging to a `release/**` branch creates the GitHub release. A full
tagged build contains individual assets plus one combined `sparrow-<version>-full.zip`.

There is **no official tagged release yet**, so this is the configured process rather than a currently published
download.

See [Release process](docs/development/release-process.md) for branch rules, repository variables, signing
secrets, Docker image tags, R8 mapping files, checksums, and the full ZIP layout.

## Documentation map

Start here:

- [Documentation home](docs/index.md)
- [Installation](docs/getting-started/installation.md)
- [First build](docs/getting-started/first-build.md)
- [Using the Android app](docs/getting-started/using-app.md)
- [Local development: Windows + macOS](docs/development/local-development.md)
- [Architecture](docs/architecture/overview.md)
- [How to extend the project](docs/development/extending.md)
- [Detailed messaging flow + UML](docs/features/message-transport-flow.md)
- [Current feature status](docs/features/current-features.md)
- [Attachments](docs/features/attachments.md)
- [Message search](docs/features/search.md)
- [Message safety](docs/features/message-safety.md)
- [Settings and diagnostics](docs/features/settings.md)
- [Security](docs/security/overview.md)
- [Server overview](docs/server/overview.md)
- [Release process](docs/development/release-process.md)
- [FAQ](docs/faq.md)

## License

Apache License 2.0. See the repository license file for the exact terms.
