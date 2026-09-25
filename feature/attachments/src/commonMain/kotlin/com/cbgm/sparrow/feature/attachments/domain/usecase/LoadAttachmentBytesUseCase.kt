package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class LoadAttachmentBytesUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(target: AttachmentTarget): Result<ByteArray> =
        repository.loadBytes(target)
}
