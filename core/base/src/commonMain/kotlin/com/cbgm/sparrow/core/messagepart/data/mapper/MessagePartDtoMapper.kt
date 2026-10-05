package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollOption
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice

fun MessagePartDto.toMessagePart(): MessagePart =
    when (this) {
        is TextDto -> toText()
        is ImageDto -> toImage()
        is VideoDto -> toVideo()
        is FileDto -> toFile()
        is VoiceDto -> toVoice()
        is LocationDto -> toLocation()
        is ContactDto -> toContact()
        is PollDto -> toPoll()
    }

private fun TextDto.toText(): Text =
    Text(
        id = id,
        text = text
    )

private fun ImageDto.toImage(): Image =
    Image(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        fileName = fileName,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath
    )

private fun VideoDto.toVideo(): Video =
    Video(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath
    )

private fun FileDto.toFile(): File =
    File(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        localFilePath = localFilePath
    )

private fun VoiceDto.toVoice(): Voice =
    Voice(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        durationMilliseconds = durationMilliseconds
    )

private fun LocationDto.toLocation(): Location =
    Location(id = id)

private fun ContactDto.toContact(): Contact =
    Contact(id = id)

private fun PollDto.toPoll(): Poll =
    Poll(
        id = id,
        question = question,
        description = description,
        options = options.map { PollOption(id = it.id, text = it.text) },
        images = images.map(ImageDto::toImage),
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        closedAtEpochMilliseconds = closedAtEpochMilliseconds
    )
