package com.cbgm.sparrow.feature.chats.data.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.chats.data.model.ImageVideoTypeDto
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.ImageVideoType
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart

internal fun List<MessageAttachment>.toMessagePartDtos(): List<MessagePartDto> =
    map(MessageAttachment::toMessagePartDto)

private fun MessageAttachment.toMessagePartDto(): MessagePartDto =
    when (this) {
        is MessageAttachment.Image ->
            MessagePartDto.ImageVideoDto(
                id = id,
                type = ImageVideoTypeDto.IMAGE,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = null,
                width = width,
                height = height,
                durationMilliseconds = null,
                localFilePath = localFilePath
            )

        is MessageAttachment.Video ->
            MessagePartDto.ImageVideoDto(
                id = id,
                type = ImageVideoTypeDto.VIDEO,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = null,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath
            )

        is MessageAttachment.File ->
            MessagePartDto.FileDto(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath
            )

        is MessageAttachment.Location -> MessagePartDto.LocationDto(id = id)
        is MessageAttachment.Contact -> MessagePartDto.ContactDto(id = id)
        is MessageAttachment.Voice ->
            MessagePartDto.VoiceDto(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds
            )
    }

internal fun MessagePartDto.toMessagePart(): MessagePart =
    when (this) {
        is MessagePartDto.TextDto -> MessagePart.Text(text = text)
        is MessagePartDto.ImageVideoDto ->
            MessagePart.ImageVideo(
                id = id,
                type = when (type) {
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
        is MessagePartDto.LocationDto -> MessagePart.Location(id = id)
        is MessagePartDto.ContactDto -> MessagePart.Contact(id = id)
        is MessagePartDto.VoiceDto ->
            MessagePart.Voice(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds
            )
    }
