# Project structure

Sparrow is split by ownership. The current Gradle settings declare **44 modules**.

## Client/application modules

| Module | Responsibility |
|---|---|
| `:androidApp` | Thin Android host, manifest and Android packaging |
| `:shared` | Shared application shell, `AppViewModel`, common composition |
| `:startup` | Startup presentation/state |
| `:navigation` | Routes, nav graphs, recovery inbox integration |
| `:notification` | Visibility, Android push/notification runtime |
| `:resources` | Shared Compose resources |
| `:core` | logging/time/IDs/common contracts |
| `:core:crypto` | cryptographic implementations/providers |
| `:core:embedding` | local embedding model/runtime |
| `:core:protocol` | packets/codecs/outbox/transport-independent protocol contracts |
| `:core:ui` | reusable UI/navigation primitives |
| `:data:database` | Room schema v53, DAOs/entities/migrations |
| `:data:datastore` | key/value/settings persistence |
| `:feature:identity` | identity, exchange/trust, backup/restore, identity-change/reconnection persistence |
| `:feature:invite` | generic Direct/Group invitation lifecycle + inbox UI |
| `:feature:contacts` | contacts, blocking, peer metadata/identity mappings |
| `:feature:contactimport` | device contact + shared identity import |
| `:feature:autoreply` | auto reply configuration/claiming |
| `:feature:avatar` | avatar loading/cache/presentation |
| `:feature:linkpreview` | preview fetch/cache/rendering |
| `:feature:chats` | Direct/Group conversations/messages/receipts/pins |
| `:feature:conversationorchestration` | cross-feature conversation/invite/identity/membership workflows |
| `:feature:membership` | Group membership/security/roles/welcome/activation/admin lifecycle |
| `:feature:attachments` | attachment transfer/cache/storage/transcripts |
| `:feature:media` | gallery/camera/file/media rendering/export |
| `:feature:voice` | voice record/playback/local transcription |
| `:feature:messaging` | generic durable outbox/incoming-envelope runners |
| `:feature:transport` | Control Plane/node discovery, WebSocket/presence/mailbox/push transport |
| `:feature:onboarding` | onboarding |
| `:feature:settings` | user/developer/network/AI settings |
| `:feature:search` | exact + semantic message search |
| `:feature:safety` | local message-safety analysis |

## Server modules

| Module | Responsibility |
|---|---|
| `:server:protocol` | server wire models |
| `:server:security` | node signatures/auth/rate limits/replay protection |
| `:server:persistence` | shared server persistence/environment helpers |
| `:server:observability` | readiness/health/metrics/request IDs |
| `:server:node-registry` | signed healthy Community Node directory |
| `:server:presence-directory` | ephemeral routing presence |
| `:server:gateway` | client WebSocket gateway + encrypted blob store |
| `:server:federation` | cross-node routing + durable retry |
| `:server:mailbox` | recipient capability-based offline mailbox |
| `:server:push` | push registrations/wakeups/pending fallback |
| `:server:link-preview` | metadata/image preview proxy service |

The deployment templates (`server/control-plane`, `server/community-node`, `server/unified`) are runtime packaging directories rather than Gradle modules.

## Feature layering

Typical structure:

```text
presentation/
domain/model/
domain/repository/
domain/usecase/
data/datasource/
data/model/
data/repository/
data/mapper/
device/          # in platform source sets where required
di/
```

Use cases may compose other use cases when they are the explicit workflow layer. Repositories/datasources still must not create hidden cross-feature dependency chains.

For a class-level inventory of the current source tree, see [Current code inventory](../generated/current-code-inventory.md).
