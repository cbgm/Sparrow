package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.attachments.domain.model.MessageFileAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaAttachment

fun List<MessageAttachmentEntity>.toDomainMediaByMessageId(): Map<String, List<MessageMediaAttachment>> =
    filter { entity ->
        entity.type == MessageAttachmentType.IMAGE.name ||
            entity.type == MessageAttachmentType.VIDEO.name ||
            entity.type == MessageAttachmentType.LOCATION.name
    }.groupBy(MessageAttachmentEntity::messageId)
        .mapValues { (_, attachments) ->
            attachments
                .sortedBy(MessageAttachmentEntity::position)
                .mapNotNull(MessageAttachmentEntity::toDomainMediaAttachment)
        }

fun List<MessageAttachmentEntity>.toDomainFilesByMessageId(
    resolveLocalFilePath: (String) -> String?
): Map<String, List<MessageFileAttachment>> =
    filter { entity -> entity.type == MessageAttachmentType.FILE.name }
        .groupBy(MessageAttachmentEntity::messageId)
        .mapValues { (_, attachments) ->
            attachments
                .sortedBy(MessageAttachmentEntity::position)
                .map { entity ->
                    MessageFileAttachment(
                        id = entity.id,
                        mimeType = entity.mimeType,
                        byteSize = entity.byteSize,
                        fileName = entity.fileName ?: entity.id,
                        localFilePath = entity.localFileName?.let(resolveLocalFilePath)
                    )
                }
        }

private fun MessageAttachmentEntity.toDomainMediaAttachment(): MessageMediaAttachment? {
    val attachmentType =
        when (type) {
            MessageAttachmentType.IMAGE.name -> MessageAttachmentType.IMAGE
            MessageAttachmentType.VIDEO.name -> MessageAttachmentType.VIDEO
            MessageAttachmentType.LOCATION.name -> MessageAttachmentType.LOCATION
            else -> return null
        }
    return MessageMediaAttachment(
        id = id,
        type = attachmentType,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
}
