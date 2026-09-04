package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendOrQueueDirectMessageUseCase

class ForwardDirectMessageUseCase(
    private val sendOrQueueDirectMessage: SendOrQueueDirectMessageUseCase
) {
    suspend operator fun invoke(
        contactId: String,
        content: ForwardMessageContent,
        conversationId: String? = null
    ): Result<Unit> =
        sendOrQueueDirectMessage(
            contactId = contactId,
            conversationId = conversationId,
            text = content.text,
            attachments = content.attachments
        ).map { Unit }
}
