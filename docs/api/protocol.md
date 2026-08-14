# Application protocol

`:core:protocol` defines transport-independent application packet types and codecs. The server routes opaque encoded/encrypted payloads rather than importing client packet semantics.

## Important packet families

Direct/contact:

- `ChatMessagePacket`
- `ContactInvitePacket`
- `ContactInviteAcceptedPacket`
- `ContactInviteDeclinedPacket`
- `ContactReadyPacket`
- `ContactVerificationReceiptPacket`
- `DirectChatAuthorizationRevokedPacket`
- `DeliveryReceiptPacket`
- `ReadReceiptPacket`
- `IdentityPacket`
- `IdentityAcknowledgementPacket`
- `MailboxRoutePacket`

Group:

- `GroupChatMessagePacket`
- `GroupCreatedPacket`
- `GroupConversationDeletedPacket`
- `GroupInvitePacket`
- `GroupInviteReceivedPacket`
- `GroupJoinRequestPacket`
- `GroupInviteDeclinedPacket`
- `GroupLeaveRequestPacket`
- `GroupMemberActivatedPacket`
- `GroupMemberActivationAcknowledgementPacket`
- `GroupMemberRemovedPacket`
- `GroupReadyAcknowledgementPacket`
- `GroupVerificationReceiptPacket`
- `GroupVerificationSnapshotRequestPacket`
- `GroupVerificationSnapshotPacket`

## Codec boundary

`PacketCodec` encodes/decodes `SparrowPacket`. Transport encryption is a separate layer. This separation allows the outbox to persist protocol packets before the final recipient routing/encryption/wire step.

## Server protocol

`server:protocol` is a separate server-facing contract module containing gateway, envelope, node and presence HTTP models. Do not merge it with `:core:protocol`: one describes client application packets; the other describes server infrastructure APIs.
