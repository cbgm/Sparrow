package com.cbgm.sparrow.feature.chats.domain.repository.direct

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment

interface DirectMessageRepository {
    suspend fun send(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit>

    suspend fun queueUntilAuthorized(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit>

    suspend fun toggleReaction(conversationId: String, messageId: String, emoji: String): Result<Unit>

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun releaseWaitingForAuthorization(contactId: String): Result<Unit>

    suspend fun discardWaitingForAuthorization(contactId: String): Result<Unit>

    suspend fun markConversationRead(conversationId: String): Result<Unit>
}
