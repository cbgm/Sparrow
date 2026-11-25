package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarReadRepository
import kotlinx.coroutines.flow.Flow

internal class GroupAvatarReadRepositoryImpl(
    private val dataSource: GroupAvatarDataSource
) : GroupAvatarReadRepository {
    override fun observe(groupId: String): Flow<GroupAvatar> = dataSource.observe(groupId)

    override fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata> = dataSource.observeMetadata(groupId)
}
