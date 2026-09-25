# Current feature status

This page describes what is implemented in the current codebase. It is deliberately conservative: roadmap items are not listed as working features.

## Platform status

| Area | Status |
|---|---|
| Android client | Current usable development/product target |
| iOS client | KMP/Xcode structure exists, but major runtime/platform functionality is incomplete; not usable/supported |
| Control Plane | Implemented as Docker/Ktor services + Windows launcher bundle |
| Community Node | Implemented as Docker/Ktor services + Windows and macOS/Linux launcher bundles |
| Official tagged release | **Not published yet**; workflow/package format is implemented |

## Android application

### Onboarding and identity

- welcome/privacy/phone/permission onboarding;
- local encryption + signing identity creation;
- Android-protected private-key persistence;
- public identity sharing;
- QR/manual identity import paths.

### Contacts and trust

- device-contact import/linking;
- phone normalization/merge behavior;
- contact invitations inbox;
- accept/decline/decline+block;
- block/unblock management;
- public identity exchange/acknowledgement;
- contact details/security state;
- safety-number and QR verification flows.

### Direct chats

- persistent conversations/messages;
- encrypted transport payload path;
- persistent outgoing outbox and retry;
- sent/delivered/read state;
- unread/read handling;
- typing indicator;
- identity/security-state handling;
- conversation deletion/authorization-revocation behavior;
- messages queued during automatic re-invitation are released on acceptance, discarded on decline, and expire after two days;
- text plus typed image/video/file/location/contact message parts.

### Group chats

- group creation and invitation flows;
- membership activation and security key distribution;
- add/remove members;
- multiple admins/member promotion;
- leave/admin-transfer requirements;
- group verification snapshots/receipts;
- epoch-based group encryption state;
- one outgoing packet per current active recipient;
- per-recipient delivery/read aggregation;
- group typing indicators;
- re-invitation through a new active membership period;
- the same typed text/image/video/file/location/contact message content used by Direct chats.

There is no orphaned-group mode in the current architecture.

### Attachments and media

Sparrow currently supports attachment types `IMAGE`, `VIDEO`, `FILE`, `LOCATION`, and `CONTACT`. All five use the encrypted blob attachment transport.

- up to 8 selected attachments per normal media/file message; location/contact actions are single-shot attachment messages;
- image limit: 4 MiB per image;
- video limit: 64 MiB per video;
- file limit: 96 MiB per file;
- total selected attachment payload limit: 96 MiB per message;
- gallery image/video selection and camera capture;
- file browser/file selection and opening of sent/received files;
- image/video bubble previews with at most three visible media tiles and a `+N` overflow tile;
- swipeable media viewer; videos do not autoplay;
- current-location sharing sends immediately after location acquisition;
- contact sharing reuses the existing Contacts selection UI and sends the selected contact immediately;
- contact bubbles show the available display name and phone number; tapping can add the contact to device contacts after confirmation;
- incoming image/video/file data is saved into the conversation attachment storage; location/contact blobs are not copied into the media/files storage tree;
- attachment storage overview/management and media export are available from Settings.

See [Attachments](attachments.md).

### Search and local AI

- normal local message search;
- optional semantic search;
- a shared on-device text-embedding model is downloaded/prepared only when a feature needs it;
- the downloaded model is integrity-checked before use;
- semantic indexing is built locally;
- search combines exact results with semantic results and falls back to exact results if semantic search is unavailable;
- search results navigate back to the matching Direct or Group message.

See [Message search](search.md).

### Message safety

- optional local message-safety analysis;
- structural checks for suspicious links/domains and high-risk request patterns;
- local embedding-based classification when the shared model is available;
- warning indicator in message presentation;
- details screen explaining detected reasons;
- block action from the safety details flow.

The analysis is local; it is not a cloud moderation service. See [Message safety](message-safety.md).

### Settings and diagnostics

- semantic-search and message-safety feature toggles with model/download/index state;
- attachment-storage entry and per-conversation attachment management;
- developer/network diagnostics;
- persisted developer error log with visible timestamps and a clear-log action.

See [Settings and diagnostics](settings.md).

### Network/operations UI

- configurable Control Plane directory loaded by `AppViewModel`;
- Settings Add field accepts a directory URL or one plane URL;
- multiple planes and health state;
- signed/verified node discovery;
- node failover/cooldown;
- current node and connection-count diagnostics;
- cooldown nodes shown with zero active connections.

### Offline/background

- WebSocket foreground delivery;
- recipient-selected mailbox delivery;
- Android FCM wake-up path;
- pending mailbox/envelope synchronization workers;
- conversation notifications/deep links.

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

The current Room database is schema **53** and includes durable recovery, invitation, membership, pin, link-preview, auto-reply, mailbox-route and protocol-outbox failure state. See [Persistence model](../architecture/persistence.md).


## Avatars and profile-picture editing

The active `:feature:avatar` module owns target-aware avatar observation and the reusable profile-picture editing pipeline. `AvatarRepositoryImpl`, `AvatarViewModel` and `SparrowAvatar` keep image loading out of broad chat UI state, while `AvatarEditorViewModel`, `ImagePicker` and `ProfilePictureCropper` implement selection and cropping. See [Avatars](avatar.md).
