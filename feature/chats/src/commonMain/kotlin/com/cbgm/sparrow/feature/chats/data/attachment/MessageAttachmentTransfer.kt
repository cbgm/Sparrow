package com.cbgm.sparrow.feature.chats.data.attachment

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DownloadBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.UploadBlobUseCase
import com.cbgm.sparrow.feature.chats.data.attachment.storage.MessageAttachmentFileStorage
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType
import com.cbgm.sparrow.feature.chats.domain.model.attachment.OutgoingMediaAttachment

class MessageAttachmentTransfer(
    private val attachmentDao: MessageAttachmentDao,
    private val fileStorage: MessageAttachmentFileStorage,
    private val uploadBlob: UploadBlobUseCase,
    private val downloadBlob: DownloadBlobUseCase,
    private val deleteBlob: DeleteBlobUseCase
) {
    private val logger = SparrowLog.withTag("MessageAttachmentTransfer")

    internal suspend fun prepareMedia(
        media: List<OutgoingMediaAttachment>,
        retentionMilliseconds: Long = BlobTransferRepository.DEFAULT_RETENTION_MILLISECONDS
    ): Result<List<PreparedMessageAttachment>> =
        runCatching {
            require(retentionMilliseconds > 0L) { "Attachment retention must be positive" }
            MessageAttachmentPolicy.requireValid(media)
            val prepared = mutableListOf<PreparedMessageAttachment>()
            try {
                media.forEach { item ->
                    val uploaded = uploadBlob(item.bytes, retentionMilliseconds).getOrThrow()
                    val localFileName =
                        runCatching { fileStorage.write(item.bytes) }
                            .getOrElse { error ->
                                deleteBlob(uploaded)
                                throw error
                            }
                    prepared +=
                        PreparedMessageAttachment(
                            attachment =
                                MessageAttachment(
                                    attachmentId = item.id,
                                    type = item.type.toProtocolType(),
                                    mimeType = item.mimeType,
                                    byteSize = item.bytes.size.toLong(),
                                    blob = uploaded.reference,
                                    width = item.width,
                                    height = item.height,
                                    durationMilliseconds = item.durationMilliseconds
                                ),
                            deleteCapability = uploaded.deleteCapability,
                            localFileName = localFileName
                        )
                }
                prepared
            } catch (error: Throwable) {
                cleanupPrepared(prepared)
                throw error
            }
        }

    internal suspend fun persistOutgoing(
        messageId: String,
        prepared: List<PreparedMessageAttachment>
    ) {
        if (prepared.isEmpty()) return
        attachmentDao.upsertAll(
            prepared.mapIndexed { index, item ->
                item.toEntity(messageId = messageId, position = index)
            }
        )
    }

    suspend fun persistIncoming(
        messageId: String,
        attachments: List<MessageAttachment>
    ) {
        if (attachments.isEmpty()) return
        attachmentDao.upsertAll(
            attachments.mapIndexed { index, attachment ->
                attachment.toEntity(
                    messageId = messageId,
                    position = index,
                    deleteCapability = null,
                    localFileName = null
                )
            }
        )
    }

    suspend fun protocolAttachments(messageId: String): List<MessageAttachment> =
        attachmentDao.findByMessageId(messageId).map { entity -> entity.toProtocolAttachment() }

    suspend fun cacheIncoming(messageId: String) {
        attachmentDao.findByMessageId(messageId)
            .filter { it.localFileName == null }
            .forEach { entity ->
                loadBytes(entity.id)
                    .onFailure { error ->
                        logger.warn(error) { "Could not cache message attachment ${entity.id}" }
                    }
            }
    }

    suspend fun loadBytes(attachmentId: String): Result<ByteArray> =
        runCatching {
            val entity = attachmentDao.findById(attachmentId) ?: error("Message attachment was not found")
            entity.localFileName?.let(fileStorage::read)?.let { return@runCatching it }

            val bytes = downloadBlob(entity.toBlobReference()).getOrThrow()
            val localFileName = fileStorage.write(bytes)
            check(attachmentDao.updateLocalFileName(entity.id, localFileName) == 1) {
                "Message attachment disappeared while it was cached"
            }
            bytes
        }

    suspend fun deleteLocalFilesForConversation(conversationId: String) {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        attachmentDao.findByConversationId(conversationId).forEach { entity ->
            entity.localFileName?.let { fileName ->
                runCatching { fileStorage.delete(fileName) }
                    .onFailure { error ->
                        logger.warn(error) { "Could not delete local attachment file ${entity.id}" }
                    }
            }
        }
    }

    suspend fun deleteForMessages(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val entities = attachmentDao.findByMessageIds(messageIds)
        entities.forEach { entity ->
            entity.localFileName?.let { fileName -> runCatching { fileStorage.delete(fileName) } }
            entity.deleteCapability?.let { deleteCapability ->
                deleteBlob(
                    UploadedBlob(
                        reference = entity.toBlobReference(),
                        deleteCapability = deleteCapability
                    )
                ).onFailure { error ->
                    logger.warn(error) { "Could not delete remote attachment blob ${entity.blobId}" }
                }
            }
        }
        attachmentDao.deleteByMessageIds(messageIds)
    }

    internal suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachment>) {
        prepared.forEach { item ->
            runCatching { fileStorage.delete(item.localFileName) }
            deleteBlob(
                UploadedBlob(
                    reference = item.attachment.blob,
                    deleteCapability = item.deleteCapability
                )
            ).onFailure { error ->
                logger.warn(error) { "Could not clean up prepared attachment ${item.attachment.attachmentId}" }
            }
        }
    }

    private fun PreparedMessageAttachment.toEntity(
        messageId: String,
        position: Int
    ): MessageAttachmentEntity =
        attachment.toEntity(
            messageId = messageId,
            position = position,
            deleteCapability = deleteCapability,
            localFileName = localFileName
        )

    private fun MessageAttachment.toEntity(
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

    private fun MessageAttachmentEntity.toProtocolAttachment(): MessageAttachment =
        MessageAttachment(
            attachmentId = id,
            type = MessageAttachmentType.valueOf(type),
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            width = width,
            height = height,
            durationMilliseconds = durationMilliseconds,
            blob = toBlobReference()
        )

    private fun MessageAttachmentEntity.toBlobReference(): EncryptedBlobReference =
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

    private fun MessageMediaType.toProtocolType(): MessageAttachmentType =
        when (this) {
            MessageMediaType.IMAGE -> MessageAttachmentType.IMAGE
            MessageMediaType.VIDEO -> MessageAttachmentType.VIDEO
        }
}
