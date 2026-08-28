package com.cbgm.sparrow.feature.chats.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.chats.data.model.ImageVideoTypeDto
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.ImageVideoType
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart

internal fun List<MessageAttachment>.toMessagePartDtos(): List<MessagePartDto> =
    map { attachment -> attachment.toMessagePartDto() }

private fun MessageAttachment.toMessagePartDto(): MessagePartDto =
    when (type) {
        MessageAttachmentType.IMAGE,
        MessageAttachmentType.VIDEO ->
            MessagePartDto.ImageVideoDto(
                id = id,
                type =
                    when (type) {
                        MessageAttachmentType.IMAGE -> ImageVideoTypeDto.IMAGE
                        MessageAttachmentType.VIDEO -> ImageVideoTypeDto.VIDEO
                        else -> error("Unsupported image/video attachment type: $type")
                    },
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath
            )

        MessageAttachmentType.FILE ->
            MessagePartDto.FileDto(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: id,
                localFilePath = localFilePath
            )

        MessageAttachmentType.LOCATION ->
            MessagePartDto.LocationDto(id = id)

        MessageAttachmentType.CONTACT ->
            MessagePartDto.ContactDto(id = id)
    }

internal fun MessagePartDto.toMessagePart(): MessagePart =
    when (this) {
        is MessagePartDto.TextDto ->
            MessagePart.Text(text = text)

        is MessagePartDto.ImageVideoDto ->
            MessagePart.ImageVideo(
                id = id,
                type =
                    when (type) {
                        ImageVideoTypeDto.IMAGE -> ImageVideoType.IMAGE
                        ImageVideoTypeDto.VIDEO -> ImageVideoType.VIDEO
                    },
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath
            )

        is MessagePartDto.FileDto ->
            MessagePart.File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath
            )

        is MessagePartDto.LocationDto ->
            MessagePart.Location(id = id)

        is MessagePartDto.ContactDto ->
            MessagePart.Contact(id = id)
    }
