package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupAvatarDto

internal class GroupAvatarDataSource(
    private val groupMetadataDao: GroupMetadataDao,
    private val fileStore: GroupAvatarFileStore
) {
    suspend fun get(groupId: String): GroupAvatarDto {
        val metadata = groupMetadataDao.get(groupId)
        return GroupAvatarDto(
            bytes = fileStore.read(groupId),
            changedAtEpochMilliseconds = metadata?.avatarChangedAtEpochMilliseconds ?: 0L,
            publishPending = metadata?.avatarPublishPending ?: false
        )
    }

    suspend fun save(groupId: String, avatar: GroupAvatarDto) {
        val bytes = avatar.bytes
        if (bytes == null) fileStore.delete(groupId) else fileStore.write(groupId, bytes)
        groupMetadataDao.upsertAvatar(
            groupId = groupId,
            hasAvatar = bytes != null,
            changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds,
            publishPending = avatar.publishPending
        )
    }

    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long) {
        groupMetadataDao.markAvatarPublished(groupId, changedAtEpochMilliseconds)
    }

    suspend fun findPublishPendingGroupIds(): List<String> = groupMetadataDao.findAvatarPublishPendingGroupIds()

    suspend fun deleteLocal(groupId: String) {
        fileStore.delete(groupId)
        groupMetadataDao.clearAvatar(groupId)
    }
}
