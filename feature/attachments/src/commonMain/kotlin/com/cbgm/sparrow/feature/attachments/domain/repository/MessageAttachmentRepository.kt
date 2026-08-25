package com.cbgm.sparrow.feature.attachments.domain.repository

interface MessageAttachmentRepository {
    suspend fun loadBytes(attachmentId: String): Result<ByteArray>
}
