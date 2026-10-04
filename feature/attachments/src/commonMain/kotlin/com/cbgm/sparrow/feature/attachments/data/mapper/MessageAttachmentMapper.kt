package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType

fun List<MessageAttachmentEntity>.toMessageAttachmentsByMessageId(
    resolveLocalFilePath: (String) -> String?
): Map<String, List<MessageAttachment>> =
    groupBy(MessageAttachmentEntity::messageId)
        .mapValues { (_, attachments) ->
            attachments
                .sortedBy(MessageAttachmentEntity::position)
                .map { entity -> entity.toMessageAttachment(resolveLocalFilePath) }
        }

private fun MessageAttachmentEntity.toMessageAttachment(
    resolveLocalFilePath: (String) -> String?
): MessageAttachment =
    when (MessageAttachmentType.valueOf(type)) {
        MessageAttachmentType.IMAGE ->
            MessageAttachment.Image(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                width = requireNotNull(width),
                height = requireNotNull(height),
                localFilePath = localFileName?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.VIDEO ->
            MessageAttachment.Video(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFileName?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.FILE ->
            MessageAttachment.File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: id,
                localFilePath = localFileName?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.VOICE ->
            MessageAttachment.Voice(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = requireNotNull(durationMilliseconds)
            )

        MessageAttachmentType.LOCATION -> MessageAttachment.Location(id)
        MessageAttachmentType.CONTACT -> MessageAttachment.Contact(id)
    }
