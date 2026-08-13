<div align="center">

# 🔐 SecureChat

**Modern end-to-end encrypted messaging built with Kotlin Multiplatform**

![CI](https://github.com/cbgm/SecureChat/actions/workflows/ci.yml/badge.svg)
[![Docs](https://img.shields.io/badge/Docs-Live-success?logo=github)](https://cbgm.github.io/SecureChat/)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
![Android](https://img.shields.io/badge/Android-API%2029+-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-Supported-black?logo=apple)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)
![Architecture](https://img.shields.io/badge/Architecture-Clean-success)
![Compose UI](https://img.shields.io/badge/UI-Compose_Multiplatform-blue)
![Detekt](https://img.shields.io/badge/Quality-Detekt-success)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

---

# Overview

SecureChat is a modular **Kotlin Multiplatform** secure messaging application built with **Compose Multiplatform**, **Material 3**, **Koin**, **Room**, **Ktor**, and **LibSodium**.

The project follows a feature-based **Clean Architecture** with centralized Gradle convention plugins, automated quality verification, generated architecture documentation, and custom static analysis rules.

---

# Project Structure

```text
androidApp/          Android application and runtime startup
shared/              Shared Compose application shell
startup/             Startup UI and initialization checks
navigation/          Application navigation
core/                Shared protocol, crypto, UI, and utilities
data/database/       Room database and persistent protocol outbox
feature/chats/       Conversations, messages, receipts, and chat UI
feature/contacts/    Contacts and remote identity exchange
feature/identity/    Local identity and identity-sharing UI
feature/messaging/   Send/receive application orchestration
feature/transport/   Routing IDs, gateway WebSocket, discovery, and wire transport
build-logic/         Convention and architecture plugins
quality/             Custom Detekt rules
docs/                MkDocs engineering handbook
```

---

# Documentation

## Main Documentation

- 📘 [Documentation Index](docs/index.md)
- 🧭 [Architecture Overview](docs/architecture/overview.md)
- 🧩 [Messaging Boundary](docs/architecture/messaging-boundary.md)
- ✉️ [Conversation, Messaging, and Delivery Flow](docs/features/message-transport-flow.md)
- 🪵 [Logging](docs/development/logging.md)
- ✉️ [Push notifications](docs/push-notifications.md)

## Generated Documentation

Generated automatically by the architecture tooling.

- 🏗️ [Architecture Overview](docs/generated/architecture.md)
- 📦 [Module Documentation](docs/generated/modules.md)
- 🔗 [Dependency Matrix](docs/generated/dependency-matrix.md)
- 📊 [Project Statistics](docs/generated/statistics.md)
- 📈 [Mermaid Module Graph](docs/generated/architecture.mmd)
- 🗂️ [Dependency JSON](docs/generated/dependencies.json)
- 🗂️ [Module JSON](docs/generated/modules.json)

---

# Getting Started

Run once after cloning:

```bash
./gradlew setup
```

---

# Build

```bash
./gradlew build
```

---

# Code Quality

Run the repository's local quality workflow:

```bash
./gradlew quality
```

Verification only (CI-safe):

```bash
./gradlew qualityCheck
```

Included checks:

- ktlint
- Detekt
- Custom Detekt Rules
- Architecture Verification

---

# Architecture Documentation

Generate documentation whenever module dependencies change:

```bash
./gradlew architectureReport
```

Verify generated documentation:

```bash
./gradlew verifyArchitectureReport
```

Generated files are written to:

```text
docs/generated/
```

---

# Android

Build:

```bash
./gradlew :androidApp:assembleDebug
```

Run using Android Studio.

---

# iOS (CURRENTLY UNAVAILABLE!)

Open

```text
iosApp/
```

in Xcode and run the application.

---

# Technology Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Material 3
- Kotlin Coroutines
- Kotlin Serialization
- Koin
- Room
- Ktor
- Kermit
- LibSodium
- Gradle Convention Plugins
- Detekt
- Ktlint
- MkDocs

---

# Architecture

SecureChat follows a modular architecture consisting of:

- Feature modules
- Shared core libraries
- Convention plugins
- Automated dependency verification
- Generated architecture documentation
- Custom Detekt rules
- Feature-based Clean Architecture

The generated architecture documentation is considered the source of truth for the project's dependency graph.

The hand-written architecture pages explain intent and runtime behavior. If they disagree with
`docs/generated/`, first verify the current Gradle configuration and then update the hand-written
page. Never edit generated files manually.

---

# Federated Server Implementation

The independently deployable registry, presence, gateway, federation, mailbox, and push services are
available under [`server/`](server/README.md). The obsolete standalone relay module has been removed.
A complete local network can be started with:

```bash
docker compose -f server/docker-compose.yml up --build
```

The app automatically signs and refreshes its presence route after connecting to the federated
gateway, enabling the federation service to locate active clients across nodes. Device routing IDs
are derived from random signing identities and do not contain or hash phone numbers.

The Android client fetches the signed node directory from the registry, verifies the pinned or
trust-on-first-use registry authority and every node descriptor, caches the last valid directory,
and rotates to another compatible gateway when its current node fails. Push continues to use its
separate HTTP base URL and is not coupled to the selected WebSocket node.

For every mutually authenticated contact, the recipient now provisions a separate expiring mailbox
capability, signs the route with its identity key, and exchanges it inside the encrypted protocol.
Senders attach the latest verified route to federated envelopes. Offline ciphertext is retained by
the selected mailbox, while FCM carries only a wake-up identifier; the receiver retrieves, processes,
and acknowledges the mailbox envelope after waking. The legacy push inbox remains as a compatibility
fallback until both contacts have exchanged mailbox routes.

Per-contact mailbox capabilities now rotate automatically before expiry and are revoked when a
contact is blocked, a direct chat is deleted, or either identity changes. Offline revocation attempts
are persisted and retried after reconnect, while obsolete remote delivery routes are removed
immediately.

Public server write endpoints now apply bounded per-client rate limits. Mailbox provisioning also
uses atomic global and per-client quotas, with only a hash of the client address stored in
PostgreSQL. Production trusts forwarded client addresses only behind the Caddy-only public edge.

All six Kotlin services now share Prometheus-compatible metrics, liveness and storage-aware
readiness endpoints, and validated `X-Request-ID` correlation in HTTP responses and structured
console logs. Docker Compose uses the readiness contract to order application startup.

Node operators can create checksummed, timestamped backups of every PostgreSQL database and signing
identity, validate them without downtime, and restore them into fresh Docker volumes with an
explicit disaster-recovery command. Short-lived Redis presence routes are intentionally rebuilt by
reconnecting clients instead of being restored.

A non-destructive PowerShell smoke test now validates the single-node or two-node Compose topology,
all storage-aware health checks, expected restored counts, request-ID propagation, and Prometheus
metrics. Restore failures include the unhealthy service's probe output and recent logs. Pull requests
that change the server now run the fresh two-node smoke test automatically and retain Compose state
and service logs as a failure artifact.

The control plane and community-node data plane can also be deployed as completely separate Compose
projects. Community nodes use their persistent Ed25519 node identity for registry, presence, push,
and federation requests; operators never receive a shared control-plane token. A second smoke test
starts two community nodes plus the control plane, verifies two stable registrations, sends an
encrypted envelope in both directions, and asserts that no project shares a Docker network or volume
with another.

See the server README for module boundaries, ports, security behavior, and migration limitations.

---

# License

Licensed under the Apache 2.0 License.
