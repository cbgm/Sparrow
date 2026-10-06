package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
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

internal fun PreparedMessageAttachmentDto.toMessageBlobEntity(): MessageBlobEntity =
    attachment.toMessageBlobEntity(
        deleteCapability = deleteCapability,
        localFilePath = localFileName
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
