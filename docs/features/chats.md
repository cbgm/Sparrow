# Chats

`:feature:chats` owns Direct and Group conversation presentation and read-only data access (read repositories, observe use cases). Every write is submitted as a `ConversationCommand` through `ConversationOrchestrator` (`orchestrator.awaitResult(command)`) and executed by a workflow in `:feature:conversationorchestration:*`; see [Conversation orchestration](../architecture/conversation-orchestration.md). Shared message content is represented by the `:core:base` `MessagePart` hierarchy; encrypted blob transport/storage is delegated to `:feature:attachments`.

## Shared message content

Current user-message parts are `Text`, `Image`, `Video`, `File`, `Voice`, `Location`, `Contact` and `Poll`. Direct and Group messages consume the same shared types, but business rules remain separate.

## Direct

`DirectConversationViewModel` and `DirectConversationActionsController` present one-to-one chats and submit `DirectConversationCommand`s. The Direct workflows in `:feature:conversationorchestration:message` (backed by `DirectMessageRepositoryImpl`) own one-to-one write semantics. Edit/delete/reaction are sent through `OperationMessage`. Incoming operations run through `ApplyRemoteMessageOperationWorkflow`, which validates sender/conversation ownership and rejects `PollVote` / `PollClose` because polls are group-only.

Read side: `ObserveDirectChatContextUseCase`, `ObserveDirectConversationUseCase`, `ObserveConversationQueueAvailabilityUseCase`. Write workflows: `SendDirectMessageWorkflow` (send or queue until authorized), `DeliverDirectMessageWorkflow`, `RetryDirectMessageWorkflow`, `EditDirectMessageWorkflow`, `DeleteDirectMessageWorkflow`, `ToggleDirectReactionWorkflow` and `MarkDirectConversationReadWorkflow`.

## Group

`GroupConversationViewModel` and its controllers (`GroupConversationActionsController`, `GroupPollController`, `GroupPinController`) present group chats and submit `GroupMessageCommand`s / `GroupAdministrationCommand`s. The Group workflows in `:feature:conversationorchestration:message` (backed by `GroupMessageRepositoryImpl`) own group write semantics; incoming group packets run through `ReceiveGroupMessageWorkflow` and the other `ReceiveGroup*` workflows. Membership/security comes from `:feature:membership`.

Read side: `ObserveGroupChatContextUseCase`, `ObserveGroupConversationUseCase`, `ObserveGroupDetailsContextUseCase`. Write workflows: `SendGroupMessageWorkflow`, `RetryGroupMessageWorkflow`, `EditGroupMessageWorkflow`, `DeleteGroupMessageWorkflow`, `ToggleGroupReactionWorkflow`, `VoteGroupPollWorkflow`, `CloseGroupPollWorkflow`, `PinGroupMessageWorkflow`, `UnpinGroupMessageWorkflow`, `MarkGroupConversationReadWorkflow` and `SendGroupIndicatorWorkflow`.

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

`CreatePollViewModel` publishes a finished `Poll` through `PollComposerRepository`. `GroupPollController` collects `ObserveFinishedPollUseCase`, sends the poll through the normal attachment-only group send path (`GroupMessageCommand.SendMessage`), cleans temporary media after success and calls `ClearFinishedPollUseCase`.

Voting and manual closing are group operations. Expiry is evaluated by `PollPolicy.isClosedAt(now)` and does not create a background close operation.

## Pinned messages

One current group pin is synchronized by `PinGroupMessageWorkflow` / `UnpinGroupMessageWorkflow` (outgoing) and `ReceiveGroupPinWorkflow` (incoming); Chats reads it through `GroupPinReadRepository`. `GroupPinnedMessage` can render ordinary message content and interactive polls. Detached pinned images reuse the generic attachment/blob loading path.

## History/forwarding

Forwarding is the `ForwardMessage` command (target: Direct conversation, group conversation or contact), executed by `ForwardMessageWorkflow`, which reuses the Direct/Group send workflows. History pagination (`LoadOlderMessagesUseCase`) is a separate read path, independent from transport delivery.

See [Chats architecture](../architecture/chats.md), [Polls](polls.md), [Pinned messages](pinned-messages.md) and [Group membership](group-membership.md).
