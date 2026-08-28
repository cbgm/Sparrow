# Project structure

Sparrow is modular. The easiest way to understand a change is to first find which module owns the behavior.

## Client modules

| Module | Responsibility |
|---|---|
| `:androidApp` | Thin Android application host, manifest, release signing/minification |
| `:shared` | Shared Compose root, `AppViewModel`, common DI, BuildKonfig |
| `:startup` | Startup state/UI |
| `:navigation` | App routes and navigation graphs |
| `:core` | IDs, time, logging, small common contracts |
| `:core:crypto` | Libsodium implementations and crypto abstractions |
| `:core:embedding` | Shared local text-embedding model/runtime used by search and safety |
| `:core:protocol` | Packets, codecs, outbox/handler/transport contracts |
| `:core:ui` | Reusable Compose design/navigation utilities |
| `:data:database` | Room database, entities, DAOs, durable outbox |
| `:data:datastore` | Shared key/value/settings persistence |
| `:feature:identity` | Local identity creation/storage/sharing |
| `:feature:contacts` | Contacts, invitations, identity exchange, verification/blocking |
| `:feature:contactimport` | Platform contact/QR import integration |
| `:feature:chats` | Direct + Group conversation data/domain/presentation and typed message parts |
| `:feature:attachments` | Attachment blob transfer, cache, saved-copy storage/management and attachment source models |
| `:feature:media` | Gallery/camera/file access, file browser, media rendering/opening/export |
| `:feature:messaging` | Cross-feature incoming/outgoing/mailbox orchestration |
| `:feature:search` | Exact + optional local semantic message search |
| `:feature:safety` | Local message-safety analysis and warning/details presentation |
| `:feature:transport` | Control Plane discovery, node selection, WebSocket, presence, mailbox/push gateways |
| `:feature:onboarding` | Onboarding |
| `:feature:settings` | User/developer/Control Plane settings, AI feature toggles and developer error log |
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
presentation/      Screens, routes, ViewModels, *Ui models/mappers/components
domain/model/      Unsuffixed domain models and state machines
domain/repository/ Repository contracts
domain/usecase/    One-purpose use cases
data/datasource/   IO/storage/network-facing data access
data/model/        *Dto data representations
data/repository/   Repository implementations
data/mapper/       Mapping functions named for destination types
device/            Platform contracts/implementations in the matching source set
di/                Koin wiring
```

The core mapping convention is:

```text
NameDto -> Name -> NameUi
toNameDto() / toName() / toNameUi()
```

Room persistence models are `NameEntity`, not DTOs. ViewModels call use cases; datasources/repositories/use cases obey the dependency rules in [Clean architecture](../architecture/clean-architecture.md).

## Chats message representation

Direct and Group message content use typed parts rather than exposing the attachment feature's source model:

```text
MessagePartDto  (chats data)
MessagePart     (chats domain)
MessagePartUi   (chats presentation)
```

The current part variants cover text, image/video, file, location and contact. Attachment blob transfer/storage still belongs to `:feature:attachments`.

## Direct vs Group

`feature/chats` deliberately contains separate Direct and Group trees for conversation-specific semantics. Shared code is limited to real shared boundaries such as message-part mapping, packet decoding/routing, overview aggregation and common wire infrastructure.

## Platform source sets

KMP modules can contain `commonMain`, `androidMain`, `iosMain` and matching test source sets. Platform implementation belongs in the appropriate platform source set of the owning module, normally under the top-level `device` responsibility. The project currently has several iOS implementations, but not enough to make the iOS app usable.

## Generated module reference

For exact current Gradle dependencies, use [Generated architecture](../generated/architecture.md). Regenerate it after dependency/module changes rather than editing generated Markdown manually.
