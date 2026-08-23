package com.cbgm.sparrow.feature.chats.domain.repository.direct

import com.cbgm.sparrow.feature.chats.domain.model.attachment.OutgoingMediaAttachment

interface DirectMessageRepository {
    suspend fun send(
        conversationId: String,
        text: String,
        media: List<OutgoingMediaAttachment> = emptyList()
    ): Result<Unit>

    suspend fun queueUntilAuthorized(
        conversationId: String,
        text: String,
        media: List<OutgoingMediaAttachment> = emptyList()
    ): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markConversationRead(conversationId: String): Result<Unit>
}
