package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import kotlinx.coroutines.flow.Flow

internal class GroupMembershipStoreDataSource(
    private val dao: GroupMembershipDao
) {
    fun observeAll(): Flow<List<GroupMembershipEntity>> = dao.observeAll()

    fun observeByGroupId(groupId: String): Flow<List<GroupMembershipEntity>> =
        dao.observeByGroupId(groupId)

    suspend fun findBySourceInvitationId(invitationId: String): GroupMembershipEntity? =
        dao.findBySourceInvitationId(invitationId)

    suspend fun findByGroupAndContact(
        groupId: String,
        contactId: String
    ): GroupMembershipEntity? = dao.findByGroupAndContact(groupId, contactId)

    suspend fun findByGroupContactAndPerspective(
        groupId: String,
        contactId: String,
        perspective: String
    ): GroupMembershipEntity? =
        dao.findByGroupContactAndPerspective(groupId, contactId, perspective)

    suspend fun findByGroupId(groupId: String): List<GroupMembershipEntity> =
        dao.findByGroupId(groupId)

    suspend fun replaceForGroupAndContact(membership: GroupMembershipEntity) =
        dao.replaceForGroupAndContact(membership)

    suspend fun deleteByGroupId(groupId: String) = dao.deleteByGroupId(groupId)

    suspend fun deleteBySourceInvitationId(invitationId: String): Int =
        dao.deleteBySourceInvitationId(invitationId)

    suspend fun deleteSupersededStagedMemberships(
        contactId: String,
        currentInvitationId: String,
        perspective: String,
        stagedStatus: String
    ): Int =
        dao.deleteSupersededStagedMemberships(
            contactId = contactId,
            currentInvitationId = currentInvitationId,
            perspective = perspective,
            stagedStatus = stagedStatus
        )

    suspend fun updateStatus(
        membershipId: String,
        expectedStatus: String,
        newStatus: String,
        updatedAt: Long
    ): Int =
        dao.updateStatus(
            membershipId = membershipId,
            expectedStatus = expectedStatus,
            newStatus = newStatus,
            updatedAt = updatedAt
        )
}
