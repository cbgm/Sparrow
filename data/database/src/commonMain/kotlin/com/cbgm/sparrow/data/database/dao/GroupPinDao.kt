package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupPinDao {
    @Query("SELECT * FROM group_pins WHERE groupId = :groupId LIMIT 1")
    fun observe(groupId: String): Flow<GroupPinEntity?>

    @Query("SELECT * FROM group_pins WHERE groupId = :groupId LIMIT 1")
    suspend fun get(groupId: String): GroupPinEntity?

    @Upsert
    suspend fun upsert(entity: GroupPinEntity)

    /** Marks the pin published only while [changedAtEpochMilliseconds] is still the stored version. */
    @Query(
        """
        UPDATE group_pins SET publishPending = 0
        WHERE groupId = :groupId AND changedAtEpochMilliseconds = :changedAtEpochMilliseconds
        """
    )
    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long)

    @Query("SELECT groupId FROM group_pins WHERE publishPending = 1")
    suspend fun findPublishPendingGroupIds(): List<String>

    @Query("DELETE FROM group_pins WHERE groupId = :groupId")
    suspend fun delete(groupId: String)
}
