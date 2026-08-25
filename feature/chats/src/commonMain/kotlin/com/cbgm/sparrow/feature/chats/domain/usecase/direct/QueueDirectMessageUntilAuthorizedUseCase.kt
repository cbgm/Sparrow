package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class QueueDirectMessageUntilAuthorizedUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        media: List<OutgoingMediaAttachment> = emptyList()
    ): Result<Unit> = repository.queueUntilAuthorized(conversationId, text, media)
}
