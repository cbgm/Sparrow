package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import kotlinx.coroutines.flow.Flow

interface GroupAvatarReadRepository {
    fun observe(groupId: String): Flow<GroupAvatar>

    fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata>
}
