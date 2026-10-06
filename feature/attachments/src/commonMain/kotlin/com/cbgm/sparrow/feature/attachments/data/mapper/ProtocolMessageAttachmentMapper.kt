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
import com.cbgm.sparrow.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType

internal fun MessageAttachment.toMessagePartEntity(
    messageId: String,
    position: Int
): MessagePartEntity =
    MessagePartEntity(
        id = attachmentId,
        messageId = messageId,
        position = position,
        type = type.name
    )

internal fun MessageAttachment.toMessageBlobEntity(
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

internal fun MessagePartDto.toMessagePartEntity(
    messageId: String,
    position: Int
): MessagePartEntity =
    MessagePartEntity(
        id = id,
        messageId = messageId,
        position = position,
        type = attachmentType().name
    )

internal fun MessagePartDto.toMessageBlobEntity(
    deleteCapability: String?,
    localFilePath: String?
): MessageBlobEntity {
    val protocol = toProtocolMessageAttachment()
    return protocol.toMessageBlobEntity(
        deleteCapability = deleteCapability,
        localFilePath = localFilePath
    )
}

internal fun MessagePartDto.toProtocolMessageAttachment(): MessageAttachment {
    val blob = requireNotNull(
        when (this) {
            is ImageDto -> blob
            is VideoDto -> blob
            is FileDto -> blob
            is VoiceDto -> blob
            is LocationDto -> blob
            is ContactDto -> blob
            is TextDto -> error("Text is not a protocol attachment")
            is PollDto -> error("Poll is not a protocol attachment")
        }
    ) { "Message part $id has not been uploaded yet" }

    return MessageAttachment(
        attachmentId = id,
        type = attachmentType(),
        mimeType = when (this) {
            is ImageDto -> mimeType
            is VideoDto -> mimeType
            is FileDto -> mimeType
            is VoiceDto -> mimeType
            is LocationDto -> mimeType
            is ContactDto -> mimeType
            is TextDto -> error("Text is not a protocol attachment")
            is PollDto -> error("Poll is not a protocol attachment")
        },
        byteSize = when (this) {
            is ImageDto -> byteSize
            is VideoDto -> byteSize
            is FileDto -> byteSize
            is VoiceDto -> byteSize
            is LocationDto -> byteSize
            is ContactDto -> byteSize
            is TextDto -> error("Text is not a protocol attachment")
            is PollDto -> error("Poll is not a protocol attachment")
        },
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
        blob = blob.toProtocol()
    )
}

internal fun MessagePartEntity.toProtocolMessageAttachment(blob: MessageBlobEntity): MessageAttachment =
    MessageAttachment(
        attachmentId = id,
        type = MessageAttachmentType.valueOf(type),
        mimeType = blob.mimeType,
        byteSize = blob.byteSize,
        fileName = blob.fileName,
        width = blob.width,
        height = blob.height,
        durationMilliseconds = blob.durationMilliseconds,
        blob = blob.toEncryptedBlobReferenceDto().toProtocol()
    )

private fun MessagePartDto.attachmentType(): MessageAttachmentType =
    when (this) {
        is ImageDto -> MessageAttachmentType.IMAGE
        is VideoDto -> MessageAttachmentType.VIDEO
        is FileDto -> MessageAttachmentType.FILE
        is VoiceDto -> MessageAttachmentType.VOICE
        is LocationDto -> MessageAttachmentType.LOCATION
        is ContactDto -> MessageAttachmentType.CONTACT
        is TextDto -> error("Text is not a protocol attachment")
        is PollDto -> error("Poll is not a protocol attachment")
    }
