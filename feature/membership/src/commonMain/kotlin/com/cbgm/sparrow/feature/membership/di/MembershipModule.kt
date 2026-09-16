package com.cbgm.sparrow.feature.membership.di

import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupEpochCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupInvitationCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupLeaveCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMemberPromotionCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMemberRemovalCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipActivationCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipAdministrationCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipCoordinator
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipDeletionCoordinator
import com.cbgm.sparrow.feature.membership.data.repository.GroupMembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AddGroupMembersUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val membershipModule =
    module {
        singleOf(::GroupMembershipLock)
        singleOf(::GroupMembershipIdentity)
        singleOf(::GroupEpochCoordinator)
        singleOf(::GroupMembershipActivationCoordinator)
        singleOf(::GroupMemberPromotionCoordinator)
        singleOf(::GroupMemberRemovalCoordinator)
        singleOf(::GroupLeaveCoordinator)
        singleOf(::GroupMembershipAdministrationCoordinator)
        singleOf(::GroupMembershipDeletionCoordinator)
        singleOf(::GroupInvitationCoordinator)
        singleOf(::GroupMembershipCoordinator)

        singleOf(::GroupMembershipRepositoryImpl) {
            bind<GroupMembershipRepository>()
        }

        singleOf(::AcceptGroupInvitationUseCase)
        singleOf(::DeclineGroupInvitationUseCase)
        singleOf(::AddGroupMembersUseCase)
        singleOf(::RemoveGroupMemberUseCase)
        singleOf(::PromoteGroupMemberUseCase)
        singleOf(::TransferGroupAdminAndLeaveUseCase)
        singleOf(::GetGroupLeaveRequirementUseCase)
        singleOf(::LeaveGroupUseCase)
        singleOf(::ObserveGroupAdministrationUseCase)
    }
