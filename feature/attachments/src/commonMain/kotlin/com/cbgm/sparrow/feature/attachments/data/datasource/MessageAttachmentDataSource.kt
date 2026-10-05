package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.dao.VoiceTranscriptDao
import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.data.database.entity.VoiceTranscriptEntity
import com.cbgm.sparrow.data.database.model.MessageBlobPartRowDto
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

    suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment> =
        attachmentDao.findByMessageId(messageId).map { it.toProtocolMessageAttachment() }

    suspend fun loadDetachedBytes(attachment: ProtocolMessageAttachment): ByteArray =
        withContext(Dispatchers.IO) {
            blobTransferDataSource.download(attachment.blob)
        }

    suspend fun cacheIncoming(messageId: String) {
        coroutineScope {
            attachmentDao.findByMessageId(messageId)
                .map { row ->
                    async {
                        try {
                            loadBytes(row.partId)
                        } catch (error: Exception) {
                            logger.error(error) { "Could not cache message part ${row.partId}" }
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
            attachmentDao
                .findById(attachmentId)
                ?.localFilePath
                ?.let(fileDataSource::resolveCacheFilePath)
        }

    suspend fun loadBytes(attachmentId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val row = attachmentDao.findById(attachmentId) ?: error("Message part was not found")
            val bytes = row.localFilePath?.let(fileDataSource::read) ?: downloadAndCacheFile(row)

            if (row.type != MessageAttachmentType.VOICE.name) {
                localAttachmentDataSource.saveIncomingConversationCopy(row, bytes)
            }
            bytes
        }

    private suspend fun downloadAndCacheFile(row: MessageBlobPartRowDto): ByteArray {
        val bytes = blobTransferDataSource.download(row.toEncryptedBlobReference())
        val localFilePath = fileDataSource.write(bytes)
        check(attachmentDao.updateLocalFilePath(row.partId, localFilePath) == 1) {
            "Message part disappeared while it was cached"
        }
        return bytes
    }

    fun observeByMessageIds(messageIds: List<String>): Flow<List<MessageBlobPartRowDto>> =
        attachmentDao.observeByMessageIds(messageIds)

    suspend fun deleteForMessages(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val rows = attachmentDao.findByMessageIds(messageIds)
        localAttachmentDataSource.delete(rows.mapTo(mutableSetOf(), MessageBlobPartRowDto::partId))

        rows.forEach { row ->
            row.deleteCapability?.let { deleteCapability ->
                try {
                    blobTransferDataSource.delete(
                        UploadedBlobDto(
                            reference = row.toEncryptedBlobReference(),
                            deleteCapability = deleteCapability
                        )
                    )
                } catch (error: Throwable) {
                    logger.error(error) { "Could not delete remote message blob ${row.blobId}" }
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

    private fun MessageBlobPartRowDto.toProtocolMessageAttachment(): ProtocolMessageAttachment =
        ProtocolMessageAttachment(
            attachmentId = partId,
            type = MessageAttachmentType.valueOf(type),
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            width = width,
            height = height,
            durationMilliseconds = durationMilliseconds,
            blob = toEncryptedBlobReference()
        )

    private fun MessageBlobPartRowDto.toEncryptedBlobReference(): EncryptedBlobReference =
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
