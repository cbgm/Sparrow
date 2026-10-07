package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType

internal fun MessagePartDto.toMessagePartEntity(
    messageId: String,
    position: Int
): MessagePartEntity =
    MessagePartEntity(
        id = id,
        messageId = messageId,
        position = position,
        type = persistenceType()
    )

internal fun MessagePartDto.toMessageBlobEntity(
    deleteCapability: String?,
    localFilePath: String?
): MessageBlobEntity {
    val reference = requireBlobReference()
    return MessageBlobEntity(
        partId = id,
        mimeType = mimeType(),
        byteSize = byteSize(),
        fileName = when (this) {
            is ImageDto -> fileName
            is VideoDto -> fileName
            is FileDto -> fileName
            else -> null
        },
        width = when (this) {
            is ImageDto -> width
            is VideoDto -> width
            else -> null
        },
        height = when (this) {
            is ImageDto -> height
            is VideoDto -> height
            else -> null
        },
        durationMilliseconds = when (this) {
            is VideoDto -> durationMilliseconds
            is VoiceDto -> durationMilliseconds
            else -> null
        },
        nodeId = reference.nodeId,
        blobId = reference.blobId,
        readCapability = reference.readCapability,
        ciphertextByteSize = reference.ciphertextByteSize,
        blobExpiresAtEpochMilliseconds = reference.expiresAtEpochMilliseconds,
        encryptionKey = reference.encryptionKey.copyOf(),
        nonce = reference.nonce.copyOf(),
        ciphertextSha256 = reference.ciphertextSha256.copyOf(),
        deleteCapability = deleteCapability,
        localFilePath = localFilePath
    )
}

internal fun MessagePartDto.requireBlobReference() =
    requireNotNull(
        when (this) {
            is ImageDto -> blob
            is VideoDto -> blob
            is FileDto -> blob
            is VoiceDto -> blob
            is LocationDto -> blob
            is ContactDto -> blob
            is TextDto -> null
            is PollDto -> null
        }
    ) { "Message part $id has no blob reference" }

private fun MessagePartDto.persistenceType(): String =
    when (this) {
        is ImageDto -> MessageAttachmentType.IMAGE.name
        is VideoDto -> MessageAttachmentType.VIDEO.name
        is FileDto -> MessageAttachmentType.FILE.name
        is VoiceDto -> MessageAttachmentType.VOICE.name
        is LocationDto -> MessageAttachmentType.LOCATION.name
        is ContactDto -> MessageAttachmentType.CONTACT.name
        is TextDto -> error("Text is not persisted as a blob-backed message part")
        is PollDto -> error("Poll is not persisted as a blob-backed attachment part")
    }

private fun MessagePartDto.mimeType(): String =
    when (this) {
        is ImageDto -> mimeType
        is VideoDto -> mimeType
        is FileDto -> mimeType
        is VoiceDto -> mimeType
        is LocationDto -> mimeType
        is ContactDto -> mimeType
        is TextDto -> error("Text has no attachment MIME type")
        is PollDto -> error("Poll has no attachment MIME type")
    }

private fun MessagePartDto.byteSize(): Long =
    when (this) {
        is ImageDto -> byteSize
        is VideoDto -> byteSize
        is FileDto -> byteSize
        is VoiceDto -> byteSize
        is LocationDto -> byteSize
        is ContactDto -> byteSize
        is TextDto -> error("Text has no attachment byte size")
        is PollDto -> error("Poll has no attachment byte size")
    }
