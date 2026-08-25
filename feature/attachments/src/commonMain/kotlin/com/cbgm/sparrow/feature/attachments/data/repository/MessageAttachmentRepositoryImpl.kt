package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow

class MessageAttachmentRepositoryImpl(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource
) : MessageAttachmentRepository {
    override suspend fun loadBytes(attachmentId: String): Result<ByteArray> =
        messageAttachmentDataSource.loadBytes(attachmentId)

    override fun observeLocalAttachments(conversationId: String): Flow<List<LocalAttachment>> =
        localAttachmentDataSource.observeByConversation(conversationId)

    override fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> =
        localAttachmentDataSource.observeStorageSummaries()

    override suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit> =
        localAttachmentDataSource.delete(attachmentIds)

    override suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit> =
        localAttachmentDataSource.deleteForConversation(conversationId)
}
