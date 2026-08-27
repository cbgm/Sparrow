package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRow
import com.cbgm.sparrow.feature.attachments.data.mapper.toAttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.data.mapper.toLocalAttachments
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val fileDataSource: MessageAttachmentFileDataSource
) {
    private val logger = SparrowLog.withTag("LocalAttachmentDataSource")

    suspend fun saveIncomingConversationCopy(
        entity: MessageAttachmentEntity,
        bytes: ByteArray
    ) {
        if (entity.type == MessageAttachmentType.LOCATION.name) return

        runCatching {
            val message = chatDao.findMessageById(entity.messageId) ?: return
            if (message.isMine) return

            val conversation = chatDao.findConversationById(message.conversationId) ?: return
            fileDataSource.saveForConversation(
                conversationId = conversation.id,
                displayName = resolveDisplayName(conversation),
                attachmentId = entity.id,
                type = MessageAttachmentType.valueOf(entity.type),
                mimeType = entity.mimeType,
                bytes = bytes
            )
            message.senderContactId?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, entity.id)
            }
        }.onFailure { error ->
            logger.warn(error) { "Could not save Sparrow conversation copy for attachment ${entity.id}" }
        }
    }

    fun observeByConversation(conversationId: String): Flow<List<LocalAttachment>> {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        return attachmentDao.observeLocalByConversationId(conversationId)
            .map { rows -> rows.toLocalAttachments() }
    }

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> =
        attachmentDao.observeAllLocal()
            .map { rows ->
                rows.groupBy { row -> row.conversationId }
                    .mapNotNull { (conversationId, conversationRows) ->
                        val conversation = chatDao.findConversationById(conversationId) ?: return@mapNotNull null
                        conversationRows
                            .toLocalAttachments()
                            .toAttachmentStorageSummary(
                                conversationId = conversationId,
                                displayName = resolveDisplayName(conversation),
                                isGroup = conversation.type == "GROUP"
                            )
                    }.sortedBy { summary -> summary.displayName.lowercase() }
            }

    suspend fun delete(attachmentIds: Set<String>): Result<Unit> =
        runCatching {
            if (attachmentIds.isEmpty()) return@runCatching

            val rows = attachmentDao.findLocalRowsByIds(attachmentIds.toList())
            for (row in rows) {
                deleteLocalCopies(row)
            }

            if (rows.isNotEmpty()) {
                attachmentDao.clearLocalFileNames(rows.map { row -> row.attachment.id })
            }
        }

    suspend fun deleteForConversation(conversationId: String): Result<Unit> =
        runCatching {
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
            attachmentDao.findByConversationId(conversationId).forEach { entity ->
                entity.localFileName?.let(fileDataSource::delete)
                deleteLegacyCopy(entity)
            }
            fileDataSource.deleteSavedConversation(conversationId)
            attachmentDao.clearLocalFileNamesForConversation(conversationId)
        }

    private suspend fun deleteLocalCopies(row: LocalMessageAttachmentRow) {
        row.attachment.localFileName?.let(fileDataSource::delete)
        fileDataSource.deleteSavedAttachment(
            conversationId = row.conversationId,
            attachmentId = row.attachment.id
        )
        deleteLegacyCopy(row.attachment)
    }

    private suspend fun deleteLegacyCopy(entity: MessageAttachmentEntity) {
        chatDao.findMessageById(entity.messageId)
            ?.senderContactId
            ?.let { senderContactId ->
                fileDataSource.deleteLegacyContactAttachment(senderContactId, entity.id)
            }
    }

    private suspend fun resolveDisplayName(conversation: ConversationEntity): String {
        conversation.title?.takeIf(String::isNotBlank)?.let { return it }
        val directContactId = conversation.contactId ?: return conversation.id
        val contact = contactDao.findById(directContactId)
        return contact?.contact?.displayName
            ?.takeIf(String::isNotBlank)
            ?: contact?.phoneNumbers?.firstOrNull()?.value
            ?: directContactId
    }
}
