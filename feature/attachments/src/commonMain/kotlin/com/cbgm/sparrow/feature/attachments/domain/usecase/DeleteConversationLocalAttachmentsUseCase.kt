package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class DeleteConversationLocalAttachmentsUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        repository.deleteLocalAttachmentsForConversation(conversationId)
}
