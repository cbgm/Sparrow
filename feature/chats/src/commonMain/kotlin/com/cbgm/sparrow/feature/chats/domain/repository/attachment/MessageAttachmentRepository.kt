package com.cbgm.sparrow.feature.chats.domain.repository.attachment

interface MessageAttachmentRepository {
    suspend fun loadBytes(attachmentId: String): Result<ByteArray>
}
