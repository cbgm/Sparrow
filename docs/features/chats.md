# Chats

`:feature:chats` implements Direct and Group conversation/message behavior and presentation. Group membership/security is supplied by `:feature:membership`; invitations and identity exchange are coordinated outside Chats.

## Presentation

Primary screens/view models include:

- `DirectConversationViewModel`
- `GroupConversationViewModel`
- conversation overview presentation
- Group details/verification presentation
- shared message history/bubble/context actions

## Direct messages

Key use cases include:

- `ObserveDirectChatContextUseCase`
- `ObserveDirectConversationUseCase`
- `SendDirectMessageUseCase`
- `SendOrQueueDirectMessageUseCase`
- `QueueDirectMessageUntilAuthorizedUseCase`
- `RetryDirectMessageUseCase`
- `EditDirectMessageUseCase`
- `DeleteDirectMessageUseCase`
- `ToggleDirectMessageReactionUseCase`
- `MarkDirectConversationReadUseCase`
- `SetDirectIndicatorUseCase`, `ObserveDirectIndicatorUseCase`
- `GetOrCreateDirectConversationUseCase`
- `ActivateAuthorizedDirectConversationUseCase`
- `DiscardPendingAuthorizationMessagesUseCase`

The data path is centered on `DirectMessageRepositoryImpl` and `DirectOutgoingMessageProcessor`.

## Group messages

Key use cases include:

- `CreateGroupConversationUseCase`
- `AddGroupMembersUseCase`
- `ObserveGroupChatContextUseCase`
- `ObserveGroupConversationUseCase`
- `SendGroupMessageUseCase`
- `RetryGroupMessageUseCase`
- `EditGroupMessageUseCase`
- `DeleteGroupMessageUseCase`
- `ToggleGroupMessageReactionUseCase`
- `MarkGroupConversationReadUseCase`
- `SetGroupIndicatorUseCase`, `ObserveGroupMemberIndicatorUseCase`
- `SetGroupTitleUseCase`, `SetGroupDescriptionUseCase`, `SetGroupAvatarUseCase`, `RemoveGroupAvatarUseCase`
- `PinGroupMessageUseCase`, `UnpinGroupMessageUseCase`

The outgoing data path is centered on `GroupMessageRepositoryImpl` and `GroupOutgoingMessageProcessor`.

## Forwarding/history

Forwarding is a distinct domain path with `PrepareForwardMessageUseCase`, `ForwardMessageUseCase`, `ForwardDirectMessageUseCase`, `ForwardToContactUseCase`, `ForwardToDirectConversationUseCase`, `ForwardToGroupConversationUseCase` and `LoadOlderMessagesUseCase`.

## Message content

Chats represents text plus attachment-backed content. Attachment transfer/storage is owned by `:feature:attachments`, media selection/rendering by `:feature:media`, and voice recording/playback/transcription by `:feature:voice`.

## Group details and membership

Group details can edit title/description/avatar and administer members, but the underlying membership/role/security operations are domain APIs from `:feature:membership` coordinated through `:feature:conversationorchestration` where cross-feature decisions are required.

See [Chats architecture](../architecture/chats.md), [Group membership](group-membership.md), [Pinned messages](pinned-messages.md), and [Runtime flows](../architecture/runtime-flows.md).
