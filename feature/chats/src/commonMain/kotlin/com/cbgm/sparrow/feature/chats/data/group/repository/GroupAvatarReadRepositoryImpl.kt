package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarReadRepository
import com.cbgm.sparrow.protocol.avatar.GroupAvatarProvider
import com.cbgm.sparrow.protocol.avatar.GroupAvatarSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupAvatarReadRepositoryImpl(
    private val dataSource: GroupAvatarDataSource
) : GroupAvatarReadRepository,
    GroupAvatarProvider {
    override fun observe(groupId: String): Flow<GroupAvatar> = dataSource.observe(groupId)

    override fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata> = dataSource.observeMetadata(groupId)

    override fun observeSnapshot(groupId: String): Flow<GroupAvatarSnapshot> =
        dataSource.observe(groupId).map { avatar ->
            GroupAvatarSnapshot(
                groupId = avatar.groupId,
                changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds,
                bytes = avatar.bytes
            )
        }
}
