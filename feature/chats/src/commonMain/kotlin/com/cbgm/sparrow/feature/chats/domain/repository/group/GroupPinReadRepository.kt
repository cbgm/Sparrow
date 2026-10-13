package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import kotlinx.coroutines.flow.Flow

interface GroupPinReadRepository {
    fun observe(groupId: String): Flow<GroupPin?>

    suspend fun loadAttachment(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray>
}
