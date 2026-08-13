# Chats

`:feature:chats` owns conversations, message behavior, delivery/read receipts, group behavior, chat
presentation, and the typed protocol handlers whose packets affect chats.

It does not own WebSocket lifecycle or gateway routing.

See [Conversation, Messaging, and Delivery Flow](message-transport-flow.md) for the exact outgoing,
incoming, group-membership, and conversation-deletion call chains.

## Package structure

```text
feature/chats/.../feature/chats/
├── domain/
│   ├── model/        # conversations, message state, state machine
│   ├── repository/   # ChatsRepository, GroupKeyStorage, typing port
│   └── usecase/      # send, retry, read, observe, group operations
├── data/
│   ├── conversation/ # DirectConversationStore
│   ├── delivery/     # MessageDeliveryStateCoordinator
│   ├── incoming/     # IncomingMessageProcessor
│   ├── invitation/   # Pending identity handshake and group activation
│   ├── outbox/       # ChatOutboxDeliveryStateListener
│   ├── protocol/     # typed chat/group/receipt handlers
│   ├── repository/   # DefaultChatsRepository
│   └── security/     # GroupSecurityManager and canonical payload encoding
├── androidMain/data/security/
│   └── AndroidGroupKeyStorage.kt
├── presentation/
│   ├── component/
│   │   └── groupdetails/ # one previewable component per file
│   ├── mapper/
│   ├── model/
│   ├── ContactsFlow.kt    # contacts-to-create-group feature flow
│   ├── screen/
│       ├── overview/
│       ├── chat/
│       ├── details/
│       └── create/
│   └── *Route.kt        # state collection and navigation-facing contracts
└── di/ChatsModule.kt
```

## Domain entry points

| Use case | Operation |
|---|---|
| `ObserveConversations` | Conversation overview |
| `ObserveConversation` | One direct or group conversation and messages |
| `GetOrCreateDirectConversation` | Stable direct conversation for a contact |
| `SendMessage` | Queue a direct message |
| `SendGroupMessage` | Encrypt once with the epoch key and queue one packet per participant |
| `RetryMessage` | Retry failed direct or recipient-specific outbox rows |
| `MarkConversationRead` | Queue read receipts |
| `CreateGroupConversation` | Create a pending group and send signed invitations |
| `AddGroupMembers` | Send owner-signed invitations from group details |
| `RemoveGroupMember` | Remove a pending member or rotate the epoch after removing an active member |
| `LeaveGroup` | Queue a signed member leave request and make the local group read-only while the owner rotates the epoch |
| `DeleteConversation` | Revoke direct-chat authorization before local deletion, leave/decline a joined group before hiding it locally, or propagate owner deletion |
| `AcceptGroupInvitation` / `DeclineGroupInvitation` | Apply the invitee's explicit decision |
| `ObserveGroupConversation` | Group metadata and participants |
| `ObserveTypingIndicator` / `SetTypingIndicator` | Ephemeral typing through a gateway |

`ChatsRepository` contains conversation operations only. Transport payload decoding enters through
the protocol-level `IncomingMessageHandler` port instead.

## Repository and persistence

`DefaultChatsRepository` uses `ChatDao`, `MessageRecipientStateDao`, `DirectConversationStore`,
`MessageDeliveryStateCoordinator`, `GroupInvitationDao`, `GroupInvitationCoordinator`,
`GroupMessageSender`, and `ProtocolOutbox`.

Outgoing messages are persisted before their packets are enqueued. This gives the UI an immediate
`QUEUED` row and lets outbox callbacks find the visible message by `packetId`.

`DirectConversationStore` centralizes reuse/creation of direct conversations so outgoing and
incoming paths do not invent separate IDs.

Direct-message permission is separate from stored identity keys. In automatic mode,
`IdentityInvitationService.requireDirectChatAuthorization()` requires the contact's latest
invitation state to be `MUTUAL_UNVERIFIED`. `DefaultChatsRepository.sendMessage()` checks it before
persisting an outgoing message, and `ChatMessagePacketHandler` checks it before accepting an
incoming message. A decline or signed direct-conversation deletion therefore makes the composer
read-only until a fresh invitation is accepted.

## Direct and group messages

A direct `MessageEntity` links to one `ChatMessagePacket.packetId`.

A group message has:

- one visible `MessageEntity` and `messageId`;
- one XChaCha20-Poly1305 encryption result shared by every recipient packet;
- one Ed25519 sender signature shared by every recipient packet;
- one `GroupChatMessagePacket` per participant, with a distinct transport `packetId`;
- one `MessageRecipientStateEntity` per participant and packet.

`MessageDeliveryStateMachine.aggregate()` derives the visible group status from all recipient
states.

## Incoming pipeline

`IncomingMessageProcessor` implements `IncomingMessageHandler`. It decodes the transport payload,
decodes the `SecureChatPacket`, creates `IncomingPacketContext`, and delegates to
`ProtocolPacketHandler`.

Chat-owned typed handlers:

| Handler | Behavior |
|---|---|
| `ChatMessagePacketHandler` | Upsert direct message and queue delivery receipt |
| `GroupInvitePacketHandler` | Verify the owner, persist the pending group, and wait for user consent |
| `GroupJoinRequestPacketHandler` | Verify the invited contact, store its identity, and attempt activation |
| `GroupLeaveRequestPacketHandler` | Verify an active member's signed leave request and run the owner removal/rotation path |
| `GroupInviteDeclinedPacketHandler` | Verify and persist a member's declined decision |
| `GroupCreatedPacketHandler` | Verify owner, unwrap the epoch key, persist membership, and acknowledge readiness |
| `GroupReadyAcknowledgementPacketHandler` | Verify that a member installed the welcome key |
| `GroupMemberRemovedPacketHandler` | Verify an owner removal, clear local group security, retain history, and make the chat read-only |
| `GroupConversationDeletedPacketHandler` | Verify an owner deletion, retain member history, clear group security, and mark the chat read-only |
| `GroupChatMessagePacketHandler` | Verify membership/signature, decrypt, persist, queue receipt |
| `DeliveryReceiptPacketHandler` | Apply `DELIVERY_CONFIRMED` |
| `ReadReceiptPacketHandler` | Apply `READ_CONFIRMED` |

Unreadable transport data is stored as an incoming message with a `MessageContentStatus` explaining
the failure.

## Delivery state

`MessageDeliveryStateMachine` is the only definition of visible transition rules.
`MessageDeliveryStateCoordinator` loads and persists direct or per-recipient state.
`ChatOutboxDeliveryStateListener` maps protocol-outbox callbacks to chat events.

| State | Meaning |
|---|---|
| `QUEUED` | Locally queued |
| `SENDING` | Current outbox attempt is running |
| `SENT` | Gateway accepted the envelope |
| `DELIVERED` | Recipient stored the message |
| `READ` | Recipient returned a read receipt |
| `FAILED` | Current local attempt failed |
| `NOT_APPLICABLE` | Incoming message |

Read [Conversation, Messaging, and Delivery Flow](message-transport-flow.md) for state machines,
retry, gateway ACKs, encryption selection, and class-by-class direct and group flow.

## Secure group architecture

Group content uses one random 256-bit key per group epoch. Epoch 1 is created with the group.
Every selected contact has an independent invitation and activation state. As soon as one accepted
member reaches `ACTIVE`, the owner can send encrypted group messages to that member while other
invitations remain pending. Adding or removing an active member advances the epoch and distributes
a fresh key to the resulting membership.

| Class | Responsibility |
|---|---|
| `GroupInvitationCoordinator` | Create/receive per-member invitations, add/remove members, distribute or rotate epochs, propagate active membership, and flush queued content |
| `GroupInvitationManager` | Create and verify signed invite, join, decline, leave, removal, deletion, and ready-acknowledgement packets |
| `GroupInvitationDao` / `GroupInvitationEntity` | Persist every per-contact invitation transition |
| `GroupMessageSender` | Persist pre-activation messages and fan them out after every member is ready |
| `GroupSecurityManager` | Orchestrate welcome creation/opening and group-message protection |
| `GroupProtocolPayloadEncoder` | Produce deterministic bytes for AEAD associated data and Ed25519 signatures |
| `GroupCrypto` / `SodiumGroupCrypto` | XChaCha20-Poly1305, sealed-key wrapping, Ed25519, random key generation |
| `GroupKeyStorage` | Platform-neutral contract for local epoch keys |
| `AndroidGroupKeyStorage` | AES-GCM-wrap epoch keys with an AES-256 Android Keystore key |
| `GroupSecurityDao` | Persist current epoch and immutable remote member-key snapshots |
| `GroupSecurityStateEntity` | Current epoch, owner key, and this device's member signing key |
| `GroupMemberKeyEntity` | Expected encryption/signing keys for one remote member in one epoch |

The raw group key is never placed in `GroupCreatedPacket`, `ProtocolOutboxEntity`, or Room.
`GroupCreatedPacket.wrappedGroupKey` is a libsodium sealed box for exactly one recipient. The
packet is also signed by the owner's Ed25519 identity key and transported with `SEALED_BOX`.

`GroupChatMessagePacket` contains `epoch`, `nonce`, `ciphertext`, and `senderSignature`; it does
not contain plaintext or a sender-supplied phone/key used for identity resolution. The receiving
handler takes the sender from `IncomingPacketContext.contactId`, loads that member's stored key
snapshot, verifies the signature, and only then decrypts.

Group-message content does not depend on pairwise identities between every member. The packet may
use plaintext **outer transport** when a recipient has no pairwise identity because its inner
payload is already authenticated group ciphertext. `GROUP_E2EE` is persisted as the message
security mode so the UI does not incorrectly describe this as an insecure message.

### Creating a group without existing identities

`GroupInvitationCoordinator.createGroup()` creates the conversation and one
`GroupInvitationEntity` in `INVITE_SENT` for every selected contact. Every contact receives an
invite, including contacts whose identity is already known, because membership requires explicit
consent.

The complete state flow is:

| Side | Persisted status | Trigger and next action |
|---|---|---|
| Creator | `INVITE_SENT` | `createGroup()` signs and enqueues `GroupInvitePacket` |
| Recipient | `AWAITING_ACCEPTANCE` | `receiveInvite()` verifies the owner and creates the visible pending group |
| Recipient | `JOIN_SENT` | `acceptInvitation()` marks the owner identity mutual and enqueues `GroupJoinRequestPacket` |
| Creator | `IDENTITY_READY` | `receiveJoinRequest()` verifies this member identity and immediately calls `activateGroupIfReady()` |
| Creator | `WELCOME_SENT` | `distributeGroupKeyToMember()` enqueues this member's `GroupCreatedPacket` |
| Recipient | `WAITING_FOR_ACTIVATION` | `GroupCreatedPacketHandler` unwraps/persists the key and enqueues `GroupReadyAcknowledgementPacket` |
| Creator | `ACTIVE` | `receiveReadyAcknowledgement()` verifies key possession, adds this participant, and propagates `GroupMemberActivatedPacket` |
| Recipient | `ACTIVE` | `GroupMemberActivatedPacketHandler` applies the final activation for the local member |

Declining follows a separate signed path:
`DeclineGroupInvitation` → `GroupInvitationCoordinator.declineInvitation()` →
`GroupInviteDeclinedPacket` → `GroupInviteDeclinedPacketHandler` → creator status `DECLINED`.
After the packet is queued, the recipient changes its own invitation from `AWAITING_ACCEPTANCE` to
`DECLINED`. `GroupInvitationCoordinator.declineInvitation()` never deletes the conversation:
existing messages and a newly created invitation chat both remain visible and read-only. Only an
explicit user-initiated conversation deletion may remove the conversation and its cascaded data.
When the retained conversation has messages, `ChatScreen` continues to render
`GroupMembershipRemovedHint`; the accept/decline actions disappear, but the user still sees that
they are no longer part of the group and can only read the existing history.

The creator may type while members are pending. If no participant is active,
`GroupMessageSender.queueOrSend()` stores a visible `MessageEntity` with `QUEUED`, but creates no
ciphertext, recipient state, or outbox packet yet. As soon as at least one member is active,
`flushQueued()` encrypts stored messages once and creates one `MessageRecipientStateEntity` and
`GroupChatMessagePacket` per currently active member. Later pending invitations do not block these
sends. An invitee cannot send until its own welcome and final activation have been installed.

Activation is retry-safe: `GroupSecurityManager.createOwnedGroup()` reuses an already stored owner
key and deterministic welcome packet IDs after an interrupted attempt. Ready acknowledgement and
queued-message packet IDs are deterministic as well, and `DefaultProtocolOutbox` deduplicates them.
Welcome packet IDs include the exact invitation ID. A newly added or re-added member can install
the current group epoch even when it has no earlier local group state, while a stale welcome from
an earlier invitation cannot activate the new invitation.

This handshake proves possession and establishes encryption keys, but a previously unknown identity
is still unverified. Safety-number verification remains the defense against a malicious gateway
performing first-contact key substitution.

### Owner membership management

`GroupDetailsFlow` owns `AddGroupMembersScreen`, `RemoveGroupMemberScreen`, and
`LeaveGroupScreen`; none is exposed as an app navigation route. `GroupVerificationViewModel` calls
`AddGroupMembers`, `RemoveGroupMember`, or `LeaveGroup`, which delegate through `ChatsRepository`
to `GroupInvitationCoordinator`.

Adding a contact sends the normal signed invitation and requires explicit acceptance. Activation
of that contact advances the epoch, creates a fresh key, snapshots the full membership in
`GroupMemberKeyEntity`, and sends a signed, individually wrapped `GroupCreatedPacket` to each
member in the new epoch. If a chat already contains messages,
`GroupInvitationCoordinator.receiveReadyAcknowledgement()` records
`GroupMembershipMessageFactory.memberAdded()` for the owner, while
`GroupCreatedPacketHandler` derives the same event for existing members from the previous and new
signed snapshots. A first local snapshot has no previous participants, so a newly joining or
rejoining device does not receive a list of false “was added” events.

Removing a contact whose key was never distributed sends an owner-signed
`GroupMemberRemovedPacket` and records `REMOVED`. Removing an active contact—or one whose welcome
key is already queued—first rotates to
`currentEpoch + 1`, omits that contact from both the key snapshot and transport recipients, and
then sends the removed member its signed removal packet. The removed device validates the original
invitation challenge, deletes its group keys and Room security snapshot, retains its existing
conversation history, clears its obsolete verification snapshot, persists a local removal system
message, and exposes the chat as read-only
with `GroupConversationState.REMOVED`. The owner persists the same membership event locally;
remaining members derive it by comparing their old participants with the next signed welcome
snapshot. Old-epoch messages are rejected by the remaining members.

An active non-owner may leave through `LeaveGroup`. The member signs a
`GroupLeaveRequestPacket`, moves its invitation to `LEAVE_SENT`, and becomes read-only while the
request is queued. `GroupLeaveRequestPacketHandler` verifies the member, invitation challenge, and
epoch on the owner, then calls the same locked removal and epoch-rotation implementation used by
`RemoveGroupMember`. The owner records `GroupMembershipMessageFactory.memberLeft()` and returns an
owner-signed `GroupMemberRemovedPacket` whose `reason` is `MEMBER_LEFT`; the departing device
records `localMembershipLeft()`, deletes current group security, retains history, and finishes in
`REMOVED`. The next `GroupCreatedPacket` also carries an owner-signed
`GroupMembershipChangePayload(reason = MEMBER_LEFT, memberSigningPublicKey)`. Remaining members match that
key to their previous `GroupMemberKeyEntity` and record `memberLeft()` for the exact departing
contact instead of treating the snapshot difference as an admin removal.

A terminal invitation row is atomically replaced through
`GroupInvitationDao.replaceForGroupAndContact()` when
`GroupInvitationCoordinator.receiveInvite()` receives a new invitation for the same group and
owner. This makes remove-then-add a normal fresh consent flow instead of conflicting with the
unique `(groupId, contactId)` invitation index.

## Presentation

`ChatsViewModel` owns the overview state. `ChatViewModel` owns a direct conversation.
`GroupChatViewModel` owns a group conversation. `GroupVerificationViewModel` owns group details,
membership management, and verification selection. `GroupMemberQrVerificationViewModel` owns
group QR verification state.
`CreateGroupViewModel` owns group title, selection, and creation.

`CreateGroupScreen` reuses `ContactsScreen` from `:feature:contacts` with
`ContactsScreenMode.GroupSelection`; the normal contacts route uses the same screen with
`ContactsScreenMode.Overview`.

`AddGroupMembersScreen` reuses it with `ContactsScreenMode.MemberSelection` inside
`GroupDetailsFlow`.

Screen-specific components live in `presentation/component/<screen-name>`, one component per file,
with a preview next to the component. Screens render state; flows collect state and coordinate
screen changes; ViewModels call use cases rather than DAOs, `ProtocolOutbox`, crypto
implementations, or `WebSocketTransportClient`.

## Extension rules

- Add chat behavior through `ChatsRepository` and a use case.
- Add packet meaning through a chat-owned `TypedProtocolPacketHandler`.
- Persist outgoing UI state before enqueueing.
- Treat incoming packets and receipts as duplicate/reorder tolerant.
- Add group recipient behavior to per-recipient state before changing aggregation.
- Keep gateway and WebSocket classes out of this module; use protocol and typing ports.
