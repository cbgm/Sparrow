package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupAvatarDto

internal class GroupAvatarDataSource(
    private val groupMetadataDao: GroupMetadataDao,
    private val fileStore: GroupAvatarFileStore
) {
    suspend fun get(groupId: String): GroupAvatarDto =
        GroupAvatarDto(
            bytes = fileStore.read(groupId),
            changedAtEpochMilliseconds = groupMetadataDao.get(groupId)?.avatarChangedAtEpochMilliseconds ?: 0L
        )

    suspend fun save(groupId: String, avatar: GroupAvatarDto) {
        val bytes = avatar.bytes
        if (bytes == null) fileStore.delete(groupId) else fileStore.write(groupId, bytes)
        groupMetadataDao.upsertAvatar(
            groupId = groupId,
            hasAvatar = bytes != null,
            changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds
        )
    }

    suspend fun deleteLocal(groupId: String) {
        fileStore.delete(groupId)
        groupMetadataDao.clearAvatar(groupId)
    }
}
