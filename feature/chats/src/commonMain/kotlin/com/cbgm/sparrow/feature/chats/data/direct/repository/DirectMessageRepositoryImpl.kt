package com.cbgm.sparrow.feature.chats.data.direct.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class DirectMessageRepositoryImpl(
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor
) : DirectMessageRepository {
    override suspend fun send(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?
    ): Result<Unit> =
        outgoingMessageProcessor.send(conversationId, text, attachments, replyToMessageId)

    override suspend fun queueUntilAuthorized(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?
    ): Result<Unit> = outgoingMessageProcessor.queueUntilAuthorized(
        conversationId,
        text,
        attachments,
        replyToMessageId
    )

    override suspend fun toggleReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.toggleReaction(conversationId, messageId, emoji)
        }

    override suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.deleteMessage(conversationId, messageId)
        }

    override suspend fun editMessage(
        conversationId: String,
        messageId: String,
        text: String
    ): Result<Unit> = safeSuspendCall {
        outgoingMessageProcessor.editMessage(conversationId, messageId, text)
    }

    override suspend fun retry(messageId: String): Result<Unit> = safeSuspendCall {
        outgoingMessageProcessor.retry(messageId)
    }

    override suspend fun releaseWaitingForAuthorization(contactId: String): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.releaseWaitingForAuthorization(contactId)
        }

    override suspend fun discardWaitingForAuthorization(contactId: String): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.discardWaitingForAuthorization(contactId)
        }

    override suspend fun markConversationRead(conversationId: String): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.sendReadReceipts(conversationId)
        }
}
