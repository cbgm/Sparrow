# Module types

The current `settings.gradle.kts` declares 57 modules.

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

- `:data:database` — Room schema v55, DAOs/entities/migrations/outbox persistence.
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
- `:feature:chats` — Direct/Group conversation presentation and read-only data; submits writes as commands.
- `:feature:conversationorchestration:api` — public `ConversationOrchestrator`, commands, states and results.
- `:feature:conversationorchestration:engine` — workflow state-machine runtime, locks and command routing.
- `:feature:conversationorchestration:delivery` — delivery state, outgoing transport policy, mailbox and routing.
- `:feature:conversationorchestration:message` — Direct/Group message, operation, poll and forwarding workflows.
- `:feature:conversationorchestration:groupstate` — admin-owned group state: title, description, avatar, pin and expense board workflows.
- `:feature:conversationorchestration:membership` — group administration, group join and verification workflows.
- `:feature:conversationorchestration:lifecycle` — direct chat lifecycle workflows (`direct/`, `result/`, `recovery/`) plus result observers.
- `:feature:conversationorchestration:incoming` — incoming packet routing and `Receive*` workflows.
- `:feature:membership` — group membership/security/admin lifecycle.
- `:feature:attachments` — encrypted attachment/blob transfer/cache/storage.
- `:feature:media` — gallery/camera/file selection/viewers/export.
- `:feature:ocr` — on-device text recognition and receipt parsing for expense scanning.
- `:feature:voice` — voice record/playback/transcription.
- `:feature:polls` — poll creation and presentation.
- `:feature:expenses` — group expense creation, splits, balances, settlements and exchange rates.
- `:feature:payment` — settle-up payments: stored bank details and SEPA payment QR codes.
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
