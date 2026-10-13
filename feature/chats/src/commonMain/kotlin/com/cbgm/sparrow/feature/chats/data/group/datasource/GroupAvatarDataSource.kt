package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.data.datastore.group.GroupMetadataPreferenceKeys
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupAvatarDataSource(
    private val dataStore: SparrowDataStore,
    private val fileStore: GroupAvatarFileStore
) {
    fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return dataStore.observeLong(GroupMetadataPreferenceKeys.avatarChangedAt(groupId)).map { changedAt ->
            GroupAvatarMetadata(
                groupId = groupId,
                changedAtEpochMilliseconds = changedAt,
                hasAvatar = fileStore.exists(groupId)
            )
        }
    }

    fun observe(groupId: String): Flow<GroupAvatar> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return dataStore.observeLong(GroupMetadataPreferenceKeys.avatarChangedAt(groupId)).map { changedAt ->
            GroupAvatar(
                groupId = groupId,
                changedAtEpochMilliseconds = changedAt,
                bytes = fileStore.read(groupId)
            )
        }
    }
}
