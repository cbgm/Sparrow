package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import kotlinx.coroutines.flow.Flow

interface GroupDescriptionRepository {
    fun observe(groupId: String): Flow<GroupDescription>

    suspend fun set(
        groupId: String,
        description: String
    ): Result<Unit>
}
