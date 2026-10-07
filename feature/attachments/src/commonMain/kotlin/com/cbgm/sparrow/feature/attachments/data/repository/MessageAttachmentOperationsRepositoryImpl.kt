package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.data.model.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.data.datasource.BlobTransferDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentFileDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.requireBlobReference
import com.cbgm.sparrow.feature.attachments.data.mapper.toDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.runtime.MessageAttachmentCacheCoordinator
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MessageAttachmentOperationsRepositoryImpl(
    private val dataSource: MessageAttachmentDataSource,
    private val blobTransferDataSource: BlobTransferDataSource,
    private val fileDataSource: MessageAttachmentFileDataSource,
    private val cacheCoordinator: MessageAttachmentCacheCoordinator
) : MessageAttachmentOperationsRepository {
    private val logger = SparrowLog.withTag("MessageAttachmentOperationsRepository")

    override suspend fun persistOutgoing(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    ): Result<List<MessagePart>> = safeSuspendCall {
        if (parts.isEmpty()) return@safeSuspendCall emptyList()
        MessageAttachmentPolicy.requireValid(parts)

        val outgoing = parts.map { part -> createOutgoingDto(part) }
        MessageAttachmentPolicy.requireValidTotalPayloadBytes(
            outgoing.sumOf { part -> part.requirePayloadBytes().size.toLong() }
        )

        val localFileNames = linkedMapOf<String, String>()
        val localParts = mutableListOf<MessagePartDto>()

        outgoing.forEach { part ->
            val bytes = part.requirePayloadBytes()
            val localFileName = fileDataSource.write(bytes)
            localFileNames[part.id] = localFileName
            val localBlob = EncryptedBlobReferenceDto(
                nodeId = "",
                blobId = "local-${part.id}",
                readCapability = "",
                ciphertextByteSize = bytes.size.toLong(),
                expiresAtEpochMilliseconds = 0L,
                encryptionKey = ByteArray(0),
                nonce = ByteArray(0),
                ciphertextSha256 = ByteArray(0)
            )
            localParts += part.withBlob(localBlob)
        }

        dataSource.persistOutgoing(
            messageId = messageId,
            parts = localParts,
            deleteCapabilities = emptyMap(),
            localFileNames = localFileNames,
            context = context.toDto()
        )

        val uploaded = mutableListOf<MessagePartDto>()
        val deleteCapabilities = linkedMapOf<String, String>()

        try {
            outgoing.forEach { part ->
                val bytes = part.requirePayloadBytes()
                val (blobReference, deleteCapability) =
                    blobTransferDataSource.upload(
                        plaintext = bytes,
                        retentionMilliseconds = MessageAttachmentPolicy.DEFAULT_RETENTION_MILLISECONDS
                    )

                dataSource.updateRemoteBlobReference(
                    partId = part.id,
                    blobReference = blobReference,
                    deleteCapability = deleteCapability
                )

                val uploadedPart = part.withBlob(blobReference)
                uploaded += uploadedPart
                deleteCapabilities[part.id] = deleteCapability
            }

            uploaded.map { it.toMessagePart() }
        } catch (error: Throwable) {
            cleanupUploads(uploaded, deleteCapabilities, localFileNames)
            throw error
        }
    }

    override suspend fun persistIncoming(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    ) {
        val dtos = parts.map { it.toDto() }
        dataSource.persistIncoming(messageId, dtos, context.toDto())
    }

    override suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean) =
        dataSource.updateConversationDisplayName(conversationId, displayName, isGroup)

    override suspend fun messageParts(messageId: String): Result<List<MessagePart>> =
        safeSuspendCall {
            dataSource.messageParts(messageId).map { it.toMessagePart() }
        }

    override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> =
        safeSuspendCall {
            dataSource.loadDetachedBytes(part.id)
        }

    override suspend fun deleteForMessages(messageIds: List<String>) = dataSource.deleteForMessages(messageIds)

    override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> =
        dataSource.observeByMessageIds(messageIds)
            .map { partsByMessageId ->
                partsByMessageId.mapValues { (_, parts) ->
                    parts.map { part -> part.toMessagePart() }
                }
            }

    override fun cacheIncoming(messageId: String) = cacheCoordinator.cache(messageId)

    private suspend fun createOutgoingDto(part: MessagePart): MessagePartDto =
        when (part) {
            is Image -> {
                val bytes = resolveFileBackedBytes(part.localFilePath, part.id)
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                ImageDto(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = bytes.size.toLong(),
                    width = part.width,
                    height = part.height,
                    fileName = part.fileName,
                    bytes = bytes.copyOf()
                )
            }

            is Video -> {
                val bytes = resolveFileBackedBytes(part.localFilePath, part.id)
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                VideoDto(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = bytes.size.toLong(),
                    fileName = part.fileName,
                    width = part.width,
                    height = part.height,
                    durationMilliseconds = part.durationMilliseconds,
                    bytes = bytes.copyOf()
                )
            }

            is File -> {
                val bytes = resolveFileBackedBytes(part.localFilePath, part.id)
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                FileDto(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = bytes.size.toLong(),
                    fileName = part.fileName,
                    bytes = bytes.copyOf()
                )
            }

            is Voice -> {
                val bytes = resolveFileBackedBytes(part.localFilePath, part.id)
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                VoiceDto(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = bytes.size.toLong(),
                    durationMilliseconds = part.durationMilliseconds,
                    bytes = bytes.copyOf()
                )
            }

            is Location -> {
                val latitude = part.latitude
                val longitude = part.longitude
                val bytes =
                    if (latitude != null && longitude != null) {
                        LocationAttachmentPayload.encode(CurrentLocation(latitude, longitude))
                    } else {
                        dataSource.loadBytes(part.id)
                    }
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                LocationDto(
                    id = part.id,
                    mimeType = LOCATION_MIME_TYPE,
                    byteSize = bytes.size.toLong(),
                    bytes = bytes.copyOf()
                )
            }

            is Contact -> {
                val phoneNumber = part.phoneNumber
                val bytes =
                    if (phoneNumber != null) {
                        ContactAttachmentPayload.encode(
                            SharedContact(
                                displayName = part.displayName,
                                phoneNumber = phoneNumber
                            )
                        )
                    } else {
                        dataSource.loadBytes(part.id)
                    }
                MessageAttachmentPolicy.requireValidPayload(part, bytes)
                ContactDto(
                    id = part.id,
                    mimeType = CONTACT_MIME_TYPE,
                    byteSize = bytes.size.toLong(),
                    bytes = bytes.copyOf()
                )
            }

            is Text -> error("Text is not uploaded as an attachment message part")
            is Poll -> error("Poll upload is handled by the poll message-part flow")
        }

    private suspend fun resolveFileBackedBytes(localFilePath: String?, sourcePartId: String): ByteArray =
        localFilePath?.let(fileDataSource::readLocalFile) ?: dataSource.loadBytes(sourcePartId)

    private fun MessagePartDto.requirePayloadBytes(): ByteArray =
        requireNotNull(
            when (this) {
                is ImageDto -> bytes
                is VideoDto -> bytes
                is FileDto -> bytes
                is VoiceDto -> bytes
                is LocationDto -> bytes
                is ContactDto -> bytes
                else -> null
            }
        ) { "Message part $id has no outgoing payload bytes" }

    private fun MessagePartDto.withBlob(blob: EncryptedBlobReferenceDto): MessagePartDto =
        when (this) {
            is ImageDto -> copy(blob = blob)
            is VideoDto -> copy(blob = blob)
            is FileDto -> copy(blob = blob)
            is VoiceDto -> copy(blob = blob)
            is LocationDto -> copy(blob = blob)
            is ContactDto -> copy(blob = blob)
            else -> error("Message part $id is not an uploadable attachment")
        }

    private suspend fun cleanupUploads(
        parts: List<MessagePartDto>,
        deleteCapabilities: Map<String, String>,
        localFileNames: Map<String, String>
    ) {
        parts.forEach { part ->
            localFileNames[part.id]?.let(fileDataSource::delete)
            val deleteCapability = deleteCapabilities[part.id] ?: return@forEach
            val reference = part.requireBlobReference()
            runCatching {
                blobTransferDataSource.delete(
                    reference = reference,
                    deleteCapability = deleteCapability
                )
            }.onFailure { error ->
                logger.error(error) { "Failed to cleanup uploaded message part ${part.id}" }
            }
        }
    }
}
