package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMembershipDao {
    @Upsert
    suspend fun upsert(membership: GroupMembershipEntity)

    @Upsert
    suspend fun upsertAll(memberships: List<GroupMembershipEntity>)

    @Query("SELECT * FROM group_memberships WHERE membershipId = :membershipId LIMIT 1")
    suspend fun findByMembershipId(membershipId: String): GroupMembershipEntity?

    @Query("SELECT * FROM group_memberships WHERE sourceInvitationId = :invitationId LIMIT 1")
    suspend fun findBySourceInvitationId(invitationId: String): GroupMembershipEntity?

    @Query(
        """
        SELECT *
        FROM group_memberships
        WHERE groupId = :groupId
          AND contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findByGroupAndContact(
        groupId: String,
        contactId: String
    ): GroupMembershipEntity?

    @Query(
        """
        SELECT *
        FROM group_memberships
        WHERE groupId = :groupId
          AND contactId = :contactId
          AND perspective = :perspective
        ORDER BY createdAtEpochMilliseconds DESC, membershipId DESC
        LIMIT 1
        """
    )
    suspend fun findByGroupContactAndPerspective(
        groupId: String,
        contactId: String,
        perspective: String
    ): GroupMembershipEntity?

    @Query(
        """
        SELECT *
        FROM group_memberships
        WHERE groupId = :groupId
        ORDER BY createdAtEpochMilliseconds, membershipId
        """
    )
    suspend fun findByGroupId(groupId: String): List<GroupMembershipEntity>

    @Query(
        """
        DELETE FROM group_memberships
        WHERE groupId = :groupId
          AND contactId = :contactId
        """
    )
    suspend fun deleteByGroupAndContact(
        groupId: String,
        contactId: String
    )

    @Query(
        """
        DELETE FROM group_memberships
        WHERE groupId = :groupId
          AND contactId = :contactId
          AND perspective = :perspective
        """
    )
    suspend fun deleteByGroupContactAndPerspective(
        groupId: String,
        contactId: String,
        perspective: String
    )

    @Query("DELETE FROM group_memberships WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Transaction
    suspend fun replaceForGroupAndContact(membership: GroupMembershipEntity) {
        deleteByGroupAndContact(
            groupId = membership.groupId,
            contactId = membership.contactId
        )
        upsert(membership)
    }

    @Query(
        """
        SELECT *
        FROM group_memberships
        WHERE groupId = :groupId
        ORDER BY createdAtEpochMilliseconds, membershipId
        """
    )
    fun observeByGroupId(groupId: String): Flow<List<GroupMembershipEntity>>

    @Query(
        """
        SELECT *
        FROM group_memberships
        WHERE perspective = :perspective
        ORDER BY createdAtEpochMilliseconds, membershipId
        """
    )
    fun observeByPerspective(perspective: String): Flow<List<GroupMembershipEntity>>

    @Query(
        """
        SELECT *
        FROM group_memberships
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC, membershipId DESC
        """
    )
    fun observeAll(): Flow<List<GroupMembershipEntity>>

    @Query("DELETE FROM group_memberships WHERE membershipId = :membershipId")
    suspend fun deleteByMembershipId(membershipId: String): Int

    @Query("DELETE FROM group_memberships WHERE sourceInvitationId = :invitationId")
    suspend fun deleteBySourceInvitationId(invitationId: String): Int

    @Query(
        """
        UPDATE group_memberships
        SET status = :newStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt)
        WHERE membershipId = :membershipId
          AND status = :expectedStatus
        """
    )
    suspend fun updateStatus(
        membershipId: String,
        expectedStatus: String,
        newStatus: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        DELETE FROM group_memberships
        WHERE contactId = :contactId
          AND sourceInvitationId != :currentInvitationId
          AND perspective = :perspective
          AND status = :stagedStatus
        """
    )
    suspend fun deleteSupersededStagedMemberships(
        contactId: String,
        currentInvitationId: String,
        perspective: String,
        stagedStatus: String
    ): Int

    @Query(
        """
        UPDATE group_memberships
        SET status = :activeStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt)
        WHERE groupId = :groupId
          AND status = :readyStatus
        """
    )
    suspend fun markGroupActive(
        groupId: String,
        readyStatus: String,
        activeStatus: String,
        updatedAt: Long
    ): Int
}
