package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.data.database.model.MessageBlobPartRowDto
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType

fun List<MessageBlobPartRowDto>.toMessageAttachmentsByMessageId(
    resolveLocalFilePath: (String) -> String?
): Map<String, List<MessageAttachment>> =
    groupBy(MessageBlobPartRowDto::messageId)
        .mapValues { (_, attachments) ->
            attachments
                .sortedBy(MessageBlobPartRowDto::position)
                .map { row -> row.toMessageAttachment(resolveLocalFilePath) }
        }

private fun MessageBlobPartRowDto.toMessageAttachment(
    resolveLocalFilePath: (String) -> String?
): MessageAttachment =
    when (MessageAttachmentType.valueOf(type)) {
        MessageAttachmentType.IMAGE ->
            MessageAttachment.Image(
                id = partId,
                mimeType = mimeType,
                byteSize = byteSize,
                width = requireNotNull(width),
                height = requireNotNull(height),
                localFilePath = localFilePath?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.VIDEO ->
            MessageAttachment.Video(
                id = partId,
                mimeType = mimeType,
                byteSize = byteSize,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.FILE ->
            MessageAttachment.File(
                id = partId,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: partId,
                localFilePath = localFilePath?.let(resolveLocalFilePath)
            )

        MessageAttachmentType.VOICE ->
            MessageAttachment.Voice(
                id = partId,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = requireNotNull(durationMilliseconds)
            )

        MessageAttachmentType.LOCATION -> MessageAttachment.Location(partId)
        MessageAttachmentType.CONTACT -> MessageAttachment.Contact(partId)
    }
