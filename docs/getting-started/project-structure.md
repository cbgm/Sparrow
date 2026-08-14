# Project structure

SecureChat is modular. The easiest way to understand a change is to first find which module owns the behavior.

## Client modules

| Module | Responsibility |
|---|---|
| `:androidApp` | Thin Android application host, manifest, release signing/minification |
| `:shared` | Shared Compose root, `AppViewModel`, common DI, BuildKonfig |
| `:startup` | Startup state/UI |
| `:navigation` | App routes and navigation graphs |
| `:core` | IDs, time, logging, small common contracts |
| `:core:crypto` | Libsodium implementations and crypto abstractions |
| `:core:protocol` | Packets, codecs, outbox/handler/transport contracts |
| `:core:ui` | Reusable Compose design/navigation utilities |
| `:data:database` | Room database, entities, DAOs, durable outbox |
| `:feature:identity` | Local identity creation/storage/sharing |
| `:feature:contacts` | Contacts, invitations, identity exchange, verification/blocking |
| `:feature:contactimport` | Platform contact/QR import integration |
| `:feature:chats` | Direct + Group conversation domain/data/UI |
| `:feature:messaging` | Cross-feature incoming/outgoing/mailbox orchestration |
| `:feature:transport` | Control Plane discovery, node selection, WebSocket, presence, mailbox/push gateways |
| `:feature:onboarding` | Onboarding |
| `:feature:settings` | Settings and Control Plane management UI |
| `:notification` | App visibility, Android notification/push workers |
| `:resources` | Shared resources |

## Server modules

| Module | Responsibility |
|---|---|
| `:server:node-registry` | Signed healthy-node directory |
| `:server:presence-directory` | Temporary routing ID -> node/connection routes |
| `:server:push` | Android device registrations and wake-up/pending fallback storage |
| `:server:gateway` | Client WebSockets and local node ingress/egress |
| `:server:federation` | Cross-node routing, retry and durable outbound queue |
| `:server:mailbox` | Recipient-selected durable offline encrypted envelopes |
| `:server:protocol` | Server wire models |
| `:server:security` | Node signatures, replay protection, routing IDs, rate limits |
| `:server:persistence` | Shared persistence helpers |
| `:server:observability` | Health/readiness/metrics/request IDs |

## Feature-internal Clean Architecture

Typical feature packages:

```text
presentation/     Screens, routes, ViewModels, UI models/mappers/components
domain/model/     Domain models and state machines
domain/repository Repository contracts
domain/usecase/   One-purpose use cases
data/repository/  Repository implementations
data/...          Mappers, storage, packet processing, protocol/security helpers
di/               Koin wiring
```

ViewModels call use cases, not DAOs or transport implementations.

## Direct vs Group

`feature/chats` deliberately contains separate trees:

```text
data/direct/...
data/group/...
domain/model/direct/...
domain/model/group/...
domain/repository/direct/...
domain/repository/group/...
domain/usecase/direct/...
domain/usecase/group/...
presentation/direct/...
presentation/group/...
```

Shared code is limited to real shared boundaries such as packet decoding/routing, overview aggregation and common
wire infrastructure.

## Platform source sets

KMP modules can contain:

- `commonMain`: shared implementation;
- `androidMain`: Android-only implementations;
- `iosMain`: iOS-only implementations;
- matching test source sets.

The project currently has several iOS implementations, but there are not enough to make the iOS app usable.

## Generated module reference

For exact current Gradle dependencies, use [Generated architecture](../generated/architecture.md). Regenerate it
after dependency/module changes rather than editing generated Markdown manually.
