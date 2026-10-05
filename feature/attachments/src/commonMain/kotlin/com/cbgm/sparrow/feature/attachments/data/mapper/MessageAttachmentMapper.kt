package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment

internal fun Map<String, List<MessagePartDto>>.toMessageAttachmentsByMessageId(): Map<String, List<MessageAttachment>> =
    mapValues { (_, parts) -> parts.mapNotNull(MessagePartDto::toMessageAttachmentOrNull) }

private fun MessagePartDto.toMessageAttachmentOrNull(): MessageAttachment? =
    when (this) {
        is ImageDto ->
            MessageAttachment.Image(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                width = requireNotNull(width),
                height = requireNotNull(height),
                localFilePath = localFilePath
            )

        is VideoDto ->
            MessageAttachment.Video(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath
            )

        is FileDto ->
            MessageAttachment.File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath
            )

        is VoiceDto ->
            MessageAttachment.Voice(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds
            )

        is LocationDto -> MessageAttachment.Location(id)
        is ContactDto -> MessageAttachment.Contact(id)
        else -> null
    }
