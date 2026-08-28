package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment

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
): MessageAttachment {
    val attachmentType = MessageAttachmentType.valueOf(type)

    return MessageAttachment(
        id = id,
        type = attachmentType,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds,
        localFilePath =
            if (attachmentType == MessageAttachmentType.FILE) {
                localFileName?.let(resolveLocalFilePath)
            } else {
                null
            }
    )
}
