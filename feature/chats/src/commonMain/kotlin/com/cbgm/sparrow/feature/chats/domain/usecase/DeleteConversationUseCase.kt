package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository

class DeleteConversationUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        runCatching {
            messageAttachmentRepository
                .deleteLocalAttachmentsForConversation(conversationId)
                .getOrThrow()
            conversationRepository
                .delete(conversationId)
                .getOrThrow()
        }
}
