package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent

class ForwardToContactUseCase(
    private val forwardDirectMessage: ForwardDirectMessageUseCase
) {
    suspend operator fun invoke(
        contactId: String,
        content: ForwardMessageContent
    ): Result<Unit> =
        forwardDirectMessage(
            contactId = contactId,
            content = content
        )
}
