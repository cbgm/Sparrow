package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository

class DeleteBlobUseCase(
    private val repository: BlobTransferRepository
) {
    suspend operator fun invoke(uploadedBlob: UploadedBlob): Result<Unit> =
        repository.delete(uploadedBlob)
}
