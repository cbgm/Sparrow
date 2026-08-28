package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import kotlinx.coroutines.flow.Flow

interface GroupAvatarRepository {
    fun observe(groupId: String): Flow<GroupAvatar>

    suspend fun set(
        groupId: String,
        bytes: ByteArray
    ): Result<Unit>

    suspend fun remove(groupId: String): Result<Unit>
}
