# SecureChat Protocol

`:core:protocol` defines transport-independent messages and ports. It contains no Ktor, Room,
Compose, contact repository, or platform crypto implementation.

For the full direct and group runtime path, see
[Conversation, Messaging, and Delivery Flow](../features/message-transport-flow.md).

## Packet envelope

Every application packet implements:

```kotlin
sealed interface SecureChatPacket {
    val packetId: String
    val version: Int
}
```

`packetId` identifies one protocol operation and is the persistent outbox idempotency key.
`version` is checked by `ProtocolVersion` during both encoding and decoding.

`createProtocolJson()` uses `packetType` as the sealed-class discriminator. It encodes defaults,
rejects unknown keys, is not lenient, and omits explicit nulls.

Conceptual JSON:

```json
{
  "packetType": "chat_message",
  "packetId": "packet-...",
  "version": 1,
  "messageId": "message-...",
  "sentAtEpochMilliseconds": 123456789,
  "text": "Hello"
}
```

The actual property set depends on the packet class.

## Packet catalog

| Discriminator | Class | Important fields | Handled by |
|---|---|---|---|
| `chat_message` | `ChatMessagePacket` | `messageId`, timestamp, text, optional sender phone | `ChatMessagePacketHandler` |
| `delivery_receipt` | `DeliveryReceiptPacket` | `messageId`, delivery timestamp | `DeliveryReceiptPacketHandler` |
| `read_receipt` | `ReadReceiptPacket` | `messageId`, read timestamp | `ReadReceiptPacketHandler` |
| `contact_invite` | `ContactInvitePacket` | invitation ID, expiry, challenge, inviter keys and signature | `ContactInvitePacketHandler` |
| `contact_invite_accepted` | `ContactInviteAcceptedPacket` | both challenges, both public identities, responder signature | `ContactInviteAcceptedPacketHandler` |
| `contact_ready` | `ContactReadyPacket` | response challenge, accepted responder keys, inviter signature | `ContactReadyPacketHandler` |
| `contact_verification_receipt` | `ContactVerificationReceiptPacket` | receipt ID, both identity snapshots, signature | `ContactVerificationReceiptPacketHandler` |
| `contact_invite_declined` | `ContactInviteDeclinedPacket` | invitation ID, challenge, decliner key and signature | `ContactInviteDeclinedPacketHandler` |
| `direct_chat_authorization_revoked` | `DirectChatAuthorizationRevokedPacket` | invitation ID, challenge, revocation timestamp, revoker key and signature | `DirectChatAuthorizationRevokedPacketHandler` |
| `identity` | `IdentityPacket` | display name and public encryption/signing keys | `IdentityPacketHandler` |
| `identity_acknowledgement` | `IdentityAcknowledgementPacket` | sender key, acknowledged keys, signature | `IdentityAcknowledgementPacketHandler` |
| `group_invite` | `GroupInvitePacket` | invitation/group metadata, challenge, owner public identity, owner signature | `GroupInvitePacketHandler` |
| `group_join_request` | `GroupJoinRequestPacket` | invitation/group IDs, challenge, member public identity, member signature | `GroupJoinRequestPacketHandler` |
| `group_invite_declined` | `GroupInviteDeclinedPacket` | invitation/group IDs, challenge, member signing key/signature | `GroupInviteDeclinedPacketHandler` |
| `group_leave_request` | `GroupLeaveRequestPacket` | invitation/group IDs, epoch, challenge, member signing key, request timestamp/signature | `GroupLeaveRequestPacketHandler` |
| `group_created` | `GroupCreatedPacket` | group metadata, epoch, members, recipient-wrapped key, owner signature | `GroupCreatedPacketHandler` |
| `group_ready_acknowledgement` | `GroupReadyAcknowledgementPacket` | group ID, epoch, welcome packet ID, key confirmation, member signature | `GroupReadyAcknowledgementPacketHandler` |
| `group_member_activated` | `GroupMemberActivatedPacket` | group/epoch, activation round, member snapshot, sender signature | `GroupMemberActivatedPacketHandler` |
| `group_member_activation_acknowledgement` | `GroupMemberActivationAcknowledgementPacket` | group/epoch, activation packet ID and acknowledger signature | `GroupMemberActivationAcknowledgementPacketHandler` |
| `group_member_removed` | `GroupMemberRemovedPacket` | invitation/group IDs, challenge, next epoch, reason, removed signing key, owner signature | `GroupMemberRemovedPacketHandler` |
| `group_conversation_deleted` | `GroupConversationDeletedPacket` | invitation/group IDs, epoch, challenge, deletion timestamp, owner signature | `GroupConversationDeletedPacketHandler` |
| `group_verification_receipt` | `GroupVerificationReceiptPacket` | group/invitation IDs, owner and participant identity snapshots, signature | `GroupVerificationReceiptPacketHandler` |
| `group_verification_snapshot_request` | `GroupVerificationSnapshotRequestPacket` | group/invitation/request IDs and requester signature | `GroupVerificationSnapshotRequestPacketHandler` |
| `group_verification_snapshot` | `GroupVerificationSnapshotPacket` | group ID, member verification rows, owner identity and signature | `GroupVerificationSnapshotPacketHandler` |
| `group_chat_message` | `GroupChatMessagePacket` | group/message IDs, epoch, nonce, ciphertext, sender signature | `GroupChatMessagePacketHandler` |

Packet definitions live under:

```text
core/protocol/src/commonMain/kotlin/com/cbgm/securechat/core/protocol/packet/
```

## Encoding

`PacketCodec` is the contract. `KotlinxPacketCodec` is the production implementation.

Encoding:

1. checks `ProtocolVersion.isSupported(packet.version)`;
2. serializes through `SecureChatPacket.serializer()`;
3. returns UTF-8 JSON bytes.

Decoding:

1. rejects an empty byte array;
2. decodes strict UTF-8;
3. deserializes through the sealed `SecureChatPacket` serializer;
4. rejects an unsupported version.

Packet bytes are not a gateway frame. The outgoing messaging pipeline next wraps them in
`EncryptedTransportPayload` and encodes that value with `TransportPayloadCodec`.

## Direct authorization packet rules

`ContactInvitePacket`, `ContactInviteAcceptedPacket`, and `ContactReadyPacket` establish one
direct-chat authorization. Stored public keys may be reused by a later handshake, but they do not
authorize messages by themselves. `ContactInviteDeclinedPacket` makes the referenced outgoing
invitation terminal.

`DirectChatAuthorizationRevokedPacket` is queued before a direct conversation is deleted locally.
`IdentityInvitationPayloadEncoder.encodeDirectChatAuthorizationRevoked()` binds the packet ID,
version, invitation ID, revocation timestamp, original invitation challenge, and revoker signing
key. `DirectChatAuthorizationRevokedPacketHandler` accepts it only for the matching contact and
pinned remote signing key, then changes the invitation to `CONVERSATION_DELETED` without deleting
the recipient's message history. A fresh invitation must complete before direct messages are
accepted again.

## Secure group packet rules

`GroupInvitePacket` bootstraps an identity when the group owner only knows a normal contact and its
phone-derived routing address. `GroupProtocolPayloadEncoder.encodeInvite()` binds the invitation ID,
group metadata, expiry, random challenge, and owner's encryption and signing public keys to the
owner signature. It contains no group key.

`GroupJoinRequestPacket` returns the same invitation ID, group ID, and challenge together with the
invitee's public identity. `GroupProtocolPayloadEncoder.encodeJoinRequest()` binds those fields to
the invitee signature. `GroupInvitationCoordinator` accepts it only for the exact persisted
invitation and contact, before expiry, and rejects conflicts with a previously stored identity.

`GroupInviteDeclinedPacket` binds the same invitation, group, challenge, and invitee signing key.
The creator accepts it only from the contact stored on that invitation. The invitee persists its
local invitation as `DECLINED` after enqueueing the packet and retains the conversation and its
message history; declining an invitation is not a conversation-deletion operation.

`GroupLeaveRequestPacket` is member-signed and valid only for the stored active invitation and
authenticated sender contact. `GroupProtocolPayloadEncoder.encodeLeaveRequest()` binds the group
epoch, original invitation challenge, member signing key, and request timestamp. The owner accepts
the request when its epoch is current or older, but never when it references a future epoch; this
keeps an already queued request valid across an unrelated owner rotation. The packet requires
encrypted outer transport through `DefaultOutgoingPacketTransportPolicy`.

`GroupCreatedPacket` is a signed initial welcome or later epoch update. Every recipient gets a
different packet because `wrappedGroupKey` is created for that recipient's X25519 encryption
public key. The signed payload
binds `packetId`, protocol version, group ID/title/timestamp, epoch, the complete ordered
`GroupMemberPayload` list, `wrappedGroupKey`, and any optional `membershipChange`. A voluntary
leave uses `GroupMembershipChangePayload(reason = MEMBER_LEFT, memberSigningPublicKey)` so every
remaining recipient can associate the snapshot removal with the exact previous-epoch identity.
When `membershipChange` is absent, `encodeWelcome()` preserves the original welcome signing
payload for compatibility. Its deterministic packet ID is
`GroupSecurityManager.welcomePacketId(groupId, invitationId, epoch)`, so the signature also binds
the welcome to that recipient's current invitation without adding another serialized field.

`GroupReadyAcknowledgementPacket` is created only after `GroupCreatedPacketHandler` has unwrapped
and persisted the epoch key. Its signature binds the group, epoch, deterministic welcome packet ID,
and SHA-256 key-confirmation value. The creator verifies both the signature and the confirmation
against its copy of the epoch key before changing that member to `ACTIVE`.

`GroupMemberRemovedPacket` is owner-signed and bound to the original invitation ID and challenge.
Epoch `0` cancels an invitation that never became active. Removing an active member carries the
next epoch and the removed member's signing key; the recipient verifies both against its installed
group state when present, deletes its local group keys/security snapshot, and retains the
conversation as read-only. The same epoch-advancing packet may be applied from `JOIN_SENT` when it
overtakes the welcome; no installed key is required because the signed invitation binding and
removed signing identity still authenticate the operation. `reason` defaults to
`REMOVED_BY_OWNER`; the owner uses `MEMBER_LEFT` after a valid `GroupLeaveRequestPacket`, and that
reason is included in the member-left signature domain.

`GroupConversationDeletedPacket` is created separately for each non-terminal invitation. Its
signature binds the recipient-specific invitation ID and challenge, group ID, last owner epoch,
and deletion timestamp through `GroupProtocolPayloadEncoder.encodeConversationDeleted()`.
`GroupConversationDeletedPacketHandler` therefore accepts deletion only from the owner stored on
that exact invitation. It deletes local group keys, participants, and verification state but keeps
the conversation and all visible messages. The invitation becomes `GROUP_DELETED`, which maps to
the read-only `GroupConversationState.DELETED` banner.

`GroupChatMessagePacket` never carries message plaintext. `GroupProtocolPayloadEncoder` defines:

- AEAD associated data: protocol version, group ID, epoch, message ID, and timestamp;
- sender-signature data: the associated data plus nonce and ciphertext.

The packet intentionally has no trusted sender phone number or sender public key. The receiver
uses `IncomingPacketContext.contactId` and `GroupMemberKeyEntity` to select the expected Ed25519
key. A packet from a non-member, a stale/future epoch, or a reused `messageId` is rejected.

`GroupCreatedPacket` requires encrypted outer transport through
`DefaultOutgoingPacketTransportPolicy`.
`GroupChatMessagePacket` does not depend on the outer transport for confidentiality because the
inner content is already authenticated group ciphertext. Invitation packets may also use plaintext
outer transport because signatures protect them and neither carries secret material.

## Incoming dispatch

`ProtocolPacketHandler` is the dispatch contract. `DefaultProtocolPacketHandler` receives all
Koin-registered `TypedProtocolPacketHandler` implementations and selects the first handler whose
`canHandle(packet)` returns true.

Each handler receives:

```kotlin
data class IncomingPacketContext(
    val contactId: String,
    val conversationId: String,
    val encodedTransportPayload: String,
    val transportMode: String,
    val receivedAtEpochMilliseconds: Long,
)
```

`transportMode` is a string so `:core:protocol` remains independent of `:core:crypto`.

Handler ownership follows packet meaning:

- chat and receipt handlers are registered by `chatsModule`;
- identity handlers are registered by `contactsModule`;
- `protocolModule` only supplies codec and generic dispatch.

## Outbox contracts

`:core:protocol` defines the persistent send abstraction without implementing storage:

- `ProtocolOutbox`
- `ProtocolOutboxItem`
- `OutboxStatus`
- `OutboxEvent`
- `OutboxStateMachine`
- `OutboxProcessor`
- `OutboxRunner`
- `OutboxDeliveryStateListener`

`DefaultProtocolOutbox` in `:data:database` implements persistence.
`DefaultOutboxProcessor` and `DefaultOutboxRunner` in `:feature:messaging` implement orchestration.

This split lets protocol packets be queued without depending on Room, contacts, crypto, or
WebSockets.

## Transport ports

`OutgoingWireSender` is the outgoing boundary:

```kotlin
interface OutgoingWireSender {
    suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String,
    ): Result<Unit>
}
```

The production implementation is `WebSocketOutgoingWireSender` in `:feature:transport`.

`IncomingMessageHandler` is the incoming application boundary. It receives an already resolved
contact ID, encoded transport payload, and the local encryption key pair. The production
implementation is `IncomingMessageProcessor` in `:feature:chats`.

## Versioning rules

When changing an existing packet:

- adding or removing a field affects strict decoding because `ignoreUnknownKeys` is `false`;
- default values affect what old/new implementations can decode;
- changing a `@SerialName` changes the wire discriminator;
- changing field meaning without changing the protocol version is a compatibility break.

Treat the protocol as an external API even while only one client implementation exists. Define the
migration and compatibility policy before changing an existing serialized shape.

## Adding a packet

1. Add a `@Serializable` class implementing `SecureChatPacket`.
2. Add a unique `@SerialName`.
3. Validate required fields in `init` where appropriate.
4. Add round-trip and invalid-input coverage to the matching codec test, such as
   `GroupInvitationPacketCodecTest`.
5. Implement `TypedProtocolPacketHandler` in the owning feature.
6. Bind it with `bind<TypedProtocolPacketHandler>()` in that feature’s Koin module.
7. Create and enqueue it through `ProtocolOutbox`; do not send directly.
8. Ensure its handler is idempotent under repeated delivery.
9. Document any special encryption requirement.

See the detailed [extension checklist](../features/message-transport-flow.md#how-to-add-a-protocol-packet).
