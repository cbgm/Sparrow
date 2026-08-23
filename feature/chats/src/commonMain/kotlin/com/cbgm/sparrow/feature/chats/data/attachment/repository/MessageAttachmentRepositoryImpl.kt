package com.cbgm.sparrow.feature.chats.data.attachment.repository

import com.cbgm.sparrow.feature.chats.data.attachment.MessageAttachmentTransfer
import com.cbgm.sparrow.feature.chats.domain.repository.attachment.MessageAttachmentRepository

class MessageAttachmentRepositoryImpl(
    private val transfer: MessageAttachmentTransfer
) : MessageAttachmentRepository {
    override suspend fun loadBytes(attachmentId: String): Result<ByteArray> =
        transfer.loadBytes(attachmentId)
}
