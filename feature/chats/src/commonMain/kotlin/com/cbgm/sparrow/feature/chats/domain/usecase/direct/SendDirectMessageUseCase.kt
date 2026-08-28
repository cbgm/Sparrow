package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class SendDirectMessageUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList()
    ): Result<Unit> =
        repository.send(conversationId, text, attachments)
}
