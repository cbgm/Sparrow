package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.PollOptionDto
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
import java.util.concurrent.ConcurrentHashMap

private val dtoRegistry = ConcurrentHashMap<String, MessagePartDto>()

fun MessagePartDto.toMessagePart(): MessagePart {
    dtoRegistry[id] = this
    return when (this) {
        is TextDto -> toText()
        is ImageDto -> toImage()
        is VideoDto -> toVideo()
        is FileDto -> toFile()
        is VoiceDto -> toVoice()
        is LocationDto -> toLocation()
        is ContactDto -> toContact()
        is PollDto -> toPoll()
    }
}

fun MessagePart.toDto(): MessagePartDto {
    val cached = dtoRegistry[id]
    return when (this) {
        is Text -> cached as? TextDto ?: TextDto(id = id, text = text)
        is Image -> (cached as? ImageDto)?.copy(
            mimeType = mimeType,
            byteSize = byteSize,
            width = width,
            height = height,
            fileName = fileName,
            localFilePath = localFilePath,
            thumbnailFilePath = thumbnailFilePath
        ) ?: ImageDto(
            id = id,
            mimeType = mimeType,
            byteSize = byteSize,
            width = width,
            height = height,
            fileName = fileName,
            localFilePath = localFilePath,
            thumbnailFilePath = thumbnailFilePath
        )
        is Video -> (cached as? VideoDto)?.copy(
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            width = width,
            height = height,
            durationMilliseconds = durationMilliseconds,
            localFilePath = localFilePath,
            thumbnailFilePath = thumbnailFilePath
        ) ?: VideoDto(
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
        is File -> (cached as? FileDto)?.copy(
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            localFilePath = localFilePath
        ) ?: FileDto(
            id = id,
            mimeType = mimeType,
            byteSize = byteSize,
            fileName = fileName,
            localFilePath = localFilePath
        )
        is Voice -> (cached as? VoiceDto)?.copy(
            mimeType = mimeType,
            byteSize = byteSize,
            durationMilliseconds = durationMilliseconds,
            localFilePath = localFilePath
        ) ?: VoiceDto(
            id = id,
            mimeType = mimeType,
            byteSize = byteSize,
            durationMilliseconds = durationMilliseconds,
            localFilePath = localFilePath
        )
        is Location -> (cached as? LocationDto)?.copy(
            mimeType = LOCATION_MIME_TYPE
        ) ?: LocationDto(id = id, mimeType = LOCATION_MIME_TYPE, byteSize = 0L)
        is Contact -> (cached as? ContactDto)?.copy(
            mimeType = CONTACT_MIME_TYPE
        ) ?: ContactDto(id = id, mimeType = CONTACT_MIME_TYPE, byteSize = 0L)
        is Poll -> PollDto(
            id = id,
            question = question,
            description = description,
            options = options.map { PollOptionDto(it.id, it.text, it.voterIds) },
            images = images.map { (it.toDto() as? ImageDto) ?: ImageDto(id = it.id, mimeType = it.mimeType, byteSize = it.byteSize) },
            allowMultipleSelection = allowMultipleSelection,
            allowVoteChange = allowVoteChange,
            isAnonymous = isAnonymous,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            closedAtEpochMilliseconds = closedAtEpochMilliseconds
        )
    }
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
        durationMilliseconds = durationMilliseconds,
        localFilePath = localFilePath
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
        options = options.map { PollOption(id = it.id, text = it.text, voterIds = it.voterIds) },
        images = images.map { it.toMessagePart() as Image },
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        closedAtEpochMilliseconds = closedAtEpochMilliseconds
    )
