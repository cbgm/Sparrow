package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class MessageAttachmentRepositoryImpl(
    private val dataSource: MessageAttachmentDataSource
) : MessageAttachmentRepository {
    override suspend fun loadBytes(attachmentId: String): Result<ByteArray> =
        dataSource.loadBytes(attachmentId)
}
