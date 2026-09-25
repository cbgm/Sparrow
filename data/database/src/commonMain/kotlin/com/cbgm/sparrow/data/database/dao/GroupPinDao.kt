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

    @Query("DELETE FROM group_pins WHERE groupId = :groupId")
    suspend fun delete(groupId: String)
}
