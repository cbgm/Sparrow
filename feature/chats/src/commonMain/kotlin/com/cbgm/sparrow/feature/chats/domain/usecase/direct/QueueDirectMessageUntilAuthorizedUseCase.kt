package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class QueueDirectMessageUntilAuthorizedUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        parts: List<MessagePart>,
        replyToMessageId: String? = null
    ): Result<Unit> = repository.queueUntilAuthorized(conversationId, parts, replyToMessageId)
}
