package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity

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

internal fun MessagePartEntity.toMessagePartDto(
    blob: MessageBlobEntity,
    resolveLocalFilePath: (String) -> String?
): MessagePartDto {
    require(id == blob.partId) { "Message part/blob ID mismatch" }
    val reference = blob.toEncryptedBlobReferenceDto()
    val localFilePath = blob.localFilePath?.let(resolveLocalFilePath)

    return when (type) {
        IMAGE_TYPE ->
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

        VIDEO_TYPE ->
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

        FILE_TYPE ->
            FileDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize,
                fileName = blob.fileName ?: id,
                localFilePath = localFilePath
            )

        VOICE_TYPE ->
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

        LOCATION_TYPE ->
            LocationDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize
            )

        CONTACT_TYPE ->
            ContactDto(
                id = id,
                blob = reference,
                mimeType = blob.mimeType,
                byteSize = blob.byteSize
            )

        else -> error("Unsupported blob-backed message part type $type")
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

private const val IMAGE_TYPE = "IMAGE"
private const val VIDEO_TYPE = "VIDEO"
private const val FILE_TYPE = "FILE"
private const val VOICE_TYPE = "VOICE"
private const val LOCATION_TYPE = "LOCATION"
private const val CONTACT_TYPE = "CONTACT"
