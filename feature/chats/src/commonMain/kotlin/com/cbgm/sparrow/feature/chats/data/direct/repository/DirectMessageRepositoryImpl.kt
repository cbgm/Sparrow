package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.model.attachment.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class DirectMessageRepositoryImpl(
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor
) : DirectMessageRepository {
    override suspend fun send(
        conversationId: String,
        text: String,
        media: List<OutgoingMediaAttachment>
    ): Result<Unit> = outgoingMessageProcessor.send(conversationId, text, media)

    override suspend fun queueUntilAuthorized(
        conversationId: String,
        text: String,
        media: List<OutgoingMediaAttachment>
    ): Result<Unit> = outgoingMessageProcessor.queueUntilAuthorized(conversationId, text, media)

    override suspend fun retry(messageId: String): Result<Unit> =
        outgoingMessageProcessor.retry(messageId)

    override suspend fun markConversationRead(conversationId: String): Result<Unit> =
        outgoingMessageProcessor.sendReadReceipts(conversationId)
}
