package com.cbgm.sparrow.feature.chats.data.attachment

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaAttachment
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType

internal fun List<MessageAttachmentEntity>.toDomainAttachmentsByMessageId(): Map<String, List<MessageMediaAttachment>> =
    filter { entity ->
        entity.type == MessageAttachmentType.IMAGE.name || entity.type == MessageAttachmentType.VIDEO.name
    }.groupBy(MessageAttachmentEntity::messageId)
        .mapValues { (_, attachments) ->
            attachments
                .sortedBy(MessageAttachmentEntity::position)
                .mapNotNull { entity -> entity.toDomainMediaAttachment() }
        }

private fun MessageAttachmentEntity.toDomainMediaAttachment(): MessageMediaAttachment? {
    val mediaType =
        when (type) {
            MessageAttachmentType.IMAGE.name -> MessageMediaType.IMAGE
            MessageAttachmentType.VIDEO.name -> MessageMediaType.VIDEO
            else -> return null
        }
    return MessageMediaAttachment(
        id = id,
        type = mediaType,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
}
