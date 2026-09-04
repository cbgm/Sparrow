package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendOrQueueDirectMessageUseCase

class ForwardDirectMessageUseCase(
    private val sendOrQueueDirectMessage: SendOrQueueDirectMessageUseCase,
    private val directConversationRepository: DirectConversationRepository,
    private val conversationOverviewRepository: ConversationOverviewRepository
) {
    suspend operator fun invoke(
        contactId: String,
        content: ForwardMessageContent,
        conversationId: String? = null
    ): Result<Unit> =
        safeSuspendCall {
            sendOrQueueDirectMessage(
                contactId = contactId,
                conversationId = conversationId,
                text = content.text,
                attachments = content.attachments
            ).getOrThrow()

            val resolvedConversationId =
                conversationId
                    ?: directConversationRepository
                        .findConversationId(contactId)
                        .getOrThrow()
                    ?: error("Direct conversation was not found after forwarding")

            conversationOverviewRepository
                .incrementUnseenLocalMessageCount(resolvedConversationId)
                .getOrThrow()
        }
}
