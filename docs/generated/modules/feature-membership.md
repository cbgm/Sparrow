# `:feature:membership`

Source directory: `feature/membership`

## Direct project dependencies

- `:core`
- `:core:crypto`
- `:core:protocol`
- `:data:database`
- `:data:datastore`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidGroupKeyStore` | `class` | `androidMain` | `feature/membership/src/androidMain/kotlin/com/cbgm/sparrow/feature/membership/device/AndroidGroupKeyStore.kt` |
| `GroupMembershipEvent` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupMembershipLock` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipLock.kt` |
| `GroupMembershipStateMachine` | `object` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/GroupMembershipStateMachine.kt` |
| `GroupEpochDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochDataSource.kt` |
| `GroupEpochSecurityDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupEpochSecurityDataSource.kt` |
| `GroupIncomingActivationDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingActivationDataSource.kt` |
| `GroupIncomingDeletionDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingDeletionDataSource.kt` |
| `GroupIncomingRemovalDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingRemovalDataSource.kt` |
| `GroupIncomingWelcomeDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupIncomingWelcomeDataSource.kt` |
| `GroupLeaveDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupLeaveDataSource.kt` |
| `GroupMemberPromotionDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberPromotionDataSource.kt` |
| `GroupMemberRemovalDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMemberRemovalDataSource.kt` |
| `GroupMembershipActivationDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipActivationDataSource.kt` |
| `GroupMembershipAdministrationDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipAdministrationDataSource.kt` |
| `GroupMembershipDeletionDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipDeletionDataSource.kt` |
| `GroupMembershipLifecycleDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipLifecycleDataSource.kt` |
| `GroupMembershipStoreDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupMembershipStoreDataSource.kt` |
| `GroupOwnerWelcomeDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupOwnerWelcomeDataSource.kt` |
| `GroupPacketBroadcaster` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupPacketBroadcaster.kt` |
| `GroupReadyAcknowledgementDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupReadyAcknowledgementDataSource.kt` |
| `GroupSecurityStoreDataSource` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/datasource/GroupSecurityStoreDataSource.kt` |
| `CreatedGroupSecurityDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupConversationStateDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupInvitationDirection` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupInvitationStatus` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupInvitationStatus.kt` |
| `GroupLeaveRequirementDto` | `interface` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupLocalMembershipEndDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupLocalMembershipEndDto.kt` |
| `GroupMemberProgressDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMemberProgressStatusDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipLifecycleDtos.kt` |
| `GroupMembershipParticipantDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipParticipantDto.kt` |
| `GroupMembershipPeerDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipPeerDto.kt` |
| `GroupMembershipPerspective` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `GroupMembershipStatus` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipStatus.kt` |
| `GroupWelcomeRecipientDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/model/GroupMembershipSecurityDto.kt` |
| `GroupMembershipPacketProtocol` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPacketProtocol.kt` |
| `GroupMembershipPayloadEncoder` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/protocol/GroupMembershipPayloadEncoder.kt` |
| `GroupMembershipRepositoryImpl` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/GroupMembershipRepositoryImpl.kt` |
| `MembershipRepositoryImpl` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/repository/MembershipRepositoryImpl.kt` |
| `GroupSecurityManager` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupSecurityManager.kt` |
| `GroupWelcomeSecurity` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/data/security/GroupWelcomeSecurity.kt` |
| `GroupAdministrationState` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupAdministrationState.kt` |
| `GroupConversationMembershipProjection` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipProjector` | `object` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipProjection.kt` |
| `GroupConversationMembershipSnapshot` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupConversationState` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationState.kt` |
| `GroupIncomingWelcomeAuthorization` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupIncomingWelcomeAuthorization.kt` |
| `GroupLeaveRequirement` | `interface` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLeaveRequirement.kt` |
| `GroupLocalMembershipEnd` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupLocalMembershipEnd.kt` |
| `GroupMemberLifecycleSnapshot` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupConversationMembershipSnapshot.kt` |
| `GroupMemberProgress` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMemberProgressStatus` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberProgress.kt` |
| `GroupMemberPromotionResult` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalReason` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMemberRemovalResult` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMemberAdministrationResult.kt` |
| `GroupMembershipContext` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMembershipContext.kt` |
| `GroupMessageMembershipAccess` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMessageMembershipAccess.kt` |
| `GroupMetadataMessageSender` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataMessageSender.kt` |
| `GroupMetadataSendContext` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupMetadataSecurityContext.kt` |
| `GroupTransportRoutingMember` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupTransportRoutingMember.kt` |
| `GroupVerificationMemberKey` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembership` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationMembershipContext` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupVerificationSecurityState` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupVerificationMembershipContext.kt` |
| `GroupWelcomeMemberKey` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupWelcomeMemberKey.kt` |
| `IncomingMembershipOffer` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineDisposition` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipDeclineResult` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipHandshake` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipJoinRequest` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipPerspective` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipResult` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipResult.kt` |
| `MembershipSigningProof` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `MembershipStatus` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipStatus.kt` |
| `MembershipVerificationSnapshot` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipVerificationSnapshot.kt` |
| `OpenedGroupWelcomeDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `OpenedIncomingGroupWelcome` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/OpenedIncomingGroupWelcome.kt` |
| `SecuredGroupMessageDto` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/GroupSecurityModels.kt` |
| `StartedMembershipHandshake` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/model/MembershipHandshake.kt` |
| `GroupMembershipRepository` | `interface` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupMembershipRepository.kt` |
| `GroupSecurityRepository` | `interface` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/GroupSecurityRepository.kt` |
| `MembershipRepository` | `interface` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/repository/MembershipRepository.kt` |
| `AcceptGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AcceptGroupMembershipUseCase.kt` |
| `ApplyIncomingGroupActivationUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ApplyIncomingGroupActivationUseCase.kt` |
| `AuthorizeGroupMetadataUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeGroupMetadataUseCase.kt` |
| `AuthorizeIncomingGroupActivationUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupActivationUseCase.kt` |
| `AuthorizeIncomingGroupDeletionUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupDeletionUseCase.kt` |
| `AuthorizeIncomingGroupRemovalUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupRemovalUseCase.kt` |
| `AuthorizeIncomingGroupWelcomeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/AuthorizeIncomingGroupWelcomeUseCase.kt` |
| `ClearMembershipHandshakeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ClearMembershipHandshakeUseCase.kt` |
| `CompleteIncomingGroupDeletionUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupDeletionUseCase.kt` |
| `CompleteIncomingGroupRemovalUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupRemovalUseCase.kt` |
| `CompleteIncomingGroupWelcomeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/CompleteIncomingGroupWelcomeUseCase.kt` |
| `ConfirmGroupMembershipIdentityUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ConfirmGroupMembershipIdentityUseCase.kt` |
| `DeclineGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeclineGroupMembershipUseCase.kt` |
| `DeleteGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DeleteGroupMembershipUseCase.kt` |
| `DiscardSupersededMembershipsUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/DiscardSupersededMembershipsUseCase.kt` |
| `GetGroupCurrentEpochUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupCurrentEpochUseCase.kt` |
| `GetGroupLeaveRequirementUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupLeaveRequirementUseCase.kt` |
| `GetGroupMessageMembershipAccessUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupMessageMembershipAccessUseCase.kt` |
| `GetGroupPinSenderSigningKeyUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupPinSenderSigningKeyUseCase.kt` |
| `GetGroupTransportRoutingMembersUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetGroupTransportRoutingMembersUseCase.kt` |
| `GetMembershipHandshakeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/GetMembershipHandshakeUseCase.kt` |
| `InspectIncomingGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/InspectIncomingGroupMembershipUseCase.kt` |
| `LeaveGroupUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/LeaveGroupUseCase.kt` |
| `MarkMembershipRemovedUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/MarkMembershipRemovedUseCase.kt` |
| `ObserveGroupAdministrationUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveGroupAdministrationUseCase.kt` |
| `ObserveMembershipResultsUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ObserveMembershipResultsUseCase.kt` |
| `OpenIncomingGroupWelcomeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/OpenIncomingGroupWelcomeUseCase.kt` |
| `PersistIncomingGroupWelcomeUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PersistIncomingGroupWelcomeUseCase.kt` |
| `PromoteGroupMemberUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/PromoteGroupMemberUseCase.kt` |
| `ReceiveGroupActivationAcknowledgementUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupActivationAcknowledgementUseCase.kt` |
| `ReceiveGroupLeaveRequestUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupLeaveRequestUseCase.kt` |
| `ReceiveGroupMembershipDeclineUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipDeclineUseCase.kt` |
| `ReceiveGroupMembershipJoinRequestUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipJoinRequestUseCase.kt` |
| `ReceiveGroupMembershipReceiptUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupMembershipReceiptUseCase.kt` |
| `ReceiveGroupReadyAcknowledgementUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveGroupReadyAcknowledgementUseCase.kt` |
| `ReceiveIncomingGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ReceiveIncomingGroupMembershipUseCase.kt` |
| `RemoveGroupMemberUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/RemoveGroupMemberUseCase.kt` |
| `ResolveGroupTransportEncryptionPublicKeyUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/ResolveGroupTransportEncryptionPublicKeyUseCase.kt` |
| `SendGroupReadyAcknowledgementUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/SendGroupReadyAcknowledgementUseCase.kt` |
| `StartGroupMembershipUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/StartGroupMembershipUseCase.kt` |
| `TransferGroupAdminAndLeaveUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/TransferGroupAdminAndLeaveUseCase.kt` |
| `VerifyGroupKeyConfirmationUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/VerifyGroupKeyConfirmationUseCase.kt` |
| `WasGroupMembershipDeletedUseCase` | `class` | `commonMain` | `feature/membership/src/commonMain/kotlin/com/cbgm/sparrow/feature/membership/domain/usecase/WasGroupMembershipDeletedUseCase.kt` |
