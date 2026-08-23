package com.cbgm.sparrow.feature.chats.domain.usecase.attachment

import com.cbgm.sparrow.feature.chats.domain.repository.attachment.MessageAttachmentRepository

class LoadMessageAttachmentUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(attachmentId: String): Result<ByteArray> =
        repository.loadBytes(attachmentId)
}
