# Glossary

**Community Node**  
A transport node that accepts client WebSocket connections, federates encrypted traffic, hosts blobs and participates in mailbox/offline delivery.

**Control Plane**  
Discovery/control infrastructure for node registration, health, presence and push registration/wake-up.

**Conversation orchestration**  
The explicit `:feature:conversationorchestration` boundary that coordinates identity, invitations, membership and conversation workflows without hiding repository-to-repository dependencies.

**MessagePartDto / MessagePart / MessagePartUi**  
The shared data/domain/presentation hierarchy in `:core:base`. Current variants are text, image, video, file, voice, location, contact and poll.

**MessageBlobEntity**  
Room row containing blob metadata/capabilities/crypto fields and the resolved local cache path for a blob-backed message part.

**MessagePartEntity**  
Room structural row for a message part (`id`, message, position, type and optional structured payload).

**OperationMessage**  
Protocol wrapper for an operation on an existing message. Its `MessageOperation` can edit, delete, react, vote in a poll or close a poll.

**Poll**  
A group-only `MessagePart` with 2–6 options, optional nested images, multiple-selection/vote-change/anonymity flags and optional expiry/close timestamps.

**Effective poll close**  
`PollPolicy.isClosedAt(now)` is true when a manual close timestamp exists or when the absolute expiry timestamp has elapsed. Only manual close is synchronized as `PollClose`.

**Pinned message**  
A group-admin-controlled snapshot of one group user message, stored as encoded `GroupMessageContent`. Pinned content can include attachments, voice, links and polls.

**Protocol outbox**  
Durable client storage for outbound protocol work, processed independently from a screen by `:feature:messaging` runtime.

**StartupTask**  
One application initialization/runtime-registration step consumed by `ApplicationStartupRunner`. Only tasks marked `waitForCompletion` block main navigation.

**App lock**  
Local device-owner authentication gate implemented by `:feature:applock`.

**Local voter ID**  
`PollPolicy.LOCAL_VOTER_ID`, an internal marker used in the locally stored poll until presentation maps it to the real local identity display name.

**DTO**  
A data-layer representation with a `Dto` suffix. Room persistence types remain `Entity`; presentation types use `Ui`; domain models are unsuffixed.
