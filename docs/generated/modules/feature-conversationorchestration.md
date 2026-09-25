# `:feature:conversationorchestration`

Source directory: `feature/conversationorchestration`

## Direct project dependencies

- `:core`
- `:core:crypto`
- `:core:protocol`
- `:feature:contacts`
- `:feature:identity`
- `:feature:invite`
- `:feature:membership`
- `:feature:messaging`
- `:feature:transport`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `WebSocketIncomingEnvelopeGateway` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/data/datasource/WebSocketIncomingEnvelopeGateway.kt` |
| `DirectChatAuthorizationRequiredException` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/error/DirectChatAuthorizationRequiredException.kt` |
| `RemoteIdentityReplacementRequiredException` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/error/RemoteIdentityReplacementRequiredException.kt` |
| `ConversationMessagePlan` | `interface` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/model/ConversationMessagePlan.kt` |
| `ConversationPort` | `interface` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/port/ConversationPort.kt` |
| `AddConversationMembersUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/AddConversationMembersUseCase.kt` |
| `CreateConversationGroupUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/CreateConversationGroupUseCase.kt` |
| `DeleteConversationGroupUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/DeleteConversationGroupUseCase.kt` |
| `DeletePeerConversationUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/DeletePeerConversationUseCase.kt` |
| `GetConversationGroupLeaveRequirementUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GetConversationGroupLeaveRequirementUseCase.kt` |
| `GroupVerificationInputsUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `LeaveConversationGroupUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/LeaveConversationGroupUseCase.kt` |
| `ObserveConversationIndicatorUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ObserveConversationIndicatorUseCase.kt` |
| `ObserveConversationQueueAvailabilityUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ObserveConversationQueueAvailabilityUseCase.kt` |
| `OwnedGroupVerificationInputs` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `PendingGroupIdentity` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/GroupVerificationInputsUseCase.kt` |
| `PrepareConversationMessageUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PrepareConversationMessageUseCase.kt` |
| `PrepareConversationOpenUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PrepareConversationOpenUseCase.kt` |
| `PromoteConversationGroupMemberUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/PromoteConversationGroupMemberUseCase.kt` |
| `ReconnectExistingConversationUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ReconnectExistingConversationUseCase.kt` |
| `RemoveConversationGroupMemberUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/RemoveConversationGroupMemberUseCase.kt` |
| `RequireDirectChatAuthorizationUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/RequireDirectChatAuthorizationUseCase.kt` |
| `ResolveIncomingIdentityPeerUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ResolveIncomingIdentityPeerUseCase.kt` |
| `ResolveSigningIdentityContactUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/ResolveSigningIdentityContactUseCase.kt` |
| `SendConversationIndicatorUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/SendConversationIndicatorUseCase.kt` |
| `StartRecoveryInvitationUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/StartRecoveryInvitationUseCase.kt` |
| `TransferConversationGroupAdminAndLeaveUseCase` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/usecase/TransferConversationGroupAdminAndLeaveUseCase.kt` |
| `ConversationFlowHandler` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/ConversationFlowHandler.kt` |
| `GroupInvitationIdentityDisposition` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/GroupInvitationIdentityPolicy.kt` |
| `GroupInvitationIdentityPolicy` | `object` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/GroupInvitationIdentityPolicy.kt` |
| `IncomingAuthorizationRevocationDecision` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/domain/workflow/IncomingAuthorizationRevocationDecision.kt` |
| `ApprovedIdentityReconnectionObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ApprovedIdentityReconnectionObserver.kt` |
| `ApprovedReconnectionRetryWorker` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ApprovedReconnectionRetryWorker.kt` |
| `ContactBlockObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/ContactBlockObserver.kt` |
| `GroupMembershipPacketObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/GroupMembershipPacketObserver.kt` |
| `IdentityExchangePacketObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/IdentityExchangePacketObserver.kt` |
| `IdentityResultObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/IdentityResultObserver.kt` |
| `InvitationResultObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/InvitationResultObserver.kt` |
| `MembershipResultObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/MembershipResultObserver.kt` |
| `MessagingTransportResultObserver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/MessagingTransportResultObserver.kt` |
| `DefaultIncomingEnvelopeProcessor` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/incoming/DefaultIncomingEnvelopeProcessor.kt` |
| `WebSocketMessagingIndicatorGateway` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/indicator/WebSocketMessagingIndicatorGateway.kt` |
| `DefaultMailboxCapabilityLifecycle` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/DefaultMailboxCapabilityLifecycle.kt` |
| `DefaultMailboxCoordinator` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/DefaultMailboxCoordinator.kt` |
| `MailboxCredentialFactory` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxCredentialFactory.kt` |
| `MailboxPendingSynchronizer` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxPendingSynchronizer.kt` |
| `MailboxRoutePacketHandler` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxRoutePacketHandler.kt` |
| `MailboxRouteProvisioner` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/mailbox/MailboxRouteProvisioner.kt` |
| `InvitationTransportFailureHandler` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/InvitationTransportFailureHandler.kt` |
| `OutgoingPacketSender` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingPacketSender.kt` |
| `OutgoingPacketTransportPolicy` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingPacketTransportPolicy.kt` |
| `OutgoingRecipientRoutingResolver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingRecipientRoutingResolver.kt` |
| `OutgoingTransportPayloadFactory` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingTransportPayloadFactory.kt` |
| `OutgoingTransportRequirement` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/OutgoingTransportRequirement.kt` |
| `RetryableWireDeliveryException` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/outbox/RetryableWireDeliveryException.kt` |
| `GroupRoutingResolver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/routing/GroupRoutingResolver.kt` |
| `GroupTransportKeyResolver` | `class` | `commonMain` | `feature/conversationorchestration/src/commonMain/kotlin/com/cbgm/sparrow/feature/conversationorchestration/runtime/routing/GroupTransportKeyResolver.kt` |
