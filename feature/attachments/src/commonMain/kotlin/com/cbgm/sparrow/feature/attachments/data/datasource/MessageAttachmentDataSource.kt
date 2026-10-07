package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.dao.VoiceTranscriptDao
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.data.database.entity.VoiceTranscriptEntity
import com.cbgm.sparrow.feature.attachments.data.mapper.flattenForPersistence
import com.cbgm.sparrow.feature.attachments.data.mapper.requireBlobReference
import com.cbgm.sparrow.feature.attachments.data.mapper.toDto
import com.cbgm.sparrow.feature.attachments.data.mapper.toDtos
import com.cbgm.sparrow.feature.attachments.data.mapper.toEncryptedBlobReferenceDto
import com.cbgm.sparrow.feature.attachments.data.mapper.toEntity
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessageBlobEntityOrNull
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessagePartEntity
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentMessageContextDto
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class MessageAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val voiceTranscriptDao: VoiceTranscriptDao,
    private val fileDataSource: MessageAttachmentFileDataSource,
    private val blobTransferDataSource: BlobTransferDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource
) {
    private val logger = SparrowLog.withTag("MessageAttachmentDataSource")

    suspend fun persistOutgoing(
        messageId: String,
        parts: List<MessagePartDto>,
        deleteCapabilities: Map<String, String>,
        localFileNames: Map<String, String>,
        context: AttachmentMessageContextDto
    ) {
        if (parts.isEmpty()) return
        saveMessageContext(messageId, context)
        val persistedParts = parts.flattenForPersistence()
        attachmentDao.upsertMessageParts(
            parts = persistedParts.mapIndexed { index, part -> part.toMessagePartEntity(messageId, index + 1) },
            blobs = persistedParts.mapNotNull { part ->
                part.toMessageBlobEntityOrNull(
                    deleteCapability = deleteCapabilities[part.id],
                    localFilePath = localFileNames[part.id]
                )
            }
        )
    }

    suspend fun updateRemoteBlobReference(
        partId: String,
        blobReference: EncryptedBlobReferenceDto,
        deleteCapability: String
    ) {
        check(
            attachmentDao.updateRemoteBlobReference(
                partId = partId,
                nodeId = blobReference.nodeId,
                blobId = blobReference.blobId,
                readCapability = blobReference.readCapability,
                ciphertextByteSize = blobReference.ciphertextByteSize,
                blobExpiresAtEpochMilliseconds = blobReference.expiresAtEpochMilliseconds,
                encryptionKey = blobReference.encryptionKey,
                nonce = blobReference.nonce,
                ciphertextSha256 = blobReference.ciphertextSha256,
                deleteCapability = deleteCapability
            ) == 1
        ) { "Message blob was not found to update remote reference" }
    }

    suspend fun persistIncoming(
        messageId: String,
        parts: List<MessagePartDto>,
        context: AttachmentMessageContextDto
    ) {
        if (parts.isEmpty()) return
        saveMessageContext(messageId, context)
        val persistedParts = parts.flattenForPersistence()
        persistedParts.forEach { part ->
            val existing = attachmentDao.findPartById(part.id)
            check(existing == null || existing.messageId == messageId) { "Message part ID belongs to another message" }
        }
        attachmentDao.upsertMessageParts(
            parts = persistedParts.mapIndexed { index, part -> part.toMessagePartEntity(messageId, index + 1) },
            blobs = persistedParts.mapNotNull { part ->
                part.toMessageBlobEntityOrNull(
                    deleteCapability = null,
                    localFilePath = attachmentDao.findBlobByPartId(part.id)?.localFilePath
                )
            }
        )
    }

    private suspend fun saveMessageContext(messageId: String, context: AttachmentMessageContextDto) {
        require(context.conversationId.isNotBlank())
        attachmentDao.upsertMessageContext(context.toEntity(messageId))
    }

    suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean) {
        require(conversationId.isNotBlank())
        val normalizedName = displayName.ifBlank { conversationId }
        attachmentDao.updateConversationDisplayName(conversationId, normalizedName, isGroup)
        localAttachmentDataSource.updateSavedConversationName(conversationId, normalizedName)
    }

    suspend fun updateMessagePart(messageId: String, part: MessagePartDto) {
        val existing = attachmentDao.findPartById(part.id) ?: error("Message part was not found")
        check(existing.messageId == messageId) { "Message part belongs to another message" }
        attachmentDao.upsertParts(
            listOf(part.toMessagePartEntity(messageId = messageId, position = existing.position))
        )
    }

    suspend fun messageParts(messageId: String): List<MessagePartDto> {
        val parts = attachmentDao.findMessagePartsByMessageId(messageId)
        return parts.toDtos(
            blobs = loadBlobs(parts),
            resolveLocalFilePath = fileDataSource::resolveCacheFilePath
        )
    }

    suspend fun loadDetachedBytes(partId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val part = attachmentDao.findPartById(partId) ?: error("Message part was not found")
            val blob = attachmentDao.findBlobByPartId(partId) ?: error("Message blob was not found")
            val partDto = part.toDto(blob, fileDataSource::resolveCacheFilePath)
            blobTransferDataSource.download(partDto.requireBlobReference())
        }

    suspend fun cacheIncoming(messageId: String) {
        coroutineScope {
            attachmentDao.findBlobPartsByMessageId(messageId)
                .map { part ->
                    async {
                        try {
                            loadBytes(part.id)
                        } catch (error: Exception) {
                            logger.error(error) { "Could not cache message part ${part.id}" }
                        }
                    }
                }.awaitAll()
        }
    }

    suspend fun updateTranscript(attachmentId: String, transcript: String) {
        voiceTranscriptDao.upsert(
            VoiceTranscriptEntity(
                partId = attachmentId,
                transcript = transcript
            )
        )
    }

    fun observeTranscript(attachmentId: String): Flow<String?> =
        voiceTranscriptDao.observe(attachmentId)

    suspend fun resolveLocalFilePath(attachmentId: String): String? =
        withContext(Dispatchers.IO) {
            attachmentDao.findBlobByPartId(attachmentId)
                ?.localFilePath
                ?.let(fileDataSource::resolveCacheFilePath)
        }

    suspend fun loadBytes(attachmentId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val part = attachmentDao.findPartById(attachmentId) ?: error("Message part was not found")
            val blob = attachmentDao.findBlobByPartId(attachmentId) ?: error("Message blob was not found")
            val partDto = part.toDto(blob, fileDataSource::resolveCacheFilePath)
            val bytes = blob.localFilePath?.let(fileDataSource::read) ?: downloadAndCacheFile(blob)

            if (part.type != MessageAttachmentType.VOICE.name) {
                localAttachmentDataSource.saveIncomingConversationCopy(
                    messageId = part.messageId,
                    part = partDto,
                    bytes = bytes
                )
            }
            bytes
        }

    private suspend fun downloadAndCacheFile(blob: MessageBlobEntity): ByteArray {
        val bytes = blobTransferDataSource.download(blob.toEncryptedBlobReferenceDto())
        val localFilePath = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFilePath(blob.partId, localFilePath) == 1) {
            "Message part disappeared while it was cached"
        }
        return bytes
    }

    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePartDto>>> =
        attachmentDao.observeMessagePartsByMessageIds(messageIds)
            .map { parts ->
                val blobs = loadBlobs(parts)
                parts.groupBy(MessagePartEntity::messageId).mapValues { (_, messageParts) ->
                    messageParts.toDtos(
                        blobs = blobs,
                        resolveLocalFilePath = fileDataSource::resolveCacheFilePath
                    )
                }
            }

    suspend fun deleteForMessages(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val parts = attachmentDao.findBlobPartsByMessageIds(messageIds)
        val blobs = loadBlobs(parts)
        localAttachmentDataSource.delete(parts.mapTo(mutableSetOf(), MessagePartEntity::id))

        blobs.forEach { blob ->
            blob.deleteCapability?.let { deleteCapability ->
                try {
                    blobTransferDataSource.delete(
                        reference = blob.toEncryptedBlobReferenceDto(),
                        deleteCapability = deleteCapability
                    )
                } catch (error: Throwable) {
                    logger.error(error) { "Could not delete remote message blob ${blob.blobId}" }
                }
            }
        }
        attachmentDao.deleteByMessageIds(messageIds)
    }

    private suspend fun loadBlobs(parts: List<MessagePartEntity>): List<MessageBlobEntity> =
        if (parts.isEmpty()) {
            emptyList()
        } else {
            attachmentDao.findBlobsByPartIds(parts.map(MessagePartEntity::id))
        }
}
