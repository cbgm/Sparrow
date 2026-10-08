# Application protocol

`:protocol` defines client application packets/codecs. Server infrastructure contracts remain in `:server:protocol`.

## User-message content

`GroupMessageContent` carries:

```kotlin
val parts: List<MessagePartDto>
val replyToMessageId: String?
```

It validates wire message parts with polls allowed and is encoded with `sparrow-group-message-v2:`. Legacy group plaintext is decoded as a text part for compatibility.

The shared serialized part hierarchy includes `TEXT`, `IMAGE`, `VIDEO`, `FILE`, `VOICE`, `LOCATION`, `CONTACT` and `POLL`. Binary parts carry `EncryptedBlobReferenceDto`; raw bytes are transferred through the blob service rather than embedded in packets. Poll structured fields are serialized in `PollDto`, including nested image descriptors.

## Operations on existing messages

`OperationMessage` wraps one sealed `MessageOperation` and is encoded with `sparrow-operation-message-v1:`.

- `EDIT` — message ID, replacement text, edit timestamp
- `DELETE` — message ID, deletion timestamp
- `REACTION` — message ID, emoji, removed flag
- `POLL_VOTE` — message ID, poll ID, selected option IDs
- `POLL_CLOSE` — message ID, poll ID, close timestamp

Direct incoming processing accepts Edit/Delete/Reaction and rejects poll operations. Group processing supports the full set.

## Packet boundary

`OperationMessagePacket` carries the encoded operation message. User-message packets and operation packets remain distinct because operations mutate existing persisted messages rather than adding content to history.

`PacketCodec` is independent from transport encryption. The durable protocol outbox can therefore persist packets before final recipient routing/encryption/transmission.

## Other packet families

Identity/invitation/direct/group membership, delivery/read receipts, group pin updates, verification and mailbox-route packet families remain part of the same application protocol. See the generated [`:protocol` module reference](../generated/modules/protocol.md) for the current declaration inventory.
