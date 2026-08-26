package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob

interface BlobTransferRepository {
    suspend fun upload(
        plaintext: ByteArray,
        retentionMilliseconds: Long = MessageAttachmentPolicy.DEFAULT_RETENTION_MILLISECONDS
    ): Result<UploadedBlob>

    suspend fun download(reference: EncryptedBlobReference): Result<ByteArray>

    suspend fun delete(uploadedBlob: UploadedBlob): Result<Unit>
}
