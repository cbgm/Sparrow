package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class LoadMessageAttachmentUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(attachmentId: String): Result<ByteArray> =
        repository.loadBytes(attachmentId)
}
