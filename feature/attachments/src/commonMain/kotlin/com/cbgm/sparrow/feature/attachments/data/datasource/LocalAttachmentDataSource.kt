package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto
import com.cbgm.sparrow.data.database.model.MessageBlobPartRowDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentStorageSummaryDto
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val fileDataSource: MessageAttachmentFileDataSource
) {
    private val logger = SparrowLog.withTag("LocalAttachmentDataSource")

    suspend fun saveIncomingConversationCopy(
        row: MessageBlobPartRowDto,
        bytes: ByteArray
    ) {
        if (
            row.type == MessageAttachmentType.LOCATION.name ||
            row.type == MessageAttachmentType.CONTACT.name ||
            row.type == MessageAttachmentType.VOICE.name
        ) {
            return
        }

        try {
            val context = attachmentDao.findMessageContext(row.messageId) ?: return
            if (context.isMine) return

            fileDataSource.saveForConversation(
                conversationId = context.conversationId,
                displayName = context.displayName,
                attachmentId = row.partId,
                type = MessageAttachmentType.valueOf(row.type),
                mimeType = row.mimeType,
                bytes = bytes
            )
            context.senderContactId?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, row.partId)
            }
        } catch (error: Throwable) {
            logger.error(error) { "Could not save Sparrow conversation copy for message part ${row.partId}" }
        }
    }

    fun updateSavedConversationName(conversationId: String, displayName: String) {
        fileDataSource.updateSavedConversationName(conversationId, displayName)
    }

    fun observeByConversation(conversationId: String): Flow<List<LocalMessageAttachmentRowDto>> {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        return attachmentDao.observeLocalByConversationId(conversationId)
    }

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummaryDto>> =
        attachmentDao.observeAllLocal()
            .map { rows ->
                rows.groupBy { row -> row.conversationId }
                    .mapNotNull { (conversationId, conversationRows) ->
                        val first = conversationRows.firstOrNull() ?: return@mapNotNull null
                        AttachmentStorageSummaryDto(
                            conversationId = conversationId,
                            displayName = first.displayName,
                            isGroup = first.isGroup,
                            rows = conversationRows
                        )
                    }.sortedBy { summary -> summary.displayName.lowercase() }
            }

    suspend fun delete(attachmentIds: Set<String>) {
        if (attachmentIds.isEmpty()) return

        val rows = attachmentDao.findLocalRowsByIds(attachmentIds.toList())
        for (row in rows) {
            deleteLocalCopies(row)
        }

        if (rows.isNotEmpty()) {
            attachmentDao.clearLocalFilePaths(rows.map { row -> row.attachment.partId })
        }
    }

    suspend fun deleteForConversation(conversationId: String) {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        attachmentDao.findByConversationId(conversationId).forEach { row ->
            row.localFilePath?.let(fileDataSource::delete)
            deleteLegacyCopy(row)
        }
        fileDataSource.deleteSavedConversation(conversationId)
        attachmentDao.clearLocalFilePathsForConversation(conversationId)
    }

    private suspend fun deleteLocalCopies(row: LocalMessageAttachmentRowDto) {
        row.attachment.localFilePath?.let(fileDataSource::delete)
        fileDataSource.deleteSavedAttachment(
            conversationId = row.conversationId,
            attachmentId = row.attachment.partId
        )
        deleteLegacyCopy(row.attachment)
    }

    private suspend fun deleteLegacyCopy(row: MessageBlobPartRowDto) {
        attachmentDao.findMessageContext(row.messageId)
            ?.senderContactId
            ?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, row.partId)
            }
    }
}
