package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.serialization.json.Json

internal fun List<MessagePartEntity>.toDtos(
    blobs: List<MessageBlobEntity>,
    resolveLocalFilePath: (String) -> String?
): List<MessagePartDto> {
    val blobsByPartId = blobs.associateBy(MessageBlobEntity::partId)
    val orderedParts = sortedBy(MessagePartEntity::position)
    val decodedById = orderedParts.associate { part ->
        part.id to part.toDto(
            blob = blobsByPartId[part.id],
            resolveLocalFilePath = resolveLocalFilePath
        )
    }
    val nestedPartIds = decodedById.values.flatMapTo(mutableSetOf()) { part -> part.nestedPartIds() }

    return orderedParts
        .filterNot { part -> part.id in nestedPartIds }
        .map { part -> decodedById.getValue(part.id).withNestedParts(decodedById) }
}

internal fun MessagePartEntity.toDto(
    blob: MessageBlobEntity?,
    resolveLocalFilePath: (String) -> String?
): MessagePartDto =
    when (MessageAttachmentType.valueOf(type)) {
        MessageAttachmentType.POLL -> {
            val json = requireNotNull(payload) { "Poll message part $id is missing payload" }
            Json.decodeFromString(PollDto.serializer(), json).also { poll ->
                require(poll.id == id) { "Message part/payload ID mismatch" }
            }
        }

        MessageAttachmentType.IMAGE -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            ImageDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize,
                width = persistedBlob.width,
                height = persistedBlob.height,
                fileName = persistedBlob.fileName,
                localFilePath = persistedBlob.localFilePath?.let(resolveLocalFilePath)
            )
        }

        MessageAttachmentType.VIDEO -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            VideoDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize,
                fileName = persistedBlob.fileName,
                width = persistedBlob.width,
                height = persistedBlob.height,
                durationMilliseconds = persistedBlob.durationMilliseconds,
                localFilePath = persistedBlob.localFilePath?.let(resolveLocalFilePath)
            )
        }

        MessageAttachmentType.FILE -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            FileDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize,
                fileName = persistedBlob.fileName ?: id,
                localFilePath = persistedBlob.localFilePath?.let(resolveLocalFilePath)
            )
        }

        MessageAttachmentType.VOICE -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            VoiceDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize,
                durationMilliseconds = requireNotNull(persistedBlob.durationMilliseconds) {
                    "Voice message part $id is missing duration"
                },
                localFilePath = persistedBlob.localFilePath?.let(resolveLocalFilePath)
            )
        }

        MessageAttachmentType.LOCATION -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            LocationDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize
            )
        }

        MessageAttachmentType.CONTACT -> {
            val persistedBlob = requireNotNull(blob) { "Message blob $id was not found" }
            require(id == persistedBlob.partId) { "Message part/blob ID mismatch" }
            ContactDto(
                id = id,
                blob = persistedBlob.toEncryptedBlobReferenceDto(),
                mimeType = persistedBlob.mimeType,
                byteSize = persistedBlob.byteSize
            )
        }
    }

private fun MessagePartDto.nestedPartIds(): List<String> =
    when (this) {
        is PollDto -> images.map(ImageDto::id)
        else -> emptyList()
    }

private fun MessagePartDto.withNestedParts(partsById: Map<String, MessagePartDto>): MessagePartDto =
    when (this) {
        is PollDto -> copy(
            images = images.map { image ->
                partsById[image.id] as? ImageDto
                    ?: error("Poll image ${image.id} was not found")
            }
        )

        else -> this
    }

internal fun MessageBlobEntity.toEncryptedBlobReferenceDto(): EncryptedBlobReferenceDto =
    EncryptedBlobReferenceDto(
        nodeId = nodeId,
        blobId = blobId,
        readCapability = readCapability,
        ciphertextByteSize = ciphertextByteSize,
        expiresAtEpochMilliseconds = blobExpiresAtEpochMilliseconds,
        encryptionKey = encryptionKey.copyOf(),
        nonce = nonce.copyOf(),
        ciphertextSha256 = ciphertextSha256.copyOf()
    )
