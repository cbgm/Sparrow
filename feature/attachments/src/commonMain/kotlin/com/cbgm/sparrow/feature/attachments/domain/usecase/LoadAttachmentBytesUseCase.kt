package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class LoadAttachmentBytesUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(partId: String, groupId: String? = null): Result<ByteArray> =
        repository.loadBytes(partId, groupId)
}
