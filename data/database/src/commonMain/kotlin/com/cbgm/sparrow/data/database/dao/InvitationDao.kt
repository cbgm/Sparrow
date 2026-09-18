package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.InvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvitationDao {
    @Upsert
    suspend fun upsert(invitation: InvitationEntity)

    @Upsert
    suspend fun upsertAll(invitations: List<InvitationEntity>)

    @Query("SELECT * FROM invitations WHERE invitationId = :invitationId LIMIT 1")
    suspend fun findById(invitationId: String): InvitationEntity?

    @Query(
        """
        SELECT *
        FROM invitations
        WHERE payloadType = :payloadType
          AND payloadId = :payloadId
          AND peerId = :peerId
          AND direction = :direction
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC, invitationId DESC
        LIMIT 1
        """
    )
    suspend fun findLatest(
        payloadType: String,
        payloadId: String,
        peerId: String,
        direction: String
    ): InvitationEntity?

    @Query(
        """
        SELECT *
        FROM invitations
        WHERE payloadType = :payloadType
          AND direction = :direction
          AND hiddenAtEpochMilliseconds IS NULL
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC, invitationId DESC
        """
    )
    fun observeByPayloadTypeAndDirection(
        payloadType: String,
        direction: String
    ): Flow<List<InvitationEntity>>

    @Query(
        """
        SELECT *
        FROM invitations
        WHERE payloadType = :payloadType
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC, invitationId DESC
        """
    )
    fun observeByPayloadType(payloadType: String): Flow<List<InvitationEntity>>

    @Query(
        """
        SELECT *
        FROM invitations
        WHERE direction = :direction
          AND hiddenAtEpochMilliseconds IS NULL
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC, invitationId DESC
        """
    )
    fun observeByDirection(direction: String): Flow<List<InvitationEntity>>

    @Query(
        """
        SELECT *
        FROM invitations
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC, invitationId DESC
        """
    )
    fun observeAll(): Flow<List<InvitationEntity>>

    @Query("DELETE FROM invitations WHERE invitationId = :invitationId")
    suspend fun deleteById(invitationId: String): Int

    @Query(
        """
        DELETE FROM invitations
        WHERE payloadType = :payloadType
          AND payloadId = :payloadId
          AND peerId = :peerId
          AND direction = :direction
        """
    )
    suspend fun deleteByPayloadPeerAndDirection(
        payloadType: String,
        payloadId: String,
        peerId: String,
        direction: String
    )

    @Query(
        """
        SELECT *
        FROM invitations
        WHERE payloadType = :payloadType
          AND payloadId = :payloadId
          AND peerId = :peerId
          AND direction = :direction
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatest(
        payloadType: String,
        payloadId: String,
        peerId: String,
        direction: String
    ): Flow<InvitationEntity?>

    @Query(
        """
        UPDATE invitations
        SET status = :newStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt),
            resultAction = :resultAction
        WHERE invitationId = :invitationId
          AND status = :expectedStatus
        """
    )
    suspend fun updateStatus(
        invitationId: String,
        expectedStatus: String,
        newStatus: String,
        updatedAt: Long,
        resultAction: String? = null
    ): Int

    @Query(
        """
        UPDATE invitations
        SET status = :failedStatus,
            updatedAtEpochMilliseconds = MAX(createdAtEpochMilliseconds, :updatedAt)
        WHERE payloadType = :payloadType
          AND payloadId = :payloadId
          AND peerId = :peerId
          AND invitationId != :currentInvitationId
          AND direction = :direction
          AND status = :pendingStatus
        """
    )
    suspend fun failSuperseded(
        payloadType: String,
        payloadId: String,
        peerId: String,
        currentInvitationId: String,
        direction: String,
        pendingStatus: String,
        failedStatus: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE invitations
        SET viewedAtEpochMilliseconds = :viewedAtEpochMilliseconds
        WHERE payloadType = :payloadType
          AND direction = :direction
          AND hiddenAtEpochMilliseconds IS NULL
        """
    )
    suspend fun markDirectionViewed(
        payloadType: String,
        direction: String,
        viewedAtEpochMilliseconds: Long
    )

    @Query(
        """
        UPDATE invitations
        SET hiddenAtEpochMilliseconds = :hiddenAtEpochMilliseconds
        WHERE invitationId = :invitationId
        """
    )
    suspend fun hideById(
        invitationId: String,
        hiddenAtEpochMilliseconds: Long
    ): Int
}
