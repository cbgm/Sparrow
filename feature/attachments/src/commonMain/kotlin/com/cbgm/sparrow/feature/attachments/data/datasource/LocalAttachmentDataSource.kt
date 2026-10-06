package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessagePartDto
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.attachments.data.mapper.toSavedFileMetadata
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentStorageSummaryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val fileDataSource: MessageAttachmentFileDataSource
) {
    private val logger = SparrowLog.withTag("LocalAttachmentDataSource")

    suspend fun saveIncomingConversationCopy(
        messageId: String,
        part: MessagePartDto,
        bytes: ByteArray
    ) {
        val saveMetadata = part.toSavedFileMetadata() ?: return

        try {
            val context = attachmentDao.findMessageContext(messageId) ?: return
            if (context.isMine) return

            fileDataSource.saveForConversation(
                conversationId = context.conversationId,
                displayName = context.displayName,
                attachmentId = part.id,
                isMedia = saveMetadata.isMedia,
                mimeType = saveMetadata.mimeType,
                bytes = bytes
            )
        } catch (error: Throwable) {
            logger.error(error) { "Could not save Sparrow conversation copy for message part ${part.id}" }
        }
    }

    fun updateSavedConversationName(conversationId: String, displayName: String) {
        fileDataSource.updateSavedConversationName(conversationId, displayName)
    }

    fun observeByConversation(conversationId: String): Flow<List<MessagePartDto>> {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        return attachmentDao.observeLocalPartsByConversationId(conversationId)
            .map { parts ->
                parts.toMessagePartDtos(loadBlobs(parts), fileDataSource::resolveCacheFilePath)
                    .filter { part -> part.isManagedLocalPart() }
            }
    }

    fun observeStorageSummaries(): Flow<List<AttachmentStorageSummaryDto>> =
        attachmentDao.observeAllLocalParts()
            .map { parts ->
                if (parts.isEmpty()) return@map emptyList()

                val blobsByPartId = loadBlobs(parts).associateBy(MessageBlobEntity::partId)
                val contextsByMessageId =
                    attachmentDao.findMessageContexts(parts.map(MessagePartEntity::messageId).distinct())
                        .associateBy { context -> context.messageId }

                parts.mapNotNull { part ->
                    val context = contextsByMessageId[part.messageId] ?: return@mapNotNull null
                    val blob = blobsByPartId[part.id] ?: return@mapNotNull null
                    part.toMessagePartDto(blob, fileDataSource::resolveCacheFilePath)
                        .takeIf { partDto -> partDto.isManagedLocalPart() }
                        ?.let { partDto ->
                            StoredPart(
                                conversationId = context.conversationId,
                                displayName = context.displayName,
                                isGroup = context.isGroup,
                                part = partDto
                            )
                        }
                }.groupBy(StoredPart::conversationId)
                    .mapNotNull { (conversationId, storedParts) ->
                        val first = storedParts.firstOrNull() ?: return@mapNotNull null
                        AttachmentStorageSummaryDto(
                            conversationId = conversationId,
                            displayName = first.displayName,
                            isGroup = first.isGroup,
                            parts = storedParts.map(StoredPart::part)
                        )
                    }.sortedBy { summary -> summary.displayName.lowercase() }
            }

    suspend fun delete(attachmentIds: Set<String>) {
        if (attachmentIds.isEmpty()) return

        val parts = attachmentDao.findLocalPartsByIds(attachmentIds.toList())
        if (parts.isEmpty()) return

        val blobsByPartId = loadBlobs(parts).associateBy(MessageBlobEntity::partId)
        val contextsByMessageId =
            attachmentDao.findMessageContexts(parts.map(MessagePartEntity::messageId).distinct())
                .associateBy { context -> context.messageId }

        parts.forEach { part ->
            blobsByPartId[part.id]?.localFilePath?.let(fileDataSource::delete)
            contextsByMessageId[part.messageId]?.let { context ->
                fileDataSource.deleteSavedAttachment(
                    conversationId = context.conversationId,
                    attachmentId = part.id
                )
            }
        }

        attachmentDao.clearLocalFilePaths(parts.map(MessagePartEntity::id))
    }

    suspend fun deleteForConversation(conversationId: String) {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        val parts = attachmentDao.findBlobPartsByConversationId(conversationId)
        loadBlobs(parts).forEach { blob ->
            blob.localFilePath?.let(fileDataSource::delete)
        }
        fileDataSource.deleteSavedConversation(conversationId)
        attachmentDao.clearLocalFilePathsForConversation(conversationId)
    }

    private suspend fun loadBlobs(parts: List<MessagePartEntity>): List<MessageBlobEntity> =
        if (parts.isEmpty()) {
            emptyList()
        } else {
            attachmentDao.findBlobsByPartIds(parts.map(MessagePartEntity::id))
        }

    private fun MessagePartDto.isManagedLocalPart(): Boolean =
        this is ImageDto || this is VideoDto || this is FileDto || this is VoiceDto

    private data class StoredPart(
        val conversationId: String,
        val displayName: String,
        val isGroup: Boolean,
        val part: MessagePartDto
    )
}
