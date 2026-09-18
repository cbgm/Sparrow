package com.cbgm.sparrow.feature.membership.di

import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.datasource.GroupEpochDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupLeaveDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMemberPromotionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMemberRemovalDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipActivationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAdministrationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipDeletionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPayloadEncoder
import com.cbgm.sparrow.feature.membership.data.repository.GroupMembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.data.repository.MembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ClearMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ConfirmGroupMembershipIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeleteGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DiscardSupersededMembershipsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InitializeOwnedGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.MarkMembershipRemovedUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipDeclineUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipJoinRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipReceiptUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.StartGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase
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
        singleOf(::GroupPacketBroadcaster)
        singleOf(::GroupSecurityStoreDataSource)
        singleOf(::GroupEpochDataSource)
        singleOf(::GroupMembershipActivationDataSource)
        singleOf(::GroupMemberPromotionDataSource)
        singleOf(::GroupMemberRemovalDataSource)
        singleOf(::GroupLeaveDataSource)
        singleOf(::GroupMembershipAdministrationDataSource)
        singleOf(::GroupMembershipDeletionDataSource)
        singleOf(::GroupMembershipAttemptDataSource)
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

        singleOf(::RemoveGroupMemberUseCase)
        singleOf(::PromoteGroupMemberUseCase)
        singleOf(::TransferGroupAdminAndLeaveUseCase)
        singleOf(::GetGroupLeaveRequirementUseCase)
        singleOf(::InitializeOwnedGroupMembershipUseCase)
        singleOf(::LeaveGroupUseCase)
        singleOf(::ObserveGroupAdministrationUseCase)
        singleOf(::ObserveMembershipResultsUseCase)
        singleOf(::StartGroupMembershipUseCase)
        singleOf(::InspectIncomingGroupMembershipUseCase)
        singleOf(::ReceiveIncomingGroupMembershipUseCase)
        singleOf(::DiscardSupersededMembershipsUseCase)
        singleOf(::GetMembershipHandshakeUseCase)
        singleOf(::AcceptGroupMembershipUseCase)
        singleOf(::DeclineGroupMembershipUseCase)
        singleOf(::DeleteGroupMembershipUseCase)
        singleOf(::ReceiveGroupMembershipReceiptUseCase)
        singleOf(::ReceiveGroupMembershipDeclineUseCase)
        singleOf(::ReceiveGroupMembershipJoinRequestUseCase)
        singleOf(::ConfirmGroupMembershipIdentityUseCase)
        singleOf(::ClearMembershipHandshakeUseCase)
        singleOf(::MarkMembershipRemovedUseCase)
    }
