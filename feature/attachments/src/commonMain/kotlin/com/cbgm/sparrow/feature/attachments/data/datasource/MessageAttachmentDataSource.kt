package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentMessageContextDto
import com.cbgm.sparrow.feature.attachments.data.model.OutgoingMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.UploadedBlobDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

internal class MessageAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
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

        val localFileName =
            if (type == MessageAttachmentType.VOICE) {
                null
            } else {
                try {
                    fileDataSource.write(bytes)
                } catch (error: Throwable) {
                    blobTransferDataSource.delete(uploaded)
                    throw error
                }
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
            localFileName = localFileName,
            payloadBytes = bytes.takeIf { type == MessageAttachmentType.VOICE }
        )
    }

    suspend fun persistOutgoing(messageId: String, prepared: List<PreparedMessageAttachmentDto>, context: AttachmentMessageContextDto) {
        if (prepared.isEmpty()) return
        saveMessageContext(messageId, context)
        attachmentDao.upsertAll(
            prepared.mapIndexed { index, item ->
                item.toMessageAttachmentEntity(messageId = messageId, position = index)
            }
        )
    }

    suspend fun persistIncoming(messageId: String, attachments: List<ProtocolMessageAttachment>, context: AttachmentMessageContextDto) {
        if (attachments.isEmpty()) return
        saveMessageContext(messageId, context)
        attachmentDao.upsertAll(
            attachments.mapIndexed { index, attachment ->
                attachment.toMessageAttachmentEntity(
                    messageId = messageId,
                    position = index,
                    deleteCapability = null,
                    localFileName = null,
                    payloadBytes = null
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
        // File-system naming is Attachments-owned. Keep existing saved copies in the
        // same conversation directory when Chats or Contacts changes its display name.
        localAttachmentDataSource.updateSavedConversationName(conversationId, normalizedName)
    }

    suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment> =
        attachmentDao.findByMessageId(messageId).map { entity -> entity.toProtocolMessageAttachment() }

    suspend fun loadDetachedBytes(attachment: ProtocolMessageAttachment): ByteArray =
        withContext(Dispatchers.IO) {
            blobTransferDataSource.download(attachment.blob)
        }

    suspend fun cacheIncoming(messageId: String) {
        coroutineScope {
            attachmentDao.findByMessageId(messageId)
                .map { entity ->
                    async {
                        try {
                            loadBytes(entity.id)
                        } catch (error: Exception) {
                            logger.warn(error) { "Could not cache message attachment ${entity.id}" }
                        }
                    }
                }.awaitAll()
        }
    }

    suspend fun updateTranscript(attachmentId: String, transcript: String) {
        check(attachmentDao.updateTranscript(attachmentId, transcript) == 1) {
            "Message attachment disappeared while its transcript was saved"
        }
    }

    fun observeTranscript(attachmentId: String): Flow<String?> =
        attachmentDao.observeById(attachmentId)
            .map { entity -> entity?.transcript }

    suspend fun resolveLocalFilePath(attachmentId: String): String? =
        withContext(Dispatchers.IO) {
            attachmentDao
                .findById(attachmentId)
                ?.localFileName
                ?.let(fileDataSource::resolveCacheFilePath)
        }

    suspend fun loadBytes(attachmentId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val entity = attachmentDao.findById(attachmentId) ?: error("Message attachment was not found")
            val type = MessageAttachmentType.valueOf(entity.type)
            val bytes =
                if (type == MessageAttachmentType.VOICE) {
                    entity.payloadBytes ?: downloadAndPersistPayload(entity)
                } else {
                    entity.localFileName?.let(fileDataSource::read) ?: downloadAndCacheFile(entity)
                }

            if (type != MessageAttachmentType.VOICE) {
                localAttachmentDataSource.saveIncomingConversationCopy(entity, bytes)
            }
            bytes
        }

    private suspend fun downloadAndCacheFile(entity: MessageAttachmentEntity): ByteArray {
        val bytes = blobTransferDataSource.download(entity.toEncryptedBlobReference())
        val localFileName = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFileName(entity.id, localFileName) == 1) {
            "Message attachment disappeared while it was cached"
        }
        return bytes
    }

    private suspend fun downloadAndPersistPayload(entity: MessageAttachmentEntity): ByteArray {
        val bytes = blobTransferDataSource.download(entity.toEncryptedBlobReference())
        check(attachmentDao.updatePayloadBytes(entity.id, bytes) == 1) {
            "Message attachment disappeared while its payload was cached"
        }
        return bytes
    }

    fun observeByMessageIds(messageIds: List<String>): Flow<List<MessageAttachmentEntity>> =
        attachmentDao.observeByMessageIds(messageIds)

    suspend fun deleteForMessages(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val entities = attachmentDao.findByMessageIds(messageIds)
        localAttachmentDataSource.delete(entities.mapTo(mutableSetOf(), MessageAttachmentEntity::id))

        entities.forEach { entity ->
            entity.deleteCapability?.let { deleteCapability ->
                try {
                    blobTransferDataSource.delete(
                        UploadedBlobDto(
                            reference = entity.toEncryptedBlobReference(),
                            deleteCapability = deleteCapability
                        )
                    )
                } catch (error: Throwable) {
                    logger.warn(error) { "Could not delete remote attachment blob ${entity.blobId}" }
                }
            }
        }
        attachmentDao.deleteByMessageIds(messageIds)
    }

    suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachmentDto>) {
        prepared.forEach { item ->
            try {
                item.localFileName?.let(fileDataSource::delete)
                item.deleteCapability.let { capability ->
                    blobTransferDataSource.delete(
                        UploadedBlobDto(
                            reference = item.attachment.blob,
                            deleteCapability = capability
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to cleanup prepared attachment during rollback" }
            }
        }
    }

    private fun PreparedMessageAttachmentDto.toMessageAttachmentEntity(
        messageId: String,
        position: Int
    ): MessageAttachmentEntity =
        attachment.toMessageAttachmentEntity(
            messageId = messageId,
            position = position,
            deleteCapability = deleteCapability,
            localFileName = localFileName,
            payloadBytes = payloadBytes
        )

    private fun ProtocolMessageAttachment.toMessageAttachmentEntity(
        messageId: String,
        position: Int,
        deleteCapability: String?,
        localFileName: String?,
        payloadBytes: ByteArray? = null
    ): MessageAttachmentEntity =
        MessageAttachmentEntity(
            id = attachmentId,
            messageId = messageId,
            position = position,
            type = type.name,
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
            localFileName = localFileName,
            payloadBytes = payloadBytes,
            transcript = null
        )

    private fun MessageAttachmentEntity.toProtocolMessageAttachment(): ProtocolMessageAttachment =
        ProtocolMessageAttachment(
            attachmentId = id,
            type = MessageAttachmentType.valueOf(type),
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            width = width,
            height = height,
            durationMilliseconds = durationMilliseconds,
            blob = toEncryptedBlobReference()
        )

    private fun MessageAttachmentEntity.toEncryptedBlobReference(): EncryptedBlobReference =
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
