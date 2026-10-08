# Chats

`:feature:chats` owns Direct and Group conversation/message semantics and presentation. Shared message content is represented by the `:core:base` `MessagePart` hierarchy; encrypted blob transport/storage is delegated to `:feature:attachments`.

## Shared message content

Current user-message parts are `Text`, `Image`, `Video`, `File`, `Voice`, `Location`, `Contact` and `Poll`. Direct and Group messages consume the same shared types, but business rules remain separate.

## Direct

`DirectConversationViewModel`, `DirectMessageRepositoryImpl`, `DirectOutgoingMessageProcessor` and Direct incoming handlers own one-to-one semantics. Edit/delete/reaction are sent through `OperationMessage`. `DirectOperationMessagePacketHandler` validates sender/conversation ownership and rejects `PollVote` / `PollClose` because polls are group-only.

Important use cases include `ObserveDirectChatContextUseCase`, `SendDirectMessageUseCase`, `SendOrQueueDirectMessageUseCase`, `RetryDirectMessageUseCase`, `EditDirectMessageUseCase`, `DeleteDirectMessageUseCase`, `ToggleDirectMessageReactionUseCase` and `MarkDirectConversationReadUseCase`.

## Group

`GroupConversationViewModel`, `GroupMessageRepositoryImpl`, `GroupOutgoingMessageProcessor` and Group incoming handlers own group semantics. Membership/security comes from `:feature:membership`.

Important use cases include `ObserveGroupChatContextUseCase`, `SendGroupMessageUseCase`, `RetryGroupMessageUseCase`, `EditGroupMessageUseCase`, `DeleteGroupMessageUseCase`, `ToggleGroupMessageReactionUseCase`, `VoteInGroupPollUseCase`, `CloseGroupPollUseCase`, `PinGroupMessageUseCase`, `UnpinGroupMessageUseCase` and read/indicator use cases.

## General operations

```text
OperationMessage
└── MessageOperation
    ├── Edit(messageId, text, editedAt)
    ├── Delete(messageId, deletedAt)
    ├── Reaction(messageId, emoji, removed)
    ├── PollVote(messageId, pollId, selectedOptionIds)
    └── PollClose(messageId, pollId, closedAt)
```

Operations mutate an existing message; they are not user-message content and do not need their own domain message ID beyond the packet/envelope transport ID.

## Poll sending

`CreatePollViewModel` publishes a finished `Poll` through `PollComposerRepository`. `GroupConversationViewModel` collects `ObserveFinishedPollUseCase`, sends the poll through the normal attachment-only group send path, cleans temporary media after success and calls `ClearFinishedPollUseCase`.

Voting and manual closing are group operations. Expiry is evaluated by `PollPolicy.isClosedAt(now)` and does not create a background close operation.

## Pinned messages

One current group pin is synchronized through `GroupPinRepositoryImpl`/`GroupPinBroadcaster`. `GroupPinnedMessage` can render ordinary message content and interactive polls. Detached pinned images reuse the generic attachment/blob loading path.

## History/forwarding

Forwarding remains a dedicated domain path (`PrepareForwardMessageUseCase`, `ForwardMessageUseCase`, Direct/contact/group forwarding implementations and `LoadOlderMessagesUseCase`). History pagination is separate from transport delivery.

See [Chats architecture](../architecture/chats.md), [Polls](polls.md), [Pinned messages](pinned-messages.md) and [Group membership](group-membership.md).
