# Project structure

The current `settings.gradle.kts` declares **47 Gradle modules**.

## Client/shared modules

| Module | Responsibility |
|---|---|
| `:androidApp` | Android host, manifest, packaging and app version resolution |
| `:shared` | app shell, shared DI and application runtime/startup tasks |
| `:startup` | startup contract/presentation gate |
| `:navigation` | application navigation graphs |
| `:resources` | shared Compose resources |
| `:core:base` | shared base/domain primitives including the message-part hierarchy |
| `:core:util` | cross-cutting utilities |
| `:core:crypto` | crypto implementations/providers |
| `:core:ui` | shared Compose/navigation primitives |
| `:protocol` | client wire packets, codecs, operation messages and protocol contracts |
| `:data:database` | Room schema v54, DAOs/entities/migrations |
| `:data:datastore` | key/value/settings persistence |
| `:feature:embedding` | local embedding runtime/model lifecycle |
| `:feature:identity` | identity lifecycle/exchange/backup/recovery persistence |
| `:feature:applock` | device-owner app lock |
| `:feature:invite` | Direct/Group invitations and inbox |
| `:feature:contacts` | contacts, blocking, routing/peer projection |
| `:feature:contactimport` | device/shared contact import |
| `:feature:autoreply` | automatic reply rules |
| `:feature:avatar` | avatar observation/editing/cache |
| `:feature:linkpreview` | link extraction/cache/rendering |
| `:feature:chats` | Direct/Group messages, operations, receipts, pins and chat UI |
| `:feature:conversationorchestration` | cross-feature identity/invite/membership/chat workflows |
| `:feature:membership` | group membership/security/admin lifecycle |
| `:feature:attachments` | encrypted blob transfer/cache/storage and attachment UI state |
| `:feature:media` | gallery/camera/files/media viewers/export |
| `:feature:voice` | recording/playback/local transcription |
| `:feature:polls` | poll creation and poll-specific presentation |
| `:feature:messaging` | durable outbox/incoming-envelope execution |
| `:feature:transport` | discovery/WebSocket/routing/mailbox/push gateways |
| `:feature:onboarding` | onboarding flows |
| `:feature:notification` | notification/background integration |
| `:feature:settings` | user/developer/network/AI/app-lock settings |
| `:feature:search` | exact + semantic message search |
| `:feature:safety` | local message-safety analysis |
| `:quality:detekt-rules` | project-specific Detekt rules |

## Server modules

The server family remains `:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`, `:server:node-registry`, `:server:presence-directory`, `:server:gateway`, `:server:federation`, `:server:mailbox`, `:server:push` and `:server:link-preview`.

Deployment folders under `server/control-plane`, `server/community-node`, `server/unified` and `server/control-plane-directory` are operator/runtime packaging rather than additional Gradle modules.

## Feature layering

Typical feature layout is:

```text
presentation/
domain/model/
domain/repository/
domain/usecase/
data/datasource/
data/model/
data/repository/
data/mapper/
device/
di/
```

Cross-feature shared models are placed in an explicit shared module rather than duplicated inside features. The current message-part hierarchy is the main example: `MessagePartDto` / `MessagePart` / `MessagePartUi` live under `:core:base`.

See the generated [Module catalog](../generated/modules.md) for the source-derived dependency/declaration inventory.
