package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatarMetadata
import kotlinx.coroutines.flow.Flow

interface GroupAvatarRepository {
    fun observe(groupId: String): Flow<GroupAvatar>

    fun observeMetadata(groupId: String): Flow<GroupAvatarMetadata>

    suspend fun set(
        groupId: String,
        bytes: ByteArray
    ): Result<Unit>

    suspend fun remove(groupId: String): Result<Unit>
}
