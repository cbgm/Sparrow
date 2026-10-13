package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.data.datastore.group.GroupMetadataPreferenceKeys
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupAvatarDto

/** The avatar image is a file; its change timestamp lives in the preference store. */
internal class GroupAvatarDataSource(
    private val dataStore: SparrowDataStore,
    private val fileStore: GroupAvatarFileStore
) {
    suspend fun get(groupId: String): GroupAvatarDto =
        GroupAvatarDto(
            bytes = fileStore.read(groupId),
            changedAtEpochMilliseconds = dataStore.getLong(GroupMetadataPreferenceKeys.avatarChangedAt(groupId))
        )

    suspend fun save(groupId: String, avatar: GroupAvatarDto) {
        val bytes = avatar.bytes
        if (bytes == null) fileStore.delete(groupId) else fileStore.write(groupId, bytes)
        dataStore.edit {
            putLong(GroupMetadataPreferenceKeys.avatarChangedAt(groupId), avatar.changedAtEpochMilliseconds)
        }
    }

    suspend fun deleteLocal(groupId: String) {
        fileStore.delete(groupId)
        dataStore.edit { removeLong(GroupMetadataPreferenceKeys.avatarChangedAt(groupId)) }
    }
}
