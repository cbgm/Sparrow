package com.cbgm.sparrow.feature.membership.data.repository

import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipCoordinator
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

class GroupMembershipRepositoryImpl(
    private val groupSecurityDao: GroupSecurityDao,
    private val membershipCoordinator: GroupMembershipCoordinator,
    private val membershipAttempts: GroupMembershipAttemptDataSource
) : GroupMembershipRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> =
        combine(
            groupSecurityDao.observeState(groupId),
            groupSecurityDao.observeCurrentMemberKeys(groupId)
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
                        leaveRequirement is GroupLeaveRequirement.PromoteAdminFirst,
                    activeMemberCount = currentMembers.size + 1
                )
            )
        }

    override suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        membershipAttempts.initializeOwnedGroup(groupId)

    override suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.removeMember(groupId, contactId)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.promoteMember(groupId, contactId)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.transferAdminAndLeave(groupId, contactId)

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        membershipCoordinator.getLeaveRequirement(groupId)

    override suspend fun leave(groupId: String): Result<Unit> =
        membershipCoordinator.leaveGroup(groupId)

    override suspend fun delete(groupId: String): Result<Unit> =
        membershipCoordinator.deleteGroupConversation(groupId)
}
