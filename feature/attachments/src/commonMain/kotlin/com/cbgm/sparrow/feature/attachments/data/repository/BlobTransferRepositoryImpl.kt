package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.data.datasource.BlobTransferDataSource
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository

class BlobTransferRepositoryImpl(
    private val dataSource: BlobTransferDataSource
) : BlobTransferRepository {
    override suspend fun upload(
        plaintext: ByteArray,
        retentionMilliseconds: Long
    ): Result<UploadedBlob> =
        safeSuspendCall { dataSource.upload(plaintext, retentionMilliseconds) }

    override suspend fun download(reference: EncryptedBlobReference): Result<ByteArray> =
        safeSuspendCall { dataSource.download(reference) }

    override suspend fun delete(uploadedBlob: UploadedBlob): Result<Unit> =
        safeSuspendCall { dataSource.delete(uploadedBlob) }
}
