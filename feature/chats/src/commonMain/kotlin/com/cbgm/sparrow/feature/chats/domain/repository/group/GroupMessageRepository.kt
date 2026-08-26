package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList()
    ): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
