package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.data.model.OutgoingMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachmentDto
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.PreparedMessageAttachment

internal fun OutgoingMessageAttachment.toDto(): OutgoingMessageAttachmentDto =
    when (this) {
        is OutgoingMessageAttachment.Image ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.IMAGE,
                bytes = bytes,
                mimeType = mimeType,
                fileName = null,
                width = width,
                height = height,
                durationMilliseconds = null
            )

        is OutgoingMessageAttachment.Video ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.VIDEO,
                bytes = bytes,
                mimeType = mimeType,
                fileName = null,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        is OutgoingMessageAttachment.File ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.FILE,
                bytes = bytes,
                mimeType = mimeType,
                fileName = fileName,
                width = null,
                height = null,
                durationMilliseconds = null
            )

        is OutgoingMessageAttachment.Voice ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.VOICE,
                bytes = bytes,
                mimeType = mimeType,
                fileName = null,
                width = null,
                height = null,
                durationMilliseconds = durationMilliseconds
            )

        is OutgoingMessageAttachment.Location ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.LOCATION,
                bytes = bytes,
                mimeType = LOCATION_MIME_TYPE,
                fileName = null,
                width = null,
                height = null,
                durationMilliseconds = null
            )

        is OutgoingMessageAttachment.Contact ->
            OutgoingMessageAttachmentDto(
                id = id,
                type = MessageAttachmentType.CONTACT,
                bytes = bytes,
                mimeType = CONTACT_MIME_TYPE,
                fileName = null,
                width = null,
                height = null,
                durationMilliseconds = null
            )
    }

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
