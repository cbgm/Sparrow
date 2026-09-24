package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cbgm.sparrow.data.database.entity.ApprovedIdentityReconnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApprovedIdentityReconnectionDao {
    @Query("SELECT * FROM approved_identity_reconnections ORDER BY approvedAtEpochMilliseconds")
    fun observeAll(): Flow<List<ApprovedIdentityReconnectionEntity>>

    @Query("SELECT * FROM approved_identity_reconnections WHERE peerId = :peerId LIMIT 1")
    suspend fun find(peerId: String): ApprovedIdentityReconnectionEntity?

    @Query("DELETE FROM approved_identity_reconnections WHERE peerId = :peerId AND approvalId = :approvalId")
    suspend fun deleteIfCurrent(peerId: String, approvalId: String): Int
}
