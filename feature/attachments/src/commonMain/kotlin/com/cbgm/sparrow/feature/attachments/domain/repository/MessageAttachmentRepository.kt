package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import kotlinx.coroutines.flow.Flow

interface MessageAttachmentRepository {
    suspend fun loadBytes(attachmentId: String): Result<ByteArray>

    fun observeLocalAttachments(conversationId: String): Flow<List<LocalAttachment>>

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>>

    suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit>

    suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit>
}
