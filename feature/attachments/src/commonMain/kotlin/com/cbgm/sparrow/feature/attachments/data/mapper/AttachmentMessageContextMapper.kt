package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.data.database.entity.AttachmentMessageContextEntity
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentMessageContextDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext

internal fun AttachmentMessageContext.toDto(): AttachmentMessageContextDto =
    AttachmentMessageContextDto(
        conversationId = conversationId,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        displayName = displayName,
        isGroup = isGroup,
        isMine = isMine,
        senderContactId = senderContactId
    )

internal fun AttachmentMessageContextDto.toEntity(messageId: String): AttachmentMessageContextEntity =
    AttachmentMessageContextEntity(
        messageId = messageId,
        conversationId = conversationId,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        displayName = displayName.ifBlank { conversationId },
        isGroup = isGroup,
        isMine = isMine,
        senderContactId = senderContactId
    )
