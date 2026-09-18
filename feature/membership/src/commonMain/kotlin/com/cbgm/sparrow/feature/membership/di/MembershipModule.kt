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
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.repository.GroupMembershipRepositoryImpl
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InitializeOwnedGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val membershipModule =
    module {
        singleOf(::GroupMembershipLock)
        singleOf(::GroupMembershipStoreDataSource)
        singleOf(::GroupSecurityStoreDataSource)
        singleOf(::GroupEpochDataSource)
        singleOf(::GroupMembershipActivationDataSource)
        singleOf(::GroupMemberPromotionDataSource)
        singleOf(::GroupMemberRemovalDataSource)
        singleOf(::GroupLeaveDataSource)
        singleOf(::GroupMembershipAdministrationDataSource)
        singleOf(::GroupMembershipDeletionDataSource)
        singleOf(::GroupMembershipAttemptDataSource)
        singleOf(::GroupMembershipLifecycleDataSource)

        singleOf(::GroupMembershipRepositoryImpl) {
            bind<GroupMembershipRepository>()
        }

        singleOf(::RemoveGroupMemberUseCase)
        singleOf(::PromoteGroupMemberUseCase)
        singleOf(::TransferGroupAdminAndLeaveUseCase)
        singleOf(::GetGroupLeaveRequirementUseCase)
        singleOf(::InitializeOwnedGroupMembershipUseCase)
        singleOf(::LeaveGroupUseCase)
        singleOf(::ObserveGroupAdministrationUseCase)
        singleOf(::ObserveMembershipResultsUseCase)
    }
