package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalAttachmentsUseCase(
    private val repository: MessageAttachmentRepository
) {
    operator fun invoke(conversationId: String): Flow<List<MessagePart>> =
        repository.observeLocalAttachments(conversationId)
}
