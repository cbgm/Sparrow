package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class DirectMessageRepositoryImpl(
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor
) : DirectMessageRepository {
    override suspend fun send(
        conversationId: String,
        text: String
    ): Result<Unit> = outgoingMessageProcessor.send(conversationId, text)

    override suspend fun retry(messageId: String): Result<Unit> =
        outgoingMessageProcessor.retry(messageId)

    override suspend fun markConversationRead(conversationId: String): Result<Unit> =
        outgoingMessageProcessor.sendReadReceipts(conversationId)
}
