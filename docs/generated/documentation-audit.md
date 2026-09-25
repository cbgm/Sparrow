# Current-code documentation audit

This page records the source-driven documentation audit performed against the 2026-09-25 repository snapshot.

## Scope and method

The audit starts from `settings.gradle.kts`, every current module `build.gradle.kts`, production Kotlin source sets, Room schema/migrations, server Compose files and server runtime/build scripts. Existing documentation was then checked for missing or stale ownership/flow descriptions.

This is intentionally different from treating the old documentation table of contents as the feature list.

## Current project footprint

- **44 Gradle modules** are included by `settings.gradle.kts`.
- The source-derived inventory contains **1,788 production top-level Kotlin declarations** (tests excluded; declarations are regex-indexed and therefore intended as a navigation inventory rather than a compiler symbol table).
- Client Room schema: **53**.
- Current public server packager: `server/unified/Build-SparrowServer.cmd` -> `dist/sparrow-server.zip`.
- `server/control-plane-directory` is a separate operator-only Python service and is excluded from that public bundle.

See [Current code inventory](current-code-inventory.md) for module-by-module files/declarations and [Module catalog](modules.md) for dependencies.

## Material current-code areas explicitly documented

| Area found in current source | Implementation anchors | Detailed documentation |
|---|---|---|
| Cross-feature conversation workflow | `ConversationFlowHandler`, result observers, `OutgoingPacketSender` | [Runtime flows](../architecture/runtime-flows.md) |
| Durable protocol execution | `DefaultOutboxRunner`, `DefaultOutboxProcessor`, `DefaultIncomingEnvelopeRunner`, `DefaultIncomingEnvelopeProcessor` | [Runtime flows](../architecture/runtime-flows.md), [Messaging boundary](../architecture/messaging-boundary.md) |
| Generic invitation lifecycle | `InvitationRepository`, `InvitationResultObserver`, `InvitationViewModel` | [Invitations](../features/invitations.md) |
| Identity backup/restore | `PrepareIdentityBackupUseCase`, `RestoreIdentityBackupUseCase`, `AndroidIdentityBackupCodec` | [Identity recovery](../features/identity-recovery.md) |
| Changed remote identity/reconnection | `PendingRemoteIdentityChange`, `ApprovedIdentityReconnection`, `ApprovedIdentityReconnectionObserver`, `ApprovedReconnectionRetryWorker`, `StartRecoveryInvitationUseCase` | [Identity recovery](../features/identity-recovery.md) |
| Group membership/security | `GroupMembershipStateMachine`, `GroupSecurityManager`, welcome/activation/admin datasources/use cases | [Group membership](../features/group-membership.md) |
| Direct/Group message flow | `DirectOutgoingMessageProcessor`, `GroupOutgoingMessageProcessor`, incoming packet routers | [Message/delivery flow](../features/message-transport-flow.md), [Runtime flows](../architecture/runtime-flows.md) |
| Persistence/migrations | `SparrowDatabase`, schema 53 migrations | [Persistence](../architecture/persistence.md) |
| Voice/transcription | `VoiceComposerViewModel`, `VoiceMessageViewModel`, `AndroidVoiceTranscriptionRepository`, `AndroidWhisperModelStore`, `WhisperNative` | [Voice](../features/voice.md) |
| Link previews | client `LinkPreviewRepositoryImpl`; server `LinkPreviewService`, `LinkPreviewFetcher`, parser/validator | [Link previews](../features/link-previews.md) |
| Auto reply | `AutoReplyRepositoryImpl`, claim/release/activate use cases | [Auto reply](../features/auto-reply.md) |
| Group pin | `GroupPinRepositoryImpl`, `GroupPinBroadcaster`, pin/unpin use cases | [Pinned messages](../features/pinned-messages.md) |
| Avatar/editor | `AvatarRepositoryImpl`, `AvatarViewModel`, `AvatarEditorViewModel`, `ImagePicker`, crop expect/actual function | [Avatars](../features/avatar.md) |
| Transport/discovery | node directory/selection/WebSocket/mailbox/push implementations | [Transport](../features/transport.md) |
| Control Plane runtime | registry, presence, push + Compose | [Control Plane](../server/control-plane.md) |
| Community Node runtime | gateway, federation, mailbox, encrypted blobs + Compose | [Community Node](../server/community-node.md) |
| Unified server build/update | `New-SparrowServerBundle.ps1`, runtime/node managers, public proxy helpers | [Runtime/build/deployment](../server/runtime-build-deployment.md) |
| Independent signed Control Plane Directory | `directory_service.py`, registration/admin/install tooling | [Runtime/build/deployment](../server/runtime-build-deployment.md#independent-control-plane-directory-operator-only) |

## Architectural corrections made during the audit

The current code no longer matches several older documentation assumptions:

1. `:feature:contacts` does **not** own the generic invitation lifecycle; `:feature:invite` does.
2. Group membership/security is not owned by Chats; `:feature:membership` does.
3. `:feature:messaging` is the generic persisted transport-execution layer, not the cross-feature business coordinator.
4. `:feature:conversationorchestration` is the explicit cross-feature workflow boundary.
5. Use cases may compose other use cases where that composition is the intended workflow/orchestration boundary. The strict rules retained are that datasources do not call repositories and repositories do not call unrelated repositories/use cases to smuggle cross-feature orchestration into the data layer.
6. The current public server operator path is the single unified server bundle, not separate public Control Plane/Community Node launcher packages.
7. `server/control-plane-directory` must remain separate/private from the public unified bundle.
8. The current source has encrypted identity-key backup/restore and explicit reconnection state; recovery is not merely a future roadmap item.
9. A completed post-creation local phone-number migration/broadcast mechanism is **not** present in this snapshot and is therefore not documented as implemented.

## Validation limits

There is no `.git` history in the provided ZIP, so this audit cannot truthfully label the exact commit in which each class first appeared. “New/current” here means code present in this snapshot that was missing from, under-described by, or contradicted by the checked-in documentation.

The supplied snapshot also does not contain `.github/workflows`, so exact current CI trigger/change-classification behavior cannot be verified from this archive. Server packaging behavior is documented from the checked-in `server/unified` scripts themselves.

The `mkdocs` executable is not installed in the audit environment. Navigation targets and repository-local Markdown links were validated directly instead.
