package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupVerificationDao {
    @Query(
        "SELECT MAX(updatedAtEpochMilliseconds) FROM group_verification_pairs WHERE groupId = :groupId"
    )
    suspend fun findLatestUpdatedAt(groupId: String): Long?

    @Query(
        """
        SELECT *
        FROM group_verification_pairs
        WHERE groupId = :groupId
        ORDER BY displayName COLLATE NOCASE, invitationId
        """
    )
    fun observeByGroupId(groupId: String): Flow<List<GroupVerificationPairEntity>>

    @Query(
        """
        SELECT *
        FROM group_verification_pairs
        WHERE groupId = :groupId
        ORDER BY displayName COLLATE NOCASE, invitationId
        """
    )
    suspend fun findByGroupId(groupId: String): List<GroupVerificationPairEntity>

    @Query(
        """
        SELECT *
        FROM group_verification_pairs
        WHERE groupId = :groupId
          AND invitationId = :invitationId
        LIMIT 1
        """
    )
    suspend fun findPair(
        groupId: String,
        invitationId: String
    ): GroupVerificationPairEntity?

    @Upsert
    suspend fun upsertAll(rows: List<GroupVerificationPairEntity>)

    @Query("DELETE FROM group_verification_pairs WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Transaction
    suspend fun replaceGroup(
        groupId: String,
        rows: List<GroupVerificationPairEntity>
    ) {
        deleteByGroupId(groupId)
        if (rows.isNotEmpty()) {
            upsertAll(rows)
        }
    }

    @Query(
        """
        UPDATE group_verification_pairs
        SET adminVerifiedParticipant = 1,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE groupId = :groupId
          AND invitationId = :invitationId
          AND membershipStatus = 'ACTIVE'
        """
    )
    suspend fun markAdminVerifiedParticipant(
        groupId: String,
        invitationId: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE group_verification_pairs
        SET participantVerifiedAdmin = 1,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE groupId = :groupId
          AND invitationId = :invitationId
          AND membershipStatus = 'ACTIVE'
        """
    )
    suspend fun markParticipantVerifiedAdmin(
        groupId: String,
        invitationId: String,
        updatedAt: Long
    ): Int
}
