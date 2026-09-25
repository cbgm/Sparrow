package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.feature.attachments.data.model.OutgoingMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.PreparedMessageAttachment

internal fun OutgoingMessageAttachment.toDto(): OutgoingMessageAttachmentDto =
    OutgoingMessageAttachmentDto(
        id = id,
        type = type,
        bytes = bytes,
        mimeType = mimeType,
        fileName = fileName,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

internal fun PreparedMessageAttachmentDto.toDomain(): PreparedMessageAttachment =
    PreparedMessageAttachment(
        attachment = attachment,
        deleteCapability = deleteCapability,
        localFileName = localFileName,
        payloadBytes = payloadBytes
    )

internal fun PreparedMessageAttachment.toDto(): PreparedMessageAttachmentDto =
    PreparedMessageAttachmentDto(
        attachment = attachment,
        deleteCapability = deleteCapability,
        localFileName = localFileName,
        payloadBytes = payloadBytes
    )
