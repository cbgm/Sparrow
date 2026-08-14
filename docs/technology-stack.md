# Technology stack

This page explains the technologies a new contributor/operator will encounter. It focuses on what each tool does in this project rather than assuming prior Android/server knowledge.

## Client and build

| Technology | What it is | How Sparrow uses it |
|---|---|---|
| Kotlin 2.4 | Programming language | Client, shared code, Gradle build logic and JVM servers |
| Kotlin Multiplatform | Shared-code platform | `commonMain` business/application/UI code with Android/iOS source sets |
| Compose Multiplatform + Material 3 | Declarative UI | Shared screens/components/theme |
| Coroutines / Flow / StateFlow | Async/reactive primitives | ViewModel state, runners, reconnect/retry loops and server work |
| Koin | Dependency injection | Connects use cases/contracts/implementations and ViewModels |
| Room + bundled SQLite | Client database | Contacts, conversations, messages, recipient state and persistent outbox |
| Kotlin Serialization | Serialization | Client application packets and JSON/server models |
| Ktor client | HTTP/WebSocket client | Control Plane directory/health, gateway WebSocket, mailbox/push calls |
| libsodium bindings | Cryptography | Identity key pairs, sealed boxes, signatures and group AEAD |
| Android Keystore | Hardware/OS-backed key store | Holds the AES wrapping key used by `AndroidPrivateKeyStorage` |
| WorkManager | Android background scheduling | Push-token registration and pending-message synchronization workers |
| Firebase Messaging | Android push client | Receives wake-up data messages |
| CameraX + ZXing | Camera/QR tools | Identity QR scanning/verification |
| Navigation Compose | Typed Compose navigation | App destinations/graphs |
| Kermit | Shared logging facade | Client-side structured/tagged logging |
| BuildKonfig | KMP build constants | Exposes `controlPlaneDirectoryUrl` as common `BuildKonfig.CONTROL_PLANE_DIRECTORY_URL` |
| Gradle + convention plugins | Build system | Modules, quality rules, architecture reports, packaging and tests |
| Detekt + ktlint + Compose rules | Static analysis/style | Fails CI on project/style/architecture-quality violations |
| R8/resource shrinker | Android optimizer | Minifies and shrinks signed release APKs; mapping retained privately in CI |
| AboutLibraries | Dependency/license UI | Generates/renders dependency license information |

## Server and operations

| Technology | What it is | How Sparrow uses it |
|---|---|---|
| Ktor server + Netty | Kotlin JVM HTTP/WebSocket server | Registry, presence, gateway, federation, mailbox and push applications |
| Docker | Container runtime | One image/runtime per server service/dependency |
| Docker Compose | Multi-container deployment definition | Starts complete Control Plane or Community Node topologies |
| Caddy | Reverse proxy/web edge | Public HTTP(S)/WSS entry, compression/security headers, path routing and `/index` |
| PostgreSQL | Durable relational database | Registry, push, federation retry queue and mailbox state |
| HikariCP | JDBC connection pool | Efficient PostgreSQL connections in JVM services |
| Redis + Jedis | Fast key/value store/client | Short-lived presence/routing data |
| Firebase Admin SDK | Server push integration | Sends Android FCM wake-ups from the Control Plane push service |
| Micrometer + Prometheus registry | Metrics instrumentation | Operational server metrics |
| SLF4J/Logback | JVM logging | Server logs |
| GitHub Actions | CI/release automation | PR validation, release candidates, signed APKs, image/package publishing |
| GHCR | GitHub Container Registry | Versioned server Docker images used by launcher bundles |
| MkDocs Material + Mermaid | Documentation site/diagrams | Hand-written engineering docs and rendered UML/flow diagrams |

## Docker in plain English

A **Docker image** is a packaged filesystem/runtime for one service. A **container** is a running instance of that image.

Sparrow keeps registry, presence, push, gateway, federation and mailbox as separate images. PostgreSQL, Redis and Caddy use standard upstream images. This makes services independently rebuildable and prevents the deployment from becoming one giant JVM process.

## Docker Compose in plain English

Compose describes which containers belong together, their environment variables, ports, networks, volumes and startup/health dependencies.

Sparrow has two production-shaped units:

- **Control Plane:** Caddy + node-registry/PostgreSQL + presence-directory/Redis + push/PostgreSQL.
- **Community Node:** Caddy + gateway + federation/PostgreSQL + mailbox/PostgreSQL + persistent node identity.

A launcher is mostly an operator-friendly way to prepare configuration/secrets and run those Compose files consistently.

## Caddy in plain English

Without Caddy an operator/client would need to know individual internal service ports. Caddy provides one public edge and routes by URL path.

For example, a Community Node receives `/v1/gateway` and proxies it to the internal `gateway:8094` container. The internal hostname is a Docker-network detail; clients never need to know it.

Caddy configuration also enables zstd/gzip compression, removes the `Server` header, sets HSTS/content/referrer headers and serves `/index`.

## PostgreSQL vs Redis

Use **PostgreSQL** when state must survive restart and be queryable/durable:

- registered nodes/signing-related registry state;
- push device/pending/wake-up rows;
- federation outbound retry queue;
- mailboxes/envelopes/capability state.

Use **Redis** for presence because routes are intentionally short-lived. If presence Redis state disappears, clients reconnect and republish signed routes. Reconstructing live presence is safer than restoring stale connection routes.

## Ktor on both sides

The client and server both use Ktor, but that does not merge their responsibilities. The client Ktor code handles HTTP/WebSocket transport. Each server application is a separate Ktor service. They share explicit protocol modules/models only where the wire contract is actually common.

## Cryptography

`core/crypto` wraps libsodium behind project-owned interfaces. Important implementations:

- `SodiumIdentityKeyGenerator`
- `SodiumTransportMessageCipher`
- `SodiumDetachedSignatureCrypto`
- `SodiumGroupCrypto`
- `SafetyNumberGenerator`

See [Security overview](security/overview.md) for algorithms, key storage and trust boundaries.

## GitHub Actions and releases

Normal feature PR validation targets `develop`. Release branches (`release/**`) use change detection so app-only changes do not rebuild all server images and server-only changes do not require Android signing. A `v*` tag forces the complete reproducible release set.

See [Release process](development/release-process.md).
