package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.dao.VoiceTranscriptDao
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.data.database.entity.VoiceTranscriptEntity
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessagePartDto
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessagePartDtosByMessageId
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentMessageContextDto
import com.cbgm.sparrow.feature.attachments.data.model.OutgoingMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.UploadedBlobDto
import com.cbgm.sparrow.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.cbgm.sparrow.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

internal class MessageAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val voiceTranscriptDao: VoiceTranscriptDao,
    private val fileDataSource: MessageAttachmentFileDataSource,
    private val blobTransferDataSource: BlobTransferDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource
) {
    private val logger = SparrowLog.withTag("MessageAttachmentDataSource")

    suspend fun prepareAttachments(
        attachments: List<OutgoingMessageAttachmentDto>,
        retentionMilliseconds: Long
    ): List<PreparedMessageAttachmentDto> {
        require(retentionMilliseconds > 0L) { "Attachment retention must be positive" }

        val prepared = mutableListOf<PreparedMessageAttachmentDto>()
        return try {
            attachments.forEach { item ->
                prepared += prepareAttachment(
                    attachmentId = item.id,
                    type = item.type,
                    bytes = item.bytes,
                    mimeType = item.mimeType,
                    retentionMilliseconds = retentionMilliseconds,
                    fileName = item.fileName,
                    width = item.width,
                    height = item.height,
                    durationMilliseconds = item.durationMilliseconds
                )
            }
            prepared
        } catch (error: Throwable) {
            cleanupPrepared(prepared)
            throw error
        }
    }

    private suspend fun prepareAttachment(
        attachmentId: String,
        type: MessageAttachmentType,
        bytes: ByteArray,
        mimeType: String,
        retentionMilliseconds: Long,
        fileName: String? = null,
        width: Int? = null,
        height: Int? = null,
        durationMilliseconds: Long? = null
    ): PreparedMessageAttachmentDto {
        val uploaded = blobTransferDataSource.upload(bytes, retentionMilliseconds)
        val localFilePath =
            try {
                fileDataSource.write(bytes)
            } catch (error: Throwable) {
                blobTransferDataSource.delete(uploaded)
                throw error
            }

        return PreparedMessageAttachmentDto(
            attachment = ProtocolMessageAttachment(
                attachmentId = attachmentId,
                type = type,
                mimeType = mimeType,
                byteSize = bytes.size.toLong(),
                blob = uploaded.reference,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            ),
            deleteCapability = uploaded.deleteCapability,
            localFileName = localFilePath,
            payloadBytes = null
        )
    }

    suspend fun persistOutgoing(
        messageId: String,
        prepared: List<PreparedMessageAttachmentDto>,
        context: AttachmentMessageContextDto
    ) {
        if (prepared.isEmpty()) return
        saveMessageContext(messageId, context)
        attachmentDao.upsertBlobParts(
            parts = prepared.mapIndexed { index, item -> item.attachment.toMessagePartEntity(messageId, index + 1) },
            blobs = prepared.map { item -> item.toMessageBlobEntity() }
        )
    }

    suspend fun persistIncoming(
        messageId: String,
        attachments: List<ProtocolMessageAttachment>,
        context: AttachmentMessageContextDto
    ) {
        if (attachments.isEmpty()) return
        saveMessageContext(messageId, context)
        attachmentDao.upsertBlobParts(
            parts = attachments.mapIndexed { index, attachment -> attachment.toMessagePartEntity(messageId, index + 1) },
            blobs = attachments.map { attachment ->
                attachment.toMessageBlobEntity(
                    deleteCapability = null,
                    localFilePath = null
                )
            }
        )
    }

    private suspend fun saveMessageContext(messageId: String, context: AttachmentMessageContextDto) {
        require(context.conversationId.isNotBlank())
        attachmentDao.upsertMessageContext(
            AttachmentMessageContextEntity(
                messageId = messageId,
                conversationId = context.conversationId,
                createdAtEpochMilliseconds = context.createdAtEpochMilliseconds,
                displayName = context.displayName.ifBlank { context.conversationId },
                isGroup = context.isGroup,
                isMine = context.isMine,
                senderContactId = context.senderContactId
            )
        )
    }

    suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean) {
        require(conversationId.isNotBlank())
        val normalizedName = displayName.ifBlank { conversationId }
        attachmentDao.updateConversationDisplayName(conversationId, normalizedName, isGroup)
        localAttachmentDataSource.updateSavedConversationName(conversationId, normalizedName)
    }

    suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment> {
        val parts = attachmentDao.findBlobPartsByMessageId(messageId)
        val blobsByPartId = loadBlobs(parts).associateBy(MessageBlobEntity::partId)
        return parts.map { part ->
            part.toProtocolMessageAttachment(
                requireNotNull(blobsByPartId[part.id]) { "Message blob ${part.id} was not found" }
            )
        }
    }

    suspend fun loadDetachedBytes(attachment: ProtocolMessageAttachment): ByteArray =
        withContext(Dispatchers.IO) {
            blobTransferDataSource.download(attachment.blob)
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
            val partDto = part.toMessagePartDto(blob, fileDataSource::resolveCacheFilePath)
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
        val bytes = blobTransferDataSource.download(blob.toEncryptedBlobReference())
        val localFilePath = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFilePath(blob.partId, localFilePath) == 1) {
            "Message part disappeared while it was cached"
        }
        return bytes
    }

    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePartDto>>> =
        attachmentDao.observeBlobPartsByMessageIds(messageIds)
            .map { parts ->
                parts.toMessagePartDtosByMessageId(
                    blobs = loadBlobs(parts),
                    resolveLocalFilePath = fileDataSource::resolveCacheFilePath
                )
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
                        UploadedBlobDto(
                            reference = blob.toEncryptedBlobReference(),
                            deleteCapability = deleteCapability
                        )
                    )
                } catch (error: Throwable) {
                    logger.error(error) { "Could not delete remote message blob ${blob.blobId}" }
                }
            }
        }
        attachmentDao.deleteByMessageIds(messageIds)
    }

    suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachmentDto>) {
        prepared.forEach { item ->
            try {
                item.localFileName?.let(fileDataSource::delete)
                blobTransferDataSource.delete(
                    UploadedBlobDto(
                        reference = item.attachment.blob,
                        deleteCapability = item.deleteCapability
                    )
                )
            } catch (error: Exception) {
                logger.error(error) { "Failed to cleanup prepared message blob during rollback" }
            }
        }
    }

    private suspend fun loadBlobs(parts: List<MessagePartEntity>): List<MessageBlobEntity> =
        if (parts.isEmpty()) {
            emptyList()
        } else {
            attachmentDao.findBlobsByPartIds(parts.map(MessagePartEntity::id))
        }

    private fun ProtocolMessageAttachment.toMessagePartEntity(
        messageId: String,
        position: Int
    ): MessagePartEntity =
        MessagePartEntity(
            id = attachmentId,
            messageId = messageId,
            position = position,
            type = type.name
        )

    private fun PreparedMessageAttachmentDto.toMessageBlobEntity(): MessageBlobEntity =
        attachment.toMessageBlobEntity(
            deleteCapability = deleteCapability,
            localFilePath = localFileName
        )

    private fun ProtocolMessageAttachment.toMessageBlobEntity(
        deleteCapability: String?,
        localFilePath: String?
    ): MessageBlobEntity =
        MessageBlobEntity(
            partId = attachmentId,
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            width = width,
            height = height,
            durationMilliseconds = durationMilliseconds,
            nodeId = blob.nodeId,
            blobId = blob.blobId,
            readCapability = blob.readCapability,
            ciphertextByteSize = blob.ciphertextByteSize,
            blobExpiresAtEpochMilliseconds = blob.expiresAtEpochMilliseconds,
            encryptionKey = blob.encryptionKey.copyOf(),
            nonce = blob.nonce.copyOf(),
            ciphertextSha256 = blob.ciphertextSha256.copyOf(),
            deleteCapability = deleteCapability,
            localFilePath = localFilePath
        )

    private fun MessagePartEntity.toProtocolMessageAttachment(blob: MessageBlobEntity): ProtocolMessageAttachment =
        ProtocolMessageAttachment(
            attachmentId = id,
            type = MessageAttachmentType.valueOf(type),
            mimeType = blob.mimeType,
            byteSize = blob.byteSize,
            fileName = blob.fileName,
            width = blob.width,
            height = blob.height,
            durationMilliseconds = blob.durationMilliseconds,
            blob = blob.toEncryptedBlobReference()
        )

    private fun MessageBlobEntity.toEncryptedBlobReference(): EncryptedBlobReference =
        EncryptedBlobReference(
            nodeId = nodeId,
            blobId = blobId,
            readCapability = readCapability,
            ciphertextByteSize = ciphertextByteSize,
            expiresAtEpochMilliseconds = blobExpiresAtEpochMilliseconds,
            encryptionKey = encryptionKey.copyOf(),
            nonce = nonce.copyOf(),
            ciphertextSha256 = ciphertextSha256.copyOf()
        )
}
