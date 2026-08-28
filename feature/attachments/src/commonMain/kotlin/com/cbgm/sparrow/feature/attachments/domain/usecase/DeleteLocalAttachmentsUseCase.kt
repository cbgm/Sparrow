package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class DeleteLocalAttachmentsUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(attachmentIds: Set<String>): Result<Unit> =
        repository.deleteLocalAttachments(attachmentIds)
}
