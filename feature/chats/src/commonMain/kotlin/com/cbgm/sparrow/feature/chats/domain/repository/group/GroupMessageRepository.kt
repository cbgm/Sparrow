package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit>

    suspend fun toggleReaction(groupId: String, messageId: String, emoji: String): Result<Unit>

    suspend fun deleteMessage(groupId: String, messageId: String): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
