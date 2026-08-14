package com.cbgm.sparrow.feature.chats.domain.repository.direct

interface DirectMessageRepository {
    suspend fun send(
        conversationId: String,
        text: String
    ): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun refreshDeliveryState(conversationId: String): Result<Unit>

    suspend fun markConversationRead(conversationId: String): Result<Unit>
}
