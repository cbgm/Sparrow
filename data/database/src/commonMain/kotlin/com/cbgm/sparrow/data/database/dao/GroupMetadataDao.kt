package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.GroupMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMetadataDao {
    @Query("SELECT * FROM group_metadata WHERE groupId = :groupId LIMIT 1")
    fun observe(groupId: String): Flow<GroupMetadataEntity?>

    @Query("SELECT * FROM group_metadata WHERE groupId = :groupId LIMIT 1")
    suspend fun get(groupId: String): GroupMetadataEntity?

    @Upsert
    suspend fun upsert(entity: GroupMetadataEntity)

    @Transaction
    suspend fun upsertTitleValue(groupId: String, title: String) {
        upsert(current(groupId).copy(title = title))
    }

    @Transaction
    suspend fun upsertTitle(groupId: String, title: String, changedAtEpochMilliseconds: Long) {
        upsert(
            current(groupId).copy(
                title = title,
                titleChangedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        )
    }

    @Transaction
    suspend fun upsertDescription(groupId: String, description: String?, changedAtEpochMilliseconds: Long) {
        upsert(
            current(groupId).copy(
                description = description,
                descriptionChangedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        )
    }

    @Transaction
    suspend fun upsertAvatar(groupId: String, hasAvatar: Boolean, changedAtEpochMilliseconds: Long) {
        upsert(
            current(groupId).copy(
                hasAvatar = hasAvatar,
                avatarChangedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
        )
    }

    @Query("UPDATE group_metadata SET titleChangedAtEpochMilliseconds = 0 WHERE groupId = :groupId")
    suspend fun clearTitleChangedAt(groupId: String)

    @Query(
        """
        UPDATE group_metadata
        SET description = NULL, descriptionChangedAtEpochMilliseconds = 0
        WHERE groupId = :groupId
        """
    )
    suspend fun clearDescription(groupId: String)

    @Query(
        """
        UPDATE group_metadata
        SET hasAvatar = 0, avatarChangedAtEpochMilliseconds = 0
        WHERE groupId = :groupId
        """
    )
    suspend fun clearAvatar(groupId: String)

    private suspend fun current(groupId: String): GroupMetadataEntity =
        get(groupId) ?: GroupMetadataEntity(groupId = groupId)
}
