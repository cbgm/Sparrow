package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository

class UploadBlobUseCase(
    private val repository: BlobTransferRepository
) {
    suspend operator fun invoke(
        plaintext: ByteArray,
        retentionMilliseconds: Long = MessageAttachmentPolicy.DEFAULT_RETENTION_MILLISECONDS
    ): Result<UploadedBlob> =
        repository.upload(
            plaintext = plaintext,
            retentionMilliseconds = retentionMilliseconds
        )
}
