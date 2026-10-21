package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cbgm.sparrow.data.database.entity.ParkedMessageOperationEntity

@Dao
interface ParkedMessageOperationDao {
    @Insert
    suspend fun insert(operation: ParkedMessageOperationEntity)

    @Query(
        "SELECT * FROM parked_message_operations WHERE targetMessageId = :messageId " +
            "ORDER BY sentAtEpochMilliseconds, id"
    )
    suspend fun findByTarget(messageId: String): List<ParkedMessageOperationEntity>

    @Query("DELETE FROM parked_message_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM parked_message_operations WHERE parkedAtEpochMilliseconds < :cutoffEpochMilliseconds")
    suspend fun deleteParkedBefore(cutoffEpochMilliseconds: Long)
}
