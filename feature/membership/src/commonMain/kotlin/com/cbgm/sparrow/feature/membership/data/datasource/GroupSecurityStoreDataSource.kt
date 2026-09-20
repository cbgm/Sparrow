package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import kotlinx.coroutines.flow.Flow

internal class GroupSecurityStoreDataSource(
    private val dao: GroupSecurityDao
) {
    suspend fun findState(groupId: String): GroupSecurityStateEntity? = dao.findState(groupId)

    /** Membership's authorization state is read from Membership's own storage, never Chats. */
    suspend fun findLocalRole(groupId: String): String? = findState(groupId)?.localRole

    suspend fun findCurrentEpoch(groupId: String): Int? = findState(groupId)?.currentEpoch

    suspend fun findOwnedGroupEpoch(groupId: String): Int? =
        findState(groupId)?.let { state ->
            check(state.localRole.isGroupAdminRole()) {
                "Only a group admin may change group membership"
            }
            state.currentEpoch
        }

    suspend fun findCurrentRemoteMemberKey(groupId: String, contactId: String): GroupMemberKeyEntity? {
        val state = findState(groupId) ?: return null
        return findMemberKey(groupId, state.currentEpoch, contactId)
    }

    suspend fun retireLocalMembership(groupId: String, retiredAtEpochMilliseconds: Long) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(retiredAtEpochMilliseconds >= 0L) { "Retirement timestamp must not be negative" }
        val state = dao.findState(groupId) ?: return
        check(
            dao.updateLocalRole(
                groupId = groupId,
                role = GROUP_LEFT_ROLE,
                updatedAtEpochMilliseconds = maxOf(state.updatedAtEpochMilliseconds, retiredAtEpochMilliseconds)
            ) == 1
        ) { "Group security state disappeared while local membership was retired" }
    }

    suspend fun replaceCurrentEpoch(
        state: GroupSecurityStateEntity,
        memberKeys: List<GroupMemberKeyEntity>
    ) {
        dao.replaceCurrentEpoch(state, memberKeys)
    }

    suspend fun upsertMemberKeys(keys: List<GroupMemberKeyEntity>) {
        dao.upsertMemberKeys(keys)
    }

    suspend fun upsertMemberKey(key: GroupMemberKeyEntity) {
        dao.upsertMemberKeys(listOf(key))
    }

    suspend fun deleteGroup(groupId: String) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        dao.deleteGroup(groupId)
    }

    suspend fun findMemberKeys(
        groupId: String,
        epoch: Int
    ): List<GroupMemberKeyEntity> = dao.findMemberKeys(groupId, epoch)

    suspend fun findMemberKey(
        groupId: String,
        epoch: Int,
        contactId: String
    ): GroupMemberKeyEntity? = dao.findMemberKey(groupId, epoch, contactId)

    fun observeState(groupId: String): Flow<GroupSecurityStateEntity?> = dao.observeState(groupId)

    fun observeCurrentMemberKeys(groupId: String): Flow<List<GroupMemberKeyEntity>> =
        dao.observeCurrentMemberKeys(groupId)
}
