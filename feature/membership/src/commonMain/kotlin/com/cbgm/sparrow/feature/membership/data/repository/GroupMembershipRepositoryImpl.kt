package com.cbgm.sparrow.feature.membership.data.repository

import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.mapper.toDomain
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

internal class GroupMembershipRepositoryImpl(
    private val securityStore: GroupSecurityStoreDataSource,
    private val operations: GroupMembershipLifecycleDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource
) : GroupMembershipRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> =
        combine(
            securityStore.observeState(groupId),
            securityStore.observeCurrentMemberKeys(groupId)
        ) { securityState, memberKeys ->
            securityState to memberKeys
        }.transformLatest { (securityState, memberKeys) ->
            if (securityState == null || securityState.localRole == GROUP_LEFT_ROLE) {
                emit(GroupAdministrationState())
                return@transformLatest
            }

            val currentMembers = memberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
            val currentAdmins =
                memberKeys
                    .filter { memberKey -> memberKey.role.isGroupAdminRole() }
                    .mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }

            val localIsAdmin = securityState.localRole.isGroupAdminRole()
            val leaveRequirement =
                GroupMembershipStateMachine.leaveRequirement(
                    isLocalAdmin = localIsAdmin,
                    currentMemberContactIds = currentMembers,
                    currentAdminContactIds = currentAdmins
                )
            emit(
                GroupAdministrationState(
                    isLocalAdmin = localIsAdmin,
                    adminContactIds = currentAdmins,
                    currentMemberContactIds = currentMembers,
                    promotableContactIds =
                        currentMembers.filterTo(mutableSetOf()) { contactId ->
                            contactId !in currentAdmins
                        },
                    requiresPromotionBeforeLeave =
                        leaveRequirement is GroupLeaveRequirementDto.PromoteAdminFirst,
                    activeMemberCount = currentMembers.size + 1
                )
            )
        }

    override suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        membershipAttempts.initializeOwnedGroup(groupId)

    override suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = operations.removeMember(groupId, contactId, context)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> = operations.promoteMember(groupId, contactId, context)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<Unit> = operations.transferAdminAndLeave(groupId, contactId, context)

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val securityState = securityStore.findState(groupId)
            if (securityState == null || !securityState.localRole.isGroupAdminRole()) {
                return@runCatching GroupLeaveRequirement.CanLeave
            }

            val memberKeys =
                securityStore.findMemberKeys(
                    groupId = groupId,
                    epoch = securityState.currentEpoch
                )
            val currentMembers = memberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
            val currentAdmins =
                memberKeys
                    .filter { memberKey -> memberKey.role.isGroupAdminRole() }
                    .mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }

            GroupMembershipStateMachine
                .leaveRequirement(
                    isLocalAdmin = true,
                    currentMemberContactIds = currentMembers,
                    currentAdminContactIds = currentAdmins
                ).toDomain()
        }

    override suspend fun leave(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Unit> = operations.leaveGroup(groupId, context)

    override suspend fun delete(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Unit> = operations.deleteGroupConversation(groupId, context)
}
