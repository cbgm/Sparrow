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
    suspend fun upsertTitle(
        groupId: String,
        title: String,
        changedAtEpochMilliseconds: Long,
        publishPending: Boolean
    ) {
        upsert(
            current(groupId).copy(
                title = title,
                titleChangedAtEpochMilliseconds = changedAtEpochMilliseconds,
                titlePublishPending = publishPending
            )
        )
    }

    @Transaction
    suspend fun upsertDescription(
        groupId: String,
        description: String?,
        changedAtEpochMilliseconds: Long,
        publishPending: Boolean
    ) {
        upsert(
            current(groupId).copy(
                description = description,
                descriptionChangedAtEpochMilliseconds = changedAtEpochMilliseconds,
                descriptionPublishPending = publishPending
            )
        )
    }

    @Transaction
    suspend fun upsertAvatar(
        groupId: String,
        hasAvatar: Boolean,
        changedAtEpochMilliseconds: Long,
        publishPending: Boolean
    ) {
        upsert(
            current(groupId).copy(
                hasAvatar = hasAvatar,
                avatarChangedAtEpochMilliseconds = changedAtEpochMilliseconds,
                avatarPublishPending = publishPending
            )
        )
    }

    /** Marks the title published only while [changedAtEpochMilliseconds] is still the stored version. */
    @Query(
        """
        UPDATE group_metadata SET titlePublishPending = 0
        WHERE groupId = :groupId AND titleChangedAtEpochMilliseconds = :changedAtEpochMilliseconds
        """
    )
    suspend fun markTitlePublished(groupId: String, changedAtEpochMilliseconds: Long)

    /** Marks the description published only while [changedAtEpochMilliseconds] is still the stored version. */
    @Query(
        """
        UPDATE group_metadata SET descriptionPublishPending = 0
        WHERE groupId = :groupId AND descriptionChangedAtEpochMilliseconds = :changedAtEpochMilliseconds
        """
    )
    suspend fun markDescriptionPublished(groupId: String, changedAtEpochMilliseconds: Long)

    /** Marks the avatar published only while [changedAtEpochMilliseconds] is still the stored version. */
    @Query(
        """
        UPDATE group_metadata SET avatarPublishPending = 0
        WHERE groupId = :groupId AND avatarChangedAtEpochMilliseconds = :changedAtEpochMilliseconds
        """
    )
    suspend fun markAvatarPublished(groupId: String, changedAtEpochMilliseconds: Long)

    @Query("SELECT groupId FROM group_metadata WHERE titlePublishPending = 1")
    suspend fun findTitlePublishPendingGroupIds(): List<String>

    @Query("SELECT groupId FROM group_metadata WHERE descriptionPublishPending = 1")
    suspend fun findDescriptionPublishPendingGroupIds(): List<String>

    @Query("SELECT groupId FROM group_metadata WHERE avatarPublishPending = 1")
    suspend fun findAvatarPublishPendingGroupIds(): List<String>

    @Query(
        """
        UPDATE group_metadata SET titleChangedAtEpochMilliseconds = 0, titlePublishPending = 0
        WHERE groupId = :groupId
        """
    )
    suspend fun clearTitleChangedAt(groupId: String)

    @Query(
        """
        UPDATE group_metadata
        SET description = NULL, descriptionChangedAtEpochMilliseconds = 0, descriptionPublishPending = 0
        WHERE groupId = :groupId
        """
    )
    suspend fun clearDescription(groupId: String)

    @Query(
        """
        UPDATE group_metadata
        SET hasAvatar = 0, avatarChangedAtEpochMilliseconds = 0, avatarPublishPending = 0
        WHERE groupId = :groupId
        """
    )
    suspend fun clearAvatar(groupId: String)

    private suspend fun current(groupId: String): GroupMetadataEntity =
        get(groupId) ?: GroupMetadataEntity(groupId = groupId)
}
