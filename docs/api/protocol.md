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

## Message attachments

`ChatMessagePacket` and `GroupMessageContent` can carry attachment metadata alongside text. The current attachment types are:

- `IMAGE`
- `VIDEO`
- `FILE`
- `LOCATION`
- `CONTACT`

A message may contain at most 8 attachments and attachment IDs must be unique within the message. The packet carries metadata plus an `EncryptedBlobReference`; raw attachment bytes are uploaded/downloaded through the blob transport rather than embedded directly into the chat packet.

Location and contact are currently structured payloads stored in the same encrypted blob attachment system, using Sparrow-specific MIME types. This keeps the protocol extensible if those attachment payloads gain richer data later.

## Codec boundary

`PacketCodec` encodes/decodes `SparrowPacket`. Transport encryption is a separate layer. This separation allows the outbox to persist protocol packets before the final recipient routing/encryption/wire step.

## Server protocol

`server:protocol` is a separate server-facing contract module containing gateway, envelope, node and presence HTTP models. Do not merge it with `:core:protocol`: one describes client application packets; the other describes server infrastructure APIs.
