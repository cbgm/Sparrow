package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupAvatarDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The avatar image lives in a file next to its version in the database. [writeMutex] keeps the version check, the
 * file and the row of one save together, so a concurrent older save can neither win nor leave its image behind.
 */
internal class GroupAvatarDataSource(
    private val groupMetadataDao: GroupMetadataDao,
    private val fileStore: GroupAvatarFileStore
) {
    private val writeMutex = Mutex()

    suspend fun get(groupId: String): GroupAvatarDto {
        val metadata = groupMetadataDao.get(groupId)
        return GroupAvatarDto(
            bytes = fileStore.read(groupId),
            changedAtEpochMilliseconds = metadata?.avatarChangedAtEpochMilliseconds ?: 0L,
            publishPending = metadata?.avatarPublishPending ?: false
        )
    }

    suspend fun saveIfVersion(
        groupId: String,
        avatar: GroupAvatarDto,
        expectedChangedAtEpochMilliseconds: Long
    ): Boolean =
        writeMutex.withLock {
            val storedChangedAt = groupMetadataDao.get(groupId)?.avatarChangedAtEpochMilliseconds ?: 0L
            if (storedChangedAt != expectedChangedAtEpochMilliseconds) return@withLock false
            val bytes = avatar.bytes
            if (bytes == null) fileStore.delete(groupId) else fileStore.write(groupId, bytes)
            groupMetadataDao.upsertAvatar(
                groupId = groupId,
                hasAvatar = bytes != null,
                changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds,
                publishPending = avatar.publishPending
            )
            true
        }

    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long) {
        groupMetadataDao.markAvatarPublished(groupId, changedAtEpochMilliseconds)
    }

    suspend fun findPublishPendingGroupIds(): List<String> = groupMetadataDao.findAvatarPublishPendingGroupIds()

    suspend fun deleteLocal(groupId: String) {
        writeMutex.withLock {
            fileStore.delete(groupId)
            groupMetadataDao.clearAvatar(groupId)
        }
    }
}
