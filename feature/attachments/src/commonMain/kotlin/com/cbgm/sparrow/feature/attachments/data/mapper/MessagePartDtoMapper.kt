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
import com.cbgm.sparrow.data.database.entity.MessageStructuredEntity
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.serialization.json.Json

internal fun List<MessagePartEntity>.toMessagePartDtos(
    blobs: List<MessageBlobEntity>,
    resolveLocalFilePath: (String) -> String?
): List<MessagePartDto> {
    val blobsByPartId = blobs.associateBy(MessageBlobEntity::partId)
    return map { part ->
        part.toMessagePartDto(
            blob = requireNotNull(blobsByPartId[part.id]) { "Message blob ${part.id} was not found" },
            resolveLocalFilePath = resolveLocalFilePath
        )
    }
}

internal fun List<MessagePartEntity>.toMessagePartDtos(
    blobs: List<MessageBlobEntity>,
    structured: List<MessageStructuredEntity>,
    resolveLocalFilePath: (String) -> String?
): List<MessagePartDto> {
    val blobsByPartId = blobs.associateBy(MessageBlobEntity::partId)
    val structuredByPartId = structured.associateBy(MessageStructuredEntity::partId)
    val orderedParts = sortedBy(MessagePartEntity::position)
    val decodedById = orderedParts.associate { part ->
        part.id to part.toMessagePartDto(
            blob = blobsByPartId[part.id],
            structured = structuredByPartId[part.id],
            resolveLocalFilePath = resolveLocalFilePath
        )
    }
    val nestedPartIds = decodedById.values.flatMapTo(mutableSetOf()) { part -> part.nestedPartIds() }

    return orderedParts
        .filterNot { part -> part.id in nestedPartIds }
        .map { part -> decodedById.getValue(part.id).withNestedParts(decodedById) }
}

internal fun List<MessagePartEntity>.toMessagePartDtosByMessageId(
    blobs: List<MessageBlobEntity>,
    resolveLocalFilePath: (String) -> String?
): Map<String, List<MessagePartDto>> {
    val blobsByPartId = blobs.associateBy(MessageBlobEntity::partId)
    return groupBy(MessagePartEntity::messageId)
        .mapValues { (_, parts) ->
            parts.sortedBy(MessagePartEntity::position)
                .map { part ->
                    part.toMessagePartDto(
                        blob = requireNotNull(blobsByPartId[part.id]) { "Message blob ${part.id} was not found" },
                        resolveLocalFilePath = resolveLocalFilePath
                    )
                }
        }
}

private fun MessagePartEntity.toMessagePartDto(
    blob: MessageBlobEntity?,
    structured: MessageStructuredEntity?,
    resolveLocalFilePath: (String) -> String?
): MessagePartDto =
    when (MessageAttachmentType.valueOf(type)) {
        MessageAttachmentType.POLL -> {
            requireNotNull(structured) { "Structured message part $id was not found" }
            require(id == structured.partId) { "Message part/structured ID mismatch" }
            Json.decodeFromString(PollDto.serializer(), structured.json).also { poll ->
                require(poll.id == id) { "Message part/structured payload ID mismatch" }
            }
        }

        else -> toMessagePartDto(
            blob = requireNotNull(blob) { "Message blob $id was not found" },
            resolveLocalFilePath = resolveLocalFilePath
        )
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

internal fun MessagePartEntity.toMessagePartDto(
    blob: MessageBlobEntity,
    resolveLocalFilePath: (String) -> String?
): MessagePartDto {
    require(id == blob.partId) { "Message part/blob ID mismatch" }
    val reference = blob.toEncryptedBlobReferenceDto()
    val localFilePath = blob.localFilePath?.let(resolveLocalFilePath)

    return when (MessageAttachmentType.valueOf(type)) {
        MessageAttachmentType.IMAGE ->
            ImageDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize,
                width = blob.width,
                height = blob.height,
                fileName = blob.fileName,
                localFilePath = localFilePath
            )

        MessageAttachmentType.VIDEO ->
            VideoDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize,
                fileName = blob.fileName,
                width = blob.width,
                height = blob.height,
                durationMilliseconds = blob.durationMilliseconds,
                localFilePath = localFilePath
            )

        MessageAttachmentType.FILE ->
            FileDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize,
                fileName = blob.fileName ?: id,
                localFilePath = localFilePath
            )

        MessageAttachmentType.VOICE ->
            VoiceDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize,
                durationMilliseconds = requireNotNull(blob.durationMilliseconds) {
                    "Voice message part $id is missing duration"
                },
                localFilePath = localFilePath
            )

        MessageAttachmentType.LOCATION ->
            LocationDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize
            )

        MessageAttachmentType.CONTACT ->
            ContactDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize
            )

        MessageAttachmentType.POLL -> error("Poll is a structured message part")
    }
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
