package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupSecurityDao {
    @Upsert
    suspend fun upsertState(state: GroupSecurityStateEntity)

    @Upsert
    suspend fun upsertMemberKeys(memberKeys: List<GroupMemberKeyEntity>)

    @Transaction
    suspend fun replaceCurrentEpoch(
        state: GroupSecurityStateEntity,
        memberKeys: List<GroupMemberKeyEntity>
    ) {
        upsertState(state)
        upsertMemberKeys(memberKeys)
        deleteEpochsBefore(
            groupId = state.groupId,
            epoch = state.currentEpoch
        )
    }

    @Query("SELECT * FROM group_security_states WHERE groupId = :groupId LIMIT 1")
    suspend fun findState(groupId: String): GroupSecurityStateEntity?

    @Query("SELECT * FROM group_security_states WHERE groupId = :groupId LIMIT 1")
    fun observeState(groupId: String): Flow<GroupSecurityStateEntity?>

    @Query("DELETE FROM group_security_states WHERE groupId = :groupId")
    suspend fun deleteState(groupId: String)

    @Query("DELETE FROM group_member_keys WHERE groupId = :groupId")
    suspend fun deleteMemberKeys(groupId: String)

    @Transaction
    suspend fun deleteGroup(groupId: String) {
        deleteMemberKeys(groupId)
        deleteState(groupId)
    }

    @Query(
        """
        SELECT *
        FROM group_member_keys
        WHERE groupId = :groupId
          AND epoch = :epoch
          AND contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findMemberKey(
        groupId: String,
        epoch: Int,
        contactId: String
    ): GroupMemberKeyEntity?

    @Query(
        """
        DELETE FROM group_member_keys
        WHERE groupId = :groupId
          AND epoch < :epoch
        """
    )
    suspend fun deleteEpochsBefore(
        groupId: String,
        epoch: Int
    )

    @Query(
        """
        SELECT *
        FROM group_member_keys
        WHERE groupId = :groupId
          AND epoch = :epoch
        ORDER BY contactId
        """
    )
    suspend fun findMemberKeys(
        groupId: String,
        epoch: Int
    ): List<GroupMemberKeyEntity>

    @Query(
        """
        SELECT member.*
        FROM group_member_keys AS member
        INNER JOIN group_security_states AS state
            ON state.groupId = member.groupId
           AND state.currentEpoch = member.epoch
        WHERE member.groupId = :groupId
        ORDER BY member.contactId
        """
    )
    fun observeCurrentMemberKeys(groupId: String): Flow<List<GroupMemberKeyEntity>>

    @Query(
        """
        UPDATE group_security_states
        SET localRole = :role,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE groupId = :groupId
        """
    )
    suspend fun updateLocalRole(
        groupId: String,
        role: String,
        updatedAtEpochMilliseconds: Long
    ): Int
}
