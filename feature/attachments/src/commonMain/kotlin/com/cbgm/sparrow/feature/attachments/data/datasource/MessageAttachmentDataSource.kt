package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository

class MessageAttachmentDataSource(
    private val attachmentDao: MessageAttachmentDao,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val fileDataSource: MessageAttachmentFileDataSource,
    private val blobTransferDataSource: BlobTransferDataSource
) {
    private val logger = SparrowLog.withTag("MessageAttachmentDataSource")

    suspend fun prepareMedia(
        media: List<OutgoingMediaAttachment>,
        retentionMilliseconds: Long = BlobTransferRepository.DEFAULT_RETENTION_MILLISECONDS
    ): Result<List<PreparedMessageAttachment>> =
        runCatching {
            require(retentionMilliseconds > 0L) { "Attachment retention must be positive" }
            MessageAttachmentPolicy.requireValid(media)
            val prepared = mutableListOf<PreparedMessageAttachment>()
            try {
                media.forEach { item ->
                    val uploaded = blobTransferDataSource.upload(item.bytes, retentionMilliseconds).getOrThrow()
                    val localFileName =
                        runCatching { fileDataSource.write(item.bytes) }
                            .getOrElse { error ->
                                blobTransferDataSource.delete(uploaded)
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

    suspend fun persistOutgoing(
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
            val bytes =
                entity.localFileName
                    ?.let(fileDataSource::read)
                    ?: downloadAndCache(entity)
            persistSparrowContactCopy(entity, bytes)
            bytes
        }

    private suspend fun downloadAndCache(entity: MessageAttachmentEntity): ByteArray {
        val bytes = blobTransferDataSource.download(entity.toBlobReference()).getOrThrow()
        val localFileName = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFileName(entity.id, localFileName) == 1) {
            "Message attachment disappeared while it was cached"
        }
        return bytes
    }

    private suspend fun persistSparrowContactCopy(
        entity: MessageAttachmentEntity,
        bytes: ByteArray
    ) {
        runCatching {
            val message = chatDao.findMessageById(entity.messageId) ?: return
            if (message.isMine) return

            val conversation = chatDao.findConversationById(message.conversationId) ?: return
            val contactId = message.senderContactId ?: conversation.contactId ?: return
            val contact = contactDao.findById(contactId)
            val contactName =
                contact?.contact?.displayName
                    ?.takeIf(String::isNotBlank)
                    ?: contact?.phoneNumbers?.firstOrNull()?.value
                    ?: contactId
            fileDataSource.saveForContact(
                contactId = contactId,
                contactName = contactName,
                attachmentId = entity.id,
                type = MessageAttachmentType.valueOf(entity.type),
                mimeType = entity.mimeType,
                originalFileName = entity.fileName,
                bytes = bytes
            )
        }.onFailure { error ->
            logger.warn(error) { "Could not save Sparrow contact copy for attachment ${entity.id}" }
        }
    }

    suspend fun deleteLocalFilesForConversation(conversationId: String) {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        attachmentDao.findByConversationId(conversationId).forEach { entity ->
            entity.localFileName?.let { fileName ->
                runCatching { fileDataSource.delete(fileName) }
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
            entity.localFileName?.let { fileName -> runCatching { fileDataSource.delete(fileName) } }
            entity.deleteCapability?.let { deleteCapability ->
                blobTransferDataSource.delete(
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

    suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachment>) {
        prepared.forEach { item ->
            runCatching { fileDataSource.delete(item.localFileName) }
            blobTransferDataSource.delete(
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
