# Pinned group messages

Groups support one current pinned user message. Pinning is admin-controlled and persists a snapshot so the pin can be rendered even when the original history page is not loaded.

## Main classes

Write side (`:feature:conversationorchestration:groupstate` / `incoming`):

- `PinGroupMessageWorkflow`, `UnpinGroupMessageWorkflow` (`GroupMessageCommand.PinMessage` / `UnpinMessage`)
- `SendCurrentGroupPinWorkflow`
- `ReceiveGroupPinWorkflow`
- `GroupPinState`, `GroupPinRepository` / `GroupPinRepositoryImpl`, `GroupPinDataSource`, `GroupPinPacketCodec`

Read side (`:feature:chats`):

- `GroupPin`
- `GroupPinReadRepository` / `GroupPinReadRepositoryImpl`, `GroupPinDataSource`
- `GroupPinController`
- `LoadGroupPinnedAttachmentUseCase`
- `GroupPinnedMessage`

## Snapshot model

`PinGroupMessageWorkflow` (`FindPinTarget -> [ResolveSenderKey] -> AuthorizeAdmin -> PreparePin -> SavePin -> PublishPin`) validates that the target is a readable group user message, resolves the sender signing key, builds `GroupMessageContent` from text plus message parts and stores the pin right away as unpublished, so it shows at once. `PublishGroupPinWorkflow` then queues it for the group under the administration lock and marks it published; a pin whose publish failed is published again when it is pinned again or after the next start (`UnpublishedGroupStateRunner`). A stored version never replaces a newer one that arrived meanwhile (`GroupStateVersionPolicy`).

```mermaid
sequenceDiagram
    participant UI as GroupPinController
    participant ORCH as ConversationOrchestrator
    participant WF as PinGroupMessageWorkflow
    participant R as GroupPinRepositoryImpl
    participant OUT as ProtocolOutbox

    UI->>ORCH: awaitResult(GroupMessageCommand.PinMessage)
    ORCH->>WF: FindPinTarget / AuthorizeAdmin / PreparePin
    WF->>OUT: CreatePinPackets / EnqueuePinPackets
    WF->>R: SavePin (GroupPinEntity + encoded GroupMessageContent)
```

`GroupPinEntity` stores the message ID, sent/pinned/change timestamps, sender identity metadata and encoded message content. Unpinning (`UnpinGroupMessageWorkflow`) stores an unpinned state with a monotonically increasing `changedAtEpochMilliseconds` right away as unpublished and publishes it like a pin (`PublishGroupPin`); an unpin that was never published is published again instead of being reported as "nothing pinned". `GroupPinEntity.publishPending` marks a pin slot that is not queued yet.

## Attachments

Pinned attachment loading uses the same message-part/blob infrastructure. Chats' `GroupPinReadRepositoryImpl.loadAttachment(...)` searches top-level snapshot parts and nested `PollDto.images`, then `loadDetachedBytes(...)` resolves the encrypted blob.

The filesystem cache does **not** need separate `writePinned`/`readPinned` APIs; “pinned” is a source context, not a different kind of attachment file.

## Polls

Pinned polls are rendered through the normal `PollMessageContent`. `GroupPinnedMessage` forwards vote and close callbacks with the original message ID and poll ID, so a pin is interactive rather than a static screenshot.

`MessagePartSourceUi.GroupPin(groupId)` gives pinned parts a stable source-aware `instanceKey` (`group-pin:<groupId>:<partId>`) without creating a parallel poll model.

## Live vs snapshot state

The snapshot is required when the original history message is unavailable. When the corresponding message is already present in live conversation state, presentation should prefer that current message so reactions, poll votes and close state reflect ongoing updates rather than remaining frozen at pin time.
