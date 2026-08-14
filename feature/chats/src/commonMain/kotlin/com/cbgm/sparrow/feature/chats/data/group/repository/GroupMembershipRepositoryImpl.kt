package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

class GroupMembershipRepositoryImpl(
    private val groupSecurityDao: GroupSecurityDao,
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupMembershipRepository {
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

    override suspend fun create(
        title: String,
        contactIds: Set<String>
    ): Result<String> = membershipCoordinator.createGroup(title, contactIds)

    override suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> = membershipCoordinator.addMembers(groupId, contactIds)

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

    override suspend fun acceptInvitation(groupId: String): Result<Unit> =
        membershipCoordinator.acceptInvitation(groupId)

    override suspend fun declineInvitation(groupId: String): Result<Unit> =
        membershipCoordinator.declineInvitation(groupId)
}
