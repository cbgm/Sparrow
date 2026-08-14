package com.cbgm.securechat.feature.chats.data.direct.repository

import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectMessageRepository

class DirectMessageRepositoryImpl(
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor,
    private val deliveryCoordinator: DirectMessageDeliveryCoordinator
) : DirectMessageRepository {
    override suspend fun send(
        conversationId: String,
        text: String
    ): Result<Unit> = outgoingMessageProcessor.send(conversationId, text)

    override suspend fun retry(messageId: String): Result<Unit> =
        outgoingMessageProcessor.retry(messageId)

    override suspend fun refreshDeliveryState(conversationId: String): Result<Unit> =
        runCatching {
            deliveryCoordinator.expireUnconfirmedMessages(conversationId)
        }

    override suspend fun markConversationRead(conversationId: String): Result<Unit> =
        outgoingMessageProcessor.sendReadReceipts(conversationId)
}
