package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository

class ForwardToDirectConversationUseCase(
    private val directConversationRepository: DirectConversationRepository,
    private val forwardDirectMessage: ForwardDirectMessageUseCase
) {
    suspend operator fun invoke(
        conversationId: String,
        content: ForwardMessageContent
    ): Result<Unit> =
        safeSuspendCall {
            val contactId =
                directConversationRepository
                    .findContactId(conversationId)
                    .getOrThrow()
                    ?: error("Direct conversation contact was not found")

            forwardDirectMessage(
                contactId = contactId,
                content = content,
                conversationId = conversationId
            ).getOrThrow()
        }
}
