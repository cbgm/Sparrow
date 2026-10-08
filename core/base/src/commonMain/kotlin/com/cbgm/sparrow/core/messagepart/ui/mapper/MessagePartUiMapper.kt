package com.cbgm.sparrow.core.messagepart.ui.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePartSource
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.messagepart.ui.model.ContactUi
import com.cbgm.sparrow.core.messagepart.ui.model.FileUi
import com.cbgm.sparrow.core.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.messagepart.ui.model.LocationUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartSourceUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.PollOptionUi
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.messagepart.ui.model.TextUi
import com.cbgm.sparrow.core.messagepart.ui.model.VideoUi
import com.cbgm.sparrow.core.messagepart.ui.model.VoiceUi

fun MessagePart.toMessagePartUi(source: MessagePartSource = MessagePartSource.Message): MessagePartUi =
    when (this) {
        is Text -> toTextUi(source)
        is Image -> toImageUi(source)
        is Video -> toVideoUi(source)
        is File -> toFileUi(source)
        is Voice -> toVoiceUi(source)
        is Location -> toLocationUi(source)
        is Contact -> toContactUi(source)
        is Poll -> toPollUi(source)
    }

fun MessagePartSource.toMessagePartSourceUi(): MessagePartSourceUi =
    when (this) {
        MessagePartSource.Message -> MessagePartSourceUi.Message
        is MessagePartSource.GroupPin -> MessagePartSourceUi.GroupPin(groupId)
    }

private fun Text.toTextUi(source: MessagePartSource): TextUi =
    TextUi(
        id = id,
        text = text,
        source = source.toMessagePartSourceUi()
    )

private fun Image.toImageUi(source: MessagePartSource): ImageUi =
    ImageUi(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        fileName = fileName,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath,
        source = source.toMessagePartSourceUi()
    )

private fun Video.toVideoUi(source: MessagePartSource): VideoUi =
    VideoUi(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds,
        localFilePath = localFilePath,
        thumbnailFilePath = thumbnailFilePath,
        source = source.toMessagePartSourceUi()
    )

private fun File.toFileUi(source: MessagePartSource): FileUi =
    FileUi(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        localFilePath = localFilePath,
        source = source.toMessagePartSourceUi()
    )

private fun Voice.toVoiceUi(source: MessagePartSource): VoiceUi =
    VoiceUi(
        id = id,
        mimeType = mimeType,
        byteSize = byteSize,
        durationMilliseconds = durationMilliseconds,
        source = source.toMessagePartSourceUi()
    )

private fun Location.toLocationUi(source: MessagePartSource): LocationUi =
    LocationUi(
        id = id,
        source = source.toMessagePartSourceUi()
    )

private fun Contact.toContactUi(source: MessagePartSource): ContactUi =
    ContactUi(
        id = id,
        source = source.toMessagePartSourceUi()
    )

private fun Poll.toPollUi(source: MessagePartSource): PollUi =
    PollUi(
        id = id,
        question = question,
        description = description,
        options = options.map { PollOptionUi(id = it.id, text = it.text, voterIds = it.voterIds) },
        images = images.map { it.toImageUi(source) },
        allowMultipleSelection = allowMultipleSelection,
        allowVoteChange = allowVoteChange,
        isAnonymous = isAnonymous,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        closedAtEpochMilliseconds = closedAtEpochMilliseconds,
        source = source.toMessagePartSourceUi()
    )
