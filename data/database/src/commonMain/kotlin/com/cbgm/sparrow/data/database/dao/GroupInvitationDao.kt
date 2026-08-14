package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupInvitationDao {
    @Upsert
    suspend fun upsert(invitation: GroupInvitationEntity)

    @Upsert
    suspend fun upsertAll(invitations: List<GroupInvitationEntity>)

    @Query("SELECT * FROM group_invitations WHERE invitationId = :invitationId LIMIT 1")
    suspend fun findByInvitationId(invitationId: String): GroupInvitationEntity?

    @Query(
        """
        SELECT *
        FROM group_invitations
        WHERE groupId = :groupId
          AND contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findByGroupAndContact(
        groupId: String,
        contactId: String
    ): GroupInvitationEntity?

    @Query(
        """
        SELECT *
        FROM group_invitations
        WHERE groupId = :groupId
          AND contactId = :contactId
          AND direction = :direction
        ORDER BY createdAtEpochMilliseconds DESC, invitationId DESC
        LIMIT 1
        """
    )
    suspend fun findByGroupContactAndDirection(
        groupId: String,
        contactId: String,
        direction: String
    ): GroupInvitationEntity?

    @Query(
        """
        SELECT *
        FROM group_invitations
        WHERE groupId = :groupId
        ORDER BY createdAtEpochMilliseconds, invitationId
        """
    )
    suspend fun findByGroupId(groupId: String): List<GroupInvitationEntity>

    @Query(
        """
        DELETE FROM group_invitations
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
        DELETE FROM group_invitations
        WHERE groupId = :groupId
          AND contactId = :contactId
          AND direction = :direction
        """
    )
    suspend fun deleteByGroupContactAndDirection(
        groupId: String,
        contactId: String,
        direction: String
    )

    @Query("DELETE FROM group_invitations WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Transaction
    suspend fun replaceForGroupAndContact(invitation: GroupInvitationEntity) {
        deleteByGroupAndContact(
            groupId = invitation.groupId,
            contactId = invitation.contactId
        )
        upsert(invitation)
    }

    @Query(
        """
        SELECT *
        FROM group_invitations
        WHERE groupId = :groupId
        ORDER BY createdAtEpochMilliseconds, invitationId
        """
    )
    fun observeByGroupId(groupId: String): Flow<List<GroupInvitationEntity>>

    @Query(
        """
        UPDATE group_invitations
        SET status = :newStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt)
        WHERE invitationId = :invitationId
          AND status = :expectedStatus
        """
    )
    suspend fun updateStatus(
        invitationId: String,
        expectedStatus: String,
        newStatus: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE group_invitations
        SET status = :failedStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt)
        WHERE contactId = :contactId
          AND invitationId != :currentInvitationId
          AND status = :awaitingAcceptanceStatus
        """
    )
    suspend fun failSupersededIncomingInvitations(
        contactId: String,
        currentInvitationId: String,
        awaitingAcceptanceStatus: String,
        failedStatus: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE group_invitations
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
