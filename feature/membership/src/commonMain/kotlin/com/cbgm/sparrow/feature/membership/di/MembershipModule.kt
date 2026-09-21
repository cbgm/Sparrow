package com.cbgm.sparrow.feature.membership.di

import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.datasource.GroupEpochDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupEpochSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingActivationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingDeletionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingRemovalDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingWelcomeDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupLeaveDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMemberPromotionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMemberRemovalDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipActivationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAdministrationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipDeletionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupOwnerWelcomeDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.membership.data.datasource.GroupReadyAcknowledgementDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPayloadEncoder
import com.cbgm.sparrow.feature.membership.data.repository.GroupMembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.data.repository.MembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.data.security.GroupSecurityManager
import com.cbgm.sparrow.feature.membership.data.security.GroupWelcomeSecurity
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ApplyIncomingGroupActivationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupActivationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupDeletionUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ClearMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupDeletionUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ConfirmGroupMembershipIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeleteGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DiscardSupersededMembershipsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupCurrentEpochUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupPinSenderSigningKeyUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupTransportRoutingMembersUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.MarkMembershipRemovedUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.OpenIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PersistIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupActivationAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupLeaveRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipDeclineUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipJoinRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipReceiptUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupReadyAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ResolveGroupTransportEncryptionPublicKeyUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.SendGroupReadyAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.StartGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.VerifyGroupKeyConfirmationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val membershipModule =
    module {
        singleOf(::GroupMembershipLock)
        singleOf(::GroupMembershipPayloadEncoder)
        single {
            GroupMembershipPacketProtocol(
                groupCrypto = get(),
                payloadEncoder = get(),
                localProfilePictureMetadataProvider = get()
            )
        }
        singleOf(::GroupMembershipStoreDataSource)
        singleOf(::GroupIncomingWelcomeDataSource)
        singleOf(::GroupReadyAcknowledgementDataSource)
        singleOf(::GroupIncomingDeletionDataSource)
        singleOf(::GroupIncomingRemovalDataSource)
        singleOf(::GroupPacketBroadcaster)
        singleOf(::GroupSecurityStoreDataSource)
        singleOf(::GroupEpochDataSource)
        singleOf(::GroupEpochSecurityDataSource)
        singleOf(::GroupWelcomeSecurity)
        singleOf(::GroupSecurityManager) { bind<GroupSecurityRepository>() }
        singleOf(::VerifyGroupKeyConfirmationUseCase)
        singleOf(::GroupOwnerWelcomeDataSource)
        singleOf(::GroupMembershipActivationDataSource)
        singleOf(::GroupIncomingActivationDataSource)
        singleOf(::GroupMemberPromotionDataSource)
        singleOf(::GroupMemberRemovalDataSource)
        singleOf(::GroupLeaveDataSource)
        singleOf(::GroupMembershipAdministrationDataSource)
        singleOf(::GroupMembershipDeletionDataSource)
        single {
            GroupMembershipLifecycleDataSource(
                activationProvider = { get() },
                administrationProvider = { get() },
                deletionProvider = { get() }
            )
        }

        singleOf(::GroupMembershipRepositoryImpl) {
            bind<GroupMembershipRepository>()
        }
        singleOf(::MembershipRepositoryImpl) {
            bind<MembershipRepository>()
        }

        singleOf(::OpenIncomingGroupWelcomeUseCase)
        singleOf(::PersistIncomingGroupWelcomeUseCase)
        singleOf(::AuthorizeIncomingGroupWelcomeUseCase)
        singleOf(::CompleteIncomingGroupWelcomeUseCase)
        singleOf(::SendGroupReadyAcknowledgementUseCase)
        singleOf(::AuthorizeIncomingGroupRemovalUseCase)
        singleOf(::AuthorizeIncomingGroupActivationUseCase)
        singleOf(::ApplyIncomingGroupActivationUseCase)
        singleOf(::CompleteIncomingGroupRemovalUseCase)
        singleOf(::AuthorizeIncomingGroupDeletionUseCase)
        singleOf(::CompleteIncomingGroupDeletionUseCase)
        singleOf(::RemoveGroupMemberUseCase)
        singleOf(::PromoteGroupMemberUseCase)
        singleOf(::TransferGroupAdminAndLeaveUseCase)
        singleOf(::GetGroupLeaveRequirementUseCase)
        singleOf(::LeaveGroupUseCase)
        singleOf(::ObserveGroupAdministrationUseCase)
        singleOf(::ObserveMembershipResultsUseCase)
        singleOf(::ReceiveGroupReadyAcknowledgementUseCase)
        singleOf(::ReceiveGroupActivationAcknowledgementUseCase)
        singleOf(::ReceiveGroupLeaveRequestUseCase)
        singleOf(::StartGroupMembershipUseCase)
        singleOf(::InspectIncomingGroupMembershipUseCase)
        singleOf(::ReceiveIncomingGroupMembershipUseCase)
        singleOf(::DiscardSupersededMembershipsUseCase)
        singleOf(::GetMembershipHandshakeUseCase)
        singleOf(::AcceptGroupMembershipUseCase)
        singleOf(::AuthorizeGroupMetadataUseCase)
        singleOf(::WasGroupMembershipDeletedUseCase)
        singleOf(::GetGroupMessageMembershipAccessUseCase)
        singleOf(::GetGroupPinSenderSigningKeyUseCase)
        singleOf(::GetGroupCurrentEpochUseCase)
        singleOf(::ResolveGroupTransportEncryptionPublicKeyUseCase)
        singleOf(::GetGroupTransportRoutingMembersUseCase)
        singleOf(::DeclineGroupMembershipUseCase)
        singleOf(::DeleteGroupMembershipUseCase)
        singleOf(::ReceiveGroupMembershipReceiptUseCase)
        singleOf(::ReceiveGroupMembershipDeclineUseCase)
        singleOf(::ReceiveGroupMembershipJoinRequestUseCase)
        singleOf(::ConfirmGroupMembershipIdentityUseCase)
        singleOf(::ClearMembershipHandshakeUseCase)
        singleOf(::MarkMembershipRemovedUseCase)
    }
