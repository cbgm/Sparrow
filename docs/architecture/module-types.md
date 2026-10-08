# Module types

The current Gradle graph contains 47 modules.

## Application shell

- `:androidApp` — Android packaging/host/version/signing.
- `:shared` — app shell, shared DI and application runtime/startup tasks.
- `:startup` — startup contract/presentation gate.
- `:navigation` — typed navigation graphs.
- `:resources` — shared Compose resources.

## Core/shared

- `:core:base` — shared base/domain primitives including `MessagePartDto` / `MessagePart` / `MessagePartUi`.
- `:core:util` — cross-cutting utilities.
- `:core:crypto` — crypto implementations/providers.
- `:core:ui` — reusable UI/theme/navigation primitives.
- `:protocol` — application packets, codecs and operation-message contracts.

## Data

- `:data:database` — Room schema v54, DAOs/entities/migrations/outbox persistence.
- `:data:datastore` — settings/key-value persistence.

## Feature modules

- `:feature:embedding` — local embedding model/runtime.
- `:feature:identity` — identity lifecycle/trust/backup/recovery.
- `:feature:applock` — device-owner app lock.
- `:feature:invite` — Direct/Group invitation lifecycle/inbox.
- `:feature:contacts` — contacts/blocking/routing projection.
- `:feature:contactimport` — device/shared contact import.
- `:feature:autoreply` — auto-reply rules/claims.
- `:feature:avatar` — avatar observation/editing/cache.
- `:feature:linkpreview` — link-preview client cache/rendering.
- `:feature:chats` — Direct/Group conversations, message operations and pins.
- `:feature:conversationorchestration` — explicit cross-feature workflow boundary.
- `:feature:membership` — group membership/security/admin lifecycle.
- `:feature:attachments` — encrypted attachment/blob transfer/cache/storage.
- `:feature:media` — gallery/camera/file selection/viewers/export.
- `:feature:voice` — voice record/playback/transcription.
- `:feature:polls` — poll creation and presentation.
- `:feature:messaging` — durable outbox/incoming-envelope execution.
- `:feature:transport` — discovery/WebSocket/presence/mailbox/push transport.
- `:feature:onboarding` — onboarding.
- `:feature:notification` — notification/background runtime.
- `:feature:settings` — user/developer/network/AI/app-lock settings.
- `:feature:search` — exact/semantic search.
- `:feature:safety` — local message-safety analysis.

## Server modules

`:server:protocol`, `:server:security`, `:server:persistence`, `:server:observability`, `:server:node-registry`, `:server:presence-directory`, `:server:gateway`, `:server:federation`, `:server:mailbox`, `:server:push`, `:server:link-preview`.

## Quality

- `:quality:detekt-rules` — project-specific Detekt checks.
- `build-logic/` — Gradle convention plugins.
