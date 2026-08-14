# Chats architecture: Direct and Group red line

Direct and Group conversations are intentionally separate feature paths. They share only infrastructure whose **meaning and lifecycle are actually shared**.

This is the central architecture guide to read before changing `:feature:chats`.

## Red line

```mermaid
flowchart TB
    IN[IncomingPacketProcessor] --> ROUTER[IncomingPacketRouter]

    ROUTER --> DIRECT[DirectIncomingPacketProcessor]
    ROUTER --> GROUP[GroupIncomingPacketProcessor]

    DIRECT --> DSTORE[DirectConversationStorage]
    DIRECT --> DDEL[DirectMessageDeliveryCoordinator]

    GROUP --> REG[GroupPacketHandlerRegistry]
    REG --> GH[explicit GroupPacketHandler]
    GH --> GMEM[Group membership/security/verification/message code]
```

Do not make Direct use Group repositories/state machines or Group use Direct repositories/state machines to “reuse” similarly named operations.

## Direct conversation path

Incoming:

```text
IncomingPacketProcessor
  -> IncomingPacketRouter
  -> DirectIncomingPacketProcessor
  -> DirectMessagePacketHandler / DirectReceiptPacketHandler
  -> DirectConversationStorage / DirectMessageDeliveryCoordinator
```

Outgoing:

```text
DirectViewModel
  -> direct *UseCase
  -> DirectMessageRepository
  -> DirectMessageRepositoryImpl
  -> DirectOutgoingMessageProcessor
  -> ProtocolOutbox
```

Delivery:

```text
DirectOutboxDeliveryHandler
  -> DirectMessageDeliveryCoordinator
  -> DirectMessageDeliveryStateMachine
```

Typing:

```text
ObserveDirectTypingUseCase / SetDirectTypingUseCase
  -> DirectTypingRepository
  -> DirectTypingRepositoryImpl
```

## Group conversation path

Incoming:

```text
IncomingPacketProcessor
  -> IncomingPacketRouter
  -> GroupIncomingPacketProcessor
  -> GroupPacketHandlerRegistry
  -> one explicit GroupPacketHandler
```

Handlers include the concrete group packet types such as `GroupInvitePacketHandler`, `GroupJoinRequestPacketHandler`, `GroupMemberActivatedPacketHandler`, `GroupMemberRemovedPacketHandler`, `GroupChatMessagePacketHandler`, verification snapshot handlers and `GroupReceiptPacketHandler`.

Outgoing:

```text
GroupViewModel
  -> group *UseCase
  -> GroupMessageRepository
  -> GroupMessageRepositoryImpl
  -> GroupOutgoingMessageProcessor
  -> ProtocolOutbox
```

Delivery:

```text
GroupOutboxDeliveryHandler
  -> GroupMessageDeliveryCoordinator
  -> GroupMessageDeliveryStateMachine
```

Typing:

```text
ObserveGroupMemberTypingUseCase / SetGroupTypingUseCase
  -> GroupTypingRepository
  -> GroupTypingRepositoryImpl
```

## Group membership lifecycle

`GroupMembershipCoordinator` is a small entry facade. Mutating membership behavior is split into focused coordinators:

- `GroupInvitationCoordinator` — creation, invitations, accept/decline and join requests;
- `GroupMembershipActivationCoordinator` — welcome/key distribution, ready/activation acknowledgements;
- `GroupMembershipAdministrationCoordinator` — promote, remove, leave and admin transfer;
- `GroupMembershipDeletionCoordinator` — local/remote group deletion lifecycle;
- `GroupMembershipIdentity` — contact identity pinning/acceptance used by membership flows;
- `GroupEpochCoordinator` — current member/role resolution and epoch payload construction;
- `GroupMembershipStateMachine` — explicit lifecycle transition rules;
- `GroupMembershipPacketProtocol` — group membership packet creation/verification/encoding.

All mutating membership coordinators share `GroupMembershipLock`, so splitting responsibilities does not split the serialization boundary.

```mermaid
classDiagram
    class GroupMembershipCoordinator
    class GroupInvitationCoordinator
    class GroupMembershipActivationCoordinator
    class GroupMembershipAdministrationCoordinator
    class GroupMembershipDeletionCoordinator
    class GroupEpochCoordinator
    class GroupMembershipIdentity
    class GroupMembershipStateMachine
    class GroupMembershipPacketProtocol
    class GroupMembershipLock

    GroupMembershipCoordinator --> GroupInvitationCoordinator
    GroupMembershipCoordinator --> GroupMembershipActivationCoordinator
    GroupMembershipCoordinator --> GroupMembershipAdministrationCoordinator
    GroupMembershipCoordinator --> GroupMembershipDeletionCoordinator
    GroupInvitationCoordinator --> GroupMembershipLock
    GroupMembershipActivationCoordinator --> GroupMembershipLock
    GroupMembershipAdministrationCoordinator --> GroupMembershipLock
    GroupMembershipDeletionCoordinator --> GroupMembershipLock
    GroupMembershipCoordinator --> GroupEpochCoordinator
    GroupMembershipCoordinator --> GroupMembershipIdentity
    GroupMembershipCoordinator --> GroupMembershipStateMachine
    GroupMembershipCoordinator --> GroupMembershipPacketProtocol
```

There is **no orphaned-group state/mode** in the current code.

## Group security and history boundary

`GroupOutgoingMessageProcessor` selects recipients from the current group security epoch, encrypts the group message through `GroupSecurityManager`, stores one recipient delivery row per active recipient and queues one packet per active recipient.

A removed member is not kept in the recipient set merely because that contact was previously in the group. Re-invitation starts a new active membership period; old absence-period messages are not supposed to become ordinary backlog for that member.

## Shared edges

These are shared because the protocol/infrastructure is actually shared:

- `IncomingPacketProcessor` — transport/protocol decode boundary;
- `IncomingPacketRouter` — packet dispatch;
- `ReceiptIncomingPacketRouter` — shared receipt-format dispatch;
- `ChatOutboxDeliveryStateRouter` — shared outbox callback dispatch;
- `ConversationOverviewRepositoryImpl` — combines Direct/Group projections for the overview screen.

Shared routers contain dispatch only. Conversation-specific rules belong under `data/direct` or `data/group`.

## Package rules

- `domain/model/direct` and `domain/model/group` — conversation-specific domain state/models/state machines;
- `domain/repository/direct|group` — repository contracts only;
- `domain/usecase/direct|group` — one-purpose use cases;
- `data/**/repository` — repository implementations only;
- `data/**/incoming` — packet processors/routing;
- `data/**/incoming/handler` — explicit packet handlers;
- `data/**/outgoing` — outgoing orchestration;
- `data/**/delivery` — delivery callbacks/coordinators;
- `data/**/mapper` — mappings;
- `data/**/storage` — focused persistence helpers that are not repositories;
- `data/group/membership` — group membership coordinators/state machine/epoch helpers;
- `data/group/protocol` — group membership protocol construction/verification;
- `data/group/security` — group cryptographic state/operations;
- `data/group/verification` — verification synchronization.

## Presentation

Direct presentation lives under the Direct screen path; Group presentation lives under the Group screen path. Shared Compose elements should be shared only if both conversation types genuinely render/use the same thing.

Previews remain in the same Kotlin file as the composable being previewed.
