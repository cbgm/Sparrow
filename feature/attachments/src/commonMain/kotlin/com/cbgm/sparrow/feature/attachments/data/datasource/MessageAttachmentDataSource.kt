package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.data.mapper.toMessageAttachmentsByMessageId
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

class MessageAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val fileDataSource: MessageAttachmentFileDataSource,
    private val blobTransferDataSource: BlobTransferDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource
) {
    private val logger = SparrowLog.withTag("MessageAttachmentDataSource")

    suspend fun prepareAttachments(
        attachments: List<OutgoingMessageAttachment>,
        retentionMilliseconds: Long = MessageAttachmentPolicy.DEFAULT_RETENTION_MILLISECONDS
    ): List<PreparedMessageAttachmentDto> {
        require(retentionMilliseconds > 0L) { "Attachment retention must be positive" }
        MessageAttachmentPolicy.requireValid(attachments)

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

        val localFileName = try {
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
            localFileName = localFileName
        )
    }

    suspend fun persistOutgoing(messageId: String, prepared: List<PreparedMessageAttachmentDto>) {
        if (prepared.isEmpty()) return
        attachmentDao.upsertAll(
            prepared.mapIndexed { index, item ->
                item.toMessageAttachmentEntity(messageId = messageId, position = index)
            }
        )
    }

    suspend fun persistIncoming(messageId: String, attachments: List<ProtocolMessageAttachment>) {
        if (attachments.isEmpty()) return
        attachmentDao.upsertAll(
            attachments.mapIndexed { index, attachment ->
                attachment.toMessageAttachmentEntity(
                    messageId = messageId,
                    position = index,
                    deleteCapability = null,
                    localFileName = null
                )
            }
        )
    }

    suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment> =
        attachmentDao.findByMessageId(messageId).map { entity -> entity.toProtocolMessageAttachment() }

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

    suspend fun loadBytes(attachmentId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val entity = attachmentDao.findById(attachmentId) ?: error("Message attachment was not found")
            val bytes = entity.localFileName?.let(fileDataSource::read) ?: downloadAndCache(entity)

            localAttachmentDataSource.saveIncomingConversationCopy(entity, bytes)
            bytes
        }

    private suspend fun downloadAndCache(entity: MessageAttachmentEntity): ByteArray {
        val bytes = blobTransferDataSource.download(entity.toEncryptedBlobReference())
        val localFileName = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFileName(entity.id, localFileName) == 1) {
            "Message attachment disappeared while it was cached"
        }
        return bytes
    }

    fun observeRecentByConversation(
        conversationId: String,
        messageLimit: Int
    ): Flow<Map<String, List<MessageAttachment>>> =
        attachmentDao.observeRecentByConversation(conversationId, messageLimit)
            .map { attachments ->
                attachments.toMessageAttachmentsByMessageId(fileDataSource::resolveCacheFilePath)
            }

    fun observeFromMessageCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<Map<String, List<MessageAttachment>>> =
        attachmentDao
            .observeFromMessageCursor(
                conversationId = conversationId,
                fromTimestamp = fromTimestamp,
                fromMessageId = fromMessageId
            ).map { attachments ->
                attachments.toMessageAttachmentsByMessageId(fileDataSource::resolveCacheFilePath)
            }

    suspend fun deleteForMessages(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val entities = attachmentDao.findByMessageIds(messageIds)
        localAttachmentDataSource.delete(entities.mapTo(mutableSetOf(), MessageAttachmentEntity::id))

        entities.forEach { entity ->
            entity.deleteCapability?.let { deleteCapability ->
                try {
                    blobTransferDataSource.delete(
                        UploadedBlob(
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
                item.localFileName.let { fileDataSource.delete(it) }
                item.deleteCapability.let { capability ->
                    blobTransferDataSource.delete(
                        UploadedBlob(
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
            localFileName = localFileName
        )

    private fun ProtocolMessageAttachment.toMessageAttachmentEntity(
        messageId: String,
        position: Int,
        deleteCapability: String?,
        localFileName: String?
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
            localFileName = localFileName
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
