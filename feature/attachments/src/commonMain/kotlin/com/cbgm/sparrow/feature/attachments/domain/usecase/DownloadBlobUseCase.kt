package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.protocol.attachment.EncryptedBlobReference

class DownloadBlobUseCase(
    private val repository: BlobTransferRepository
) {
    suspend operator fun invoke(reference: EncryptedBlobReference): Result<ByteArray> =
        repository.download(reference)
}
