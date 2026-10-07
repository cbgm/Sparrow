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
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType

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

        MessageAttachmentType.POLL -> error("Poll is not a blob-backed attachment part")
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
