package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.datastore.group.GroupAvatarFileStore
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GroupAvatarDataSource(
    private val groupMetadataDao: GroupMetadataDao,
    private val fileStore: GroupAvatarFileStore
) {
    fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return observeAvatarState(groupId).map { (hasAvatar, changedAt) ->
            GroupAvatarMetadata(
                groupId = groupId,
                changedAtEpochMilliseconds = changedAt,
                hasAvatar = hasAvatar
            )
        }
    }

    fun observe(groupId: String): Flow<GroupAvatar> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return observeAvatarState(groupId).map { (hasAvatar, changedAt) ->
            GroupAvatar(
                groupId = groupId,
                changedAtEpochMilliseconds = changedAt,
                bytes = if (hasAvatar) fileStore.read(groupId) else null
            )
        }
    }

    private fun observeAvatarState(groupId: String): Flow<Pair<Boolean, Long>> =
        groupMetadataDao.observe(groupId)
            .map { metadata ->
                (metadata?.hasAvatar == true) to (metadata?.avatarChangedAtEpochMilliseconds ?: 0L)
            }.distinctUntilChanged()
}
