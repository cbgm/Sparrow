# Chats architecture

Direct and Group conversations are separate feature paths. They share only protocol/transport primitives that are genuinely common at the application boundary.

## Direct conversation red line

Incoming:

`IncomingPacketProcessor` → `IncomingPacketRouter` → `DirectIncomingPacketProcessor` → `DirectMessagePacketHandler` → `DirectConversationStorage` / `DirectMessageDeliveryCoordinator`

Outgoing:

`DirectViewModel` → Direct message `*UseCase` → `DirectMessageRepository` → `DirectMessageRepositoryImpl` → `DirectOutgoingMessageProcessor` → `ProtocolOutbox`

Delivery state:

`DirectMessageDeliveryCoordinator` → `DirectMessageDeliveryStateMachine`

Typing:

Direct typing use cases → `DirectTypingRepository` → `DirectTypingRepositoryImpl`

## Group conversation red line

Incoming:

`IncomingPacketProcessor` → `IncomingPacketRouter` → `GroupIncomingPacketProcessor` → `GroupPacketHandlerRegistry` → one explicit `GroupPacketHandler` → membership/security/verification/message responsibility

Membership packets:

`GroupPacketHandler` → `GroupMembershipCoordinator` → one focused membership coordinator → `GroupMembershipStateMachine` / `GroupMembershipPacketProtocol`

`GroupMembershipCoordinator` is intentionally a small facade. It keeps the entry point obvious while the lifecycle is split into:

- `GroupInvitationCoordinator`: group creation, invitations, accept/decline, join requests.
- `GroupMembershipActivationCoordinator`: welcome/key distribution, ready acknowledgements, activation.
- `GroupMembershipAdministrationCoordinator`: promote, remove, leave, admin transfer.
- `GroupMembershipDeletionCoordinator`: local/remote group deletion lifecycle.
- `GroupMembershipIdentity`: contact identity pinning/acceptance used by membership flows.
- `GroupEpochCoordinator`: current member/role resolution and epoch payload construction.

All mutating membership coordinators share one `GroupMembershipLock`, so splitting the code does not split the serialization boundary.

Membership/admin use cases:

Group membership `*UseCase` → `GroupMembershipRepository` → `GroupMembershipRepositoryImpl` → `GroupMembershipCoordinator`

Outgoing messages:

`GroupViewModel` → Group message `*UseCase` → `GroupMessageRepository` → `GroupMessageRepositoryImpl` → `GroupOutgoingMessageProcessor` → `ProtocolOutbox`

Delivery state:

`GroupMessageDeliveryCoordinator` → `GroupMessageDeliveryStateMachine`

Typing:

Group typing use cases → `GroupTypingRepository` → `GroupTypingRepositoryImpl`

## Shared edges

These classes are shared because the wire format or infrastructure is shared, not because Direct and Group behavior is mixed:

- `IncomingPacketProcessor` decodes transport and protocol bytes.
- `IncomingPacketRouter` dispatches decoded packets to Direct, Group, receipt, or non-chat fallback paths.
- `ReceiptIncomingPacketRouter` dispatches the shared receipt packet format to Direct or Group receipt handlers.
- `ChatOutboxDeliveryStateRouter` dispatches shared outbox callbacks to Direct or Group delivery handlers.
- `ConversationOverviewRepositoryImpl` combines Direct/Group rows only for the conversation overview screen.

Shared routers contain dispatch only. Conversation-specific rules belong under `data/direct` or `data/group`.

## Package rules

- `domain/model`: domain state and models, separated into Direct, Group, and Overview where applicable.
- `domain/repository`: repository contracts only; names end in `Repository`. Conversation, message, membership, typing, verification, and key persistence are separate contracts when their responsibilities differ.
- `domain/usecase`: one-purpose use cases; names end in `UseCase`.
- `data/**/repository`: repository implementations only; names end in `RepositoryImpl`.
- `data/**/incoming`: packet processors and routing for that conversation type.
- `data/**/incoming/handler`: explicit packet handlers.
- `data/**/outgoing`: outgoing message orchestration.
- `data/**/mapper`: persistence/protocol-to-domain mappings.
- `data/**/storage`: persistence-focused helpers that are not repositories.
- `data/group/membership`: small membership facade, focused invitation/activation/administration/deletion coordinators, membership state machine, and epoch/identity helpers.
- `data/group/protocol`: group membership packet creation/verification and payload encoding.
- `data/group/security`: group cryptographic state and operations.
- `data/group/verification`: group verification synchronization.

Do not introduce a generic chat abstraction merely because Direct and Group happen to perform similarly named operations. Share code only when the semantics and lifecycle are genuinely the same.
