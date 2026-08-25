package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMediaAttachment

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        text: String,
        media: List<OutgoingMediaAttachment> = emptyList()
    ): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
