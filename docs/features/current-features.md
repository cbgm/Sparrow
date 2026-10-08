# Current feature status

This page describes the implemented behavior in the supplied 2026-10-08 release source tree. Roadmap-only ideas are not listed as working features.

## Platform status

| Area | Status |
|---|---|
| Android client | Current usable target |
| iOS client | KMP/Xcode/source-set structure exists, but important runtime/platform functionality is incomplete |
| Control Plane | Implemented as Docker/Ktor services + operator tooling |
| Community Node | Implemented as Docker/Ktor services + operator tooling |

## Identity, onboarding and trust

- onboarding, phone entry/hint, permission flow and identity creation;
- local signing/encryption identity and encrypted backup/restore;
- public identity exchange, acknowledgement and manual/QR import;
- durable pending remote-identity-change review and approved reconnection retry;
- safety-number/QR verification;
- device-contact import, contact merge/routing projection and blocking;
- invitation inbox for Direct/Group flows;
- explicit conversation orchestration in `:feature:conversationorchestration`.

## Direct chats

- persistent conversations/messages;
- encrypted direct transport and authorization gates;
- durable outbox/retry and queued-until-authorized messages;
- sent/delivered/read state and typing indicators;
- edit/delete/reaction operations through `OperationMessage`;
- text, image, video, file, voice, location and contact message parts;
- poll operations are explicitly rejected in Direct chats.

## Group chats

- group creation/invitation/join/welcome/activation flows;
- epoch/group security state and member key distribution;
- multiple admins, promotion/removal/leave/admin transfer;
- per-recipient delivery/read aggregation;
- typing indicators;
- edit/delete/reaction operations;
- one current pinned message;
- group polls including images, voting, anonymity, optional expiry and manual creator/admin close.

## Polls

`:feature:polls` is implemented and wired as a group message-part feature:

- 2–6 options;
- optional description;
- images from gallery only;
- optional multiple selection;
- optional vote changes;
- anonymous/non-anonymous voter presentation;
- duration entered as minutes and converted to an absolute expiry timestamp;
- live `Open for …` countdown while visible with lifecycle resync on resume;
- one effective `isClosed` state derived by `PollPolicy.isClosedAt(...)`;
- option tap submits a vote immediately;
- creator/admin manual close sends `MessageOperation.PollClose`;
- elapsed expiry does not broadcast an automatic close operation;
- voter detail overlay grouped by option;
- polls can be pinned and remain interactive.

See [Polls](polls.md).

## Attachments/media/voice

The shared `:core:base` message-part hierarchy supports `Text`, `Image`, `Video`, `File`, `Voice`, `Location`, `Contact` and `Poll`.

`:feature:attachments` owns encrypted blob transfer/cache/storage; `:feature:media` owns selection/viewers/export; `:feature:voice` owns recording/playback/transcription.

- media/file management screens and per-conversation storage summaries;
- image/video bubble previews and media viewer;
- saved file opening/export;
- local voice transcription with persisted transcripts;
- receiver-side cache paths are persisted in `MessageBlobEntity.localFilePath` and mapped back into UI models.

See [Attachments](attachments.md) and [Voice](voice.md).

## App lock

`:feature:applock` is wired into Settings and application navigation. It uses persisted enabled state plus platform device-owner authentication through `AppLockAuthenticationLauncher`. Cancelling authentication leaves the app locked and allows a new request.

See [App lock](app-lock.md).

## Startup/runtime

Application startup is task-based through `ApplicationStartupRunner` and concrete `StartupTask` instances. Only waiting tasks block navigation. Non-waiting observers/runtime work starts after `AppRoute.Main` through `startPostNavigationRuntime()` and `ForegroundRuntimeCoordinator`.

See [Startup architecture](../architecture/startup.md).

## Search, safety, links, auto reply and avatars

- exact local message search and optional local semantic search;
- local message-safety analysis;
- cached/prefetched link previews plus server link-preview service;
- persisted auto-reply rules/recipient claims;
- reusable avatar observation/editing/cropping pipeline.

## Settings and diagnostics

- app-lock toggle with device authentication;
- semantic search and message-safety settings;
- attachment storage/management;
- network/developer diagnostics and persisted error log;
- Control Plane/node state and connection diagnostics.

## Offline/background transport

- foreground WebSocket delivery;
- durable protocol outbox;
- recipient mailbox delivery;
- Android FCM wake-up path;
- synchronization workers and conversation notifications/deep links.

## Server

### Control Plane

- signed Community Node registration/directory;
- node heartbeat/health expiry;
- Redis-backed presence routes;
- PostgreSQL-backed push registrations/wake-ups;
- Firebase Admin integration;
- Caddy reverse proxy and `/index` operator page;
- LAN/Public deployment through the unified server manager.

### Community Node

- client WebSocket gateway;
- signed presence-route registration/refresh;
- local delivery;
- cross-node federation;
- durable federation retry queue;
- recipient mailbox persistence/capabilities;
- encrypted blob upload/download used by message attachments;
- Control Plane directory parsing/caching/retry;
- continuous registration/heartbeat;
- Caddy edge and `/index` operator page;
- Windows and macOS/Linux lifecycle through the unified server manager.

## Release/build automation

- Android debug/release builds and signing configuration are present in the source tree.
- Current server packaging is the single unified `dist/sparrow-server.zip` built from `server/unified`.
- `server/control-plane-directory` is private operator infrastructure and is deliberately excluded from that public bundle.
- This source snapshot does not include `.github/workflows`; exact current CI trigger/change-classification behavior therefore must be verified from the release checkout rather than inferred here.

## Not currently advertised as working

- usable iOS client;
- multi-device identity synchronization;
- desktop client release;
- voice/video calling;
- a completed independent security audit.

## Additional implemented modules present in the current source snapshot

### Identity backup and recovery

The current code includes encrypted identity export/restore (`PrepareIdentityBackupUseCase`, `RestoreIdentityBackupUseCase`, `AndroidIdentityBackupCodec`) plus durable remote-key replacement review and reconnection (`PendingRemoteIdentityChange`, `ApprovedIdentityReconnection`, `ApprovedIdentityReconnectionObserver`, `ApprovedReconnectionRetryWorker`, `StartRecoveryInvitationUseCase`). See [Identity backup, recovery and reconnection](identity-recovery.md).

### Generic invitations and orchestration

Invitation lifecycle is now a dedicated `:feature:invite` concern, while `:feature:conversationorchestration` contains `ConversationFlowHandler` and result observers that coordinate Invite, Identity, Membership and Chats. See [Invitations](invitations.md) and [Runtime orchestration](../architecture/runtime-flows.md).

### Group membership module

Group membership/security is implemented in `:feature:membership` rather than inside Chats. This includes `GroupMembershipStateMachine`, `GroupSecurityManager`, Group welcome/activation datasources, admin promotion/removal/leave, current epoch/member routing, and the membership result stream. See [Group membership and group security](group-membership.md).

### Voice and transcription

`:feature:voice` implements attachment-backed recording/playback plus optional local Whisper transcription using `AndroidVoiceTranscriptionRepository`, `AndroidWhisperModelStore` and `WhisperNative`. See [Voice messages and local transcription](voice.md).

### Link previews

`:feature:linkpreview` implements client cache/fetch/rendering and `:server:link-preview` implements URL validation, HTML parsing, metadata caching and proxied preview images. See [Link previews](link-previews.md).

### Auto reply

`:feature:autoreply` persists configured replies and recipient claims with `AutoReplyEntity`/`AutoReplyRecipientEntity`, and exposes create/update/activate/deactivate/claim/release use cases. See [Auto reply](auto-reply.md).

### Group pinned messages

Group admins can synchronize current pin state through `GroupPinRepositoryImpl`, `GroupPinBroadcaster`, `GroupPinUpdatedPacket`, `PinGroupMessageUseCase` and `UnpinGroupMessageUseCase`. See [Group pinned messages](pinned-messages.md).

### Persistence

The current Room database is schema **54** and includes durable recovery, invitation, membership, pin, link-preview, auto-reply, mailbox-route and protocol-outbox failure state. See [Persistence model](../architecture/persistence.md).


## Avatars and profile-picture editing

The active `:feature:avatar` module owns target-aware avatar observation and the reusable profile-picture editing pipeline. `AvatarRepositoryImpl`, `AvatarViewModel` and `SparrowAvatar` keep image loading out of broad chat UI state, while `AvatarEditorViewModel`, `ImagePicker` and `ProfilePictureCropper` implement selection and cropping. See [Avatars](avatar.md).
