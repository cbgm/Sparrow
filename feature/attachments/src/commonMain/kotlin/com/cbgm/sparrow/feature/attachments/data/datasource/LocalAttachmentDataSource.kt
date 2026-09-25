package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentStorageSummaryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val fileDataSource: MessageAttachmentFileDataSource
) {
    private val logger = SparrowLog.withTag("LocalAttachmentDataSource")

    suspend fun saveIncomingConversationCopy(
        entity: MessageAttachmentEntity,
        bytes: ByteArray
    ) {
        if (
            entity.type == MessageAttachmentType.LOCATION.name ||
            entity.type == MessageAttachmentType.CONTACT.name ||
            entity.type == MessageAttachmentType.VOICE.name
        ) {
            return
        }

        try {
            val context = attachmentDao.findMessageContext(entity.messageId) ?: return
            if (context.isMine) return

            fileDataSource.saveForConversation(
                conversationId = context.conversationId,
                displayName = context.displayName,
                attachmentId = entity.id,
                type = MessageAttachmentType.valueOf(entity.type),
                mimeType = entity.mimeType,
                bytes = bytes
            )
            context.senderContactId?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, entity.id)
            }
        } catch (error: Throwable) {
            logger.error(error) { "Could not save Sparrow conversation copy for attachment ${entity.id}" }
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
            attachmentDao.clearLocalFileNames(rows.map { row -> row.attachment.id })
        }
    }

    suspend fun deleteForConversation(conversationId: String) {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        attachmentDao.findByConversationId(conversationId).forEach { entity ->
            entity.localFileName?.let(fileDataSource::delete)
            deleteLegacyCopy(entity)
        }
        fileDataSource.deleteSavedConversation(conversationId)
        attachmentDao.clearLocalFileNamesForConversation(conversationId)
    }

    private suspend fun deleteLocalCopies(row: LocalMessageAttachmentRowDto) {
        row.attachment.localFileName?.let(fileDataSource::delete)
        fileDataSource.deleteSavedAttachment(
            conversationId = row.conversationId,
            attachmentId = row.attachment.id
        )
        deleteLegacyCopy(row.attachment)
    }

    private suspend fun deleteLegacyCopy(entity: MessageAttachmentEntity) {
        attachmentDao.findMessageContext(entity.messageId)
            ?.senderContactId
            ?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, entity.id)
            }
    }
}
