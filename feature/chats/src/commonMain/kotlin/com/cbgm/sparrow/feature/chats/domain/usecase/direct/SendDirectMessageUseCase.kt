package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class SendDirectMessageUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        parts: List<MessagePart> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit> =
        repository.send(conversationId, text, parts, replyToMessageId)
}
