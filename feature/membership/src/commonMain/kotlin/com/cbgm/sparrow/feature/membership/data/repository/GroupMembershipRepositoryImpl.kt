package com.cbgm.sparrow.feature.membership.data.repository

import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.mapper.toDomain
import com.cbgm.sparrow.feature.membership.data.mapper.toMembershipResult
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

internal class GroupMembershipRepositoryImpl(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val operations: GroupMembershipLifecycleDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource
) : GroupMembershipRepository {
    override fun observeMembershipResults(): Flow<List<MembershipResult>> =
        membershipStore
            .observeAll()
            .map { memberships -> memberships.map { membership -> membership.toMembershipResult() } }
            .distinctUntilChanged()

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
        contactId: String
    ): Result<Unit> = operations.removeMember(groupId, contactId)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = operations.promoteMember(groupId, contactId)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = operations.transferAdminAndLeave(groupId, contactId)

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        operations.getLeaveRequirement(groupId).map { requirement -> requirement.toDomain() }

    override suspend fun leave(groupId: String): Result<Unit> =
        operations.leaveGroup(groupId)

    override suspend fun delete(groupId: String): Result<Unit> =
        operations.deleteGroupConversation(groupId)
}
