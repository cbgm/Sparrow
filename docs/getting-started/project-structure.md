# Project Structure

SecureChat uses Gradle modules as architecture boundaries and packages as internal layers. The
generated [module catalog](../generated/modules.md) is the source of truth for the current module
list and dependencies.

## Repository layout

```text
SecureChat/
├── androidApp/              # Android entry point and runtime service startup
├── shared/                  # shared Compose application shell
├── startup/                 # startup initialization and UI
├── navigation/              # cross-feature routes and graph
├── core/
│   ├── crypto/              # transport/identity crypto and codecs
│   ├── protocol/            # packets and transport-independent ports
│   └── ui/                  # reusable Compose UI
├── data/
│   └── database/            # Room entities, DAOs, and persistent outbox
├── feature/
│   ├── chats/
│   ├── contactimport/
│   ├── contacts/
│   ├── identity/
│   ├── messaging/
│   ├── onboarding/
│   ├── settings/
│   └── transport/
├── build-logic/             # included Gradle build and convention plugins
├── quality/
│   └── detekt-rules/        # custom static-analysis rules
├── config/                  # tool configuration
├── docs/                    # MkDocs handbook and generated reference
├── gradle/                  # wrapper and version catalog
└── .github/                 # CI and documentation publishing
```

The grouping projects `:core`, `:data`, and `:feature` may appear in generated architecture output.
Behavior belongs in their child modules or appropriate root sources, not in a grouping module by
default.

## Application composition modules

### `:androidApp`

Contains Android-specific entry points: `SecureChatApplication`, the Activity, manifest, and Koin
assembly. It starts transport runtime services after local identity setup. Business logic remains in
feature or core modules.

### `:shared`

Contains the shared Compose application shell and common app-level UI composition.

### `:startup`

Contains `AppInitializer`, startup result/state, `StartupViewModel`, `StartupRoute`,
`StartupScreen`, and screen components. It is a standalone module, not a package inside identity.

### `:navigation`

Contains routes and the navigation graph. It coordinates feature entry points without implementing
feature business rules.

## Core modules

### `:core`

Small reusable primitives such as ID generation and time.

### `:core:crypto`

Key operations, identity acknowledgement crypto, safety numbers, transport encryption/decryption,
`EncryptedTransportPayload`, and `TransportPayloadCodec`.

### `:core:protocol`

`SecureChatPacket` types, `PacketCodec`, typed incoming-handler contracts, persistent-outbox
contracts/state machine, identity provider ports, phone-number ports, and `OutgoingWireSender`.

It must remain independent of Ktor, Room, Compose, and feature repositories.

### `:core:ui`

Reusable Compose components, theming, and design-system utilities.

## Data

### `:data:database`

Owns Room entities, DAOs, database initialization, and `DefaultProtocolOutbox`. Domain interfaces
usually live in the feature that owns the behavior; this module supplies shared persistence
infrastructure.

## Feature modules

### `:feature:chats`

Conversations, direct/group messages, visible delivery state, receipts, typed chat packet handlers,
and chat UI. See [Chats](../features/chats.md).

### `:feature:contacts`

Contacts, phone numbers, contact merge, remote identity exchange, verification, and reusable
contacts UI. See [Contacts](../features/contacts.md).

### `:feature:identity`

Local identity lifecycle, identity storage ports/adapters, identity sharing, and setup/share UI.
See [Identity](../features/identity.md).

### `:feature:messaging`

UI-less application orchestration:

```text
application/   incoming, mailbox, outbox, and routing workflows
data/          repository and routing adapters
di/            Koin wiring
```

It connects chats, contacts, crypto, protocol, database, and transport. See
[Messaging Boundary](../architecture/messaging-boundary.md).

### `:feature:transport`

Routing IDs, node discovery, connection lifecycle, gateway WebSocket frames, outgoing wire adapter, and platform HTTP clients.
It moves opaque payloads. See [Transport](../features/transport.md).

### Other features

| Module | Responsibility |
|---|---|
| `:feature:contactimport` | Platform address-book implementations |
| `:feature:onboarding` | Onboarding flow |
| `:feature:settings` | Settings behavior and presentation |

## Server applications

The independently deployable server applications live under `server/`. Client-facing WebSocket routing belongs to `:server:gateway`; cross-node routing belongs to `:server:federation`; offline ciphertext belongs to `:server:mailbox`; push wake-ups and the pending inbox belong to `:server:push`.

## Standard feature layout

A user-visible feature commonly uses:

```text
feature/<name>/.../feature/<name>/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── mapper/
│   └── repository/
├── presentation/
│   ├── model/
│   ├── screen/
│   └── component/
└── di/
```

Use only packages needed by the feature. Long-running, UI-less orchestration may add
`application/`. Infrastructure features such as transport need not invent presentation or use-case
packages.

Screen-specific components should live under their screen package. Components shared by multiple
screens belong under `presentation/component`.

## Source sets

Kotlin Multiplatform modules commonly contain:

```text
src/
├── commonMain/
├── commonTest/
├── androidMain/
├── androidDeviceTest/
├── iosMain/
└── platform-specific tests
```

Not every module contains every source set. Platform APIs belong in platform source sets behind
common interfaces.

## Build and documentation infrastructure

`build-logic` supplies convention plugins and architecture-report tasks. `quality/detekt-rules`
contains project-specific Detekt checks. `docs/generated/` is produced from Gradle and must not be
edited manually.

After module or dependency changes:

```bash
./gradlew architectureReport
./gradlew verifyArchitectureReport
```

## Placement checklist

Before adding a class:

1. Which module owns the business decision?
2. Is the class a domain rule, application workflow, data adapter, or presentation element?
3. Can the dependency point toward a stable port instead of another implementation?
4. Is the code platform-independent enough for `commonMain`?
5. Does an existing shared component or core contract already cover it?
6. Will the new dependency appear correctly in the generated architecture report?
