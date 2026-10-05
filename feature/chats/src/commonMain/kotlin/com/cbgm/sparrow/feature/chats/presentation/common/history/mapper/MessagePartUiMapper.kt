package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentSource
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessagePartUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollOptionUi

internal fun List<MessagePart>.toMessagePartsUi(
    attachmentSource: AttachmentSource = AttachmentSource.Message
): List<MessagePartUi> =
    map { part -> part.toMessagePartUi(attachmentSource) }

private fun MessagePart.toMessagePartUi(
    attachmentSource: AttachmentSource
): MessagePartUi =
    when (this) {
        is Text ->
            MessagePartUi.Text(
                text = text,
                isContentFailed = false
            )

        is Image ->
            MessagePartUi.ImageVideo(
                id = id,
                media =
                    MediaItemUi(
                        id = id,
                        type = MediaTypeUi.IMAGE,
                        mimeType = mimeType,
                        width = width,
                        height = height,
                        durationMilliseconds = null
                    ),
                byteSize = byteSize,
                fileName = fileName,
                attachmentSource = attachmentSource
            )

        is Video ->
            MessagePartUi.ImageVideo(
                id = id,
                media =
                    MediaItemUi(
                        id = id,
                        type = MediaTypeUi.VIDEO,
                        mimeType = mimeType,
                        width = width,
                        height = height,
                        durationMilliseconds = durationMilliseconds
                    ),
                byteSize = byteSize,
                fileName = fileName,
                attachmentSource = attachmentSource
            )

        is File ->
            MessagePartUi.File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                attachmentSource = attachmentSource
            )

        is Location ->
            MessagePartUi.Location(
                id = id,
                attachmentSource = attachmentSource
            )

        is Contact ->
            MessagePartUi.Contact(
                id = id,
                attachmentSource = attachmentSource
            )

        is Voice ->
            MessagePartUi.Voice(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = durationMilliseconds,
                attachmentSource = attachmentSource
            )

        is Poll ->
            MessagePartUi.Poll(
                id = id,
                question = question,
                description = description,
                options = options.map {
                    PollOptionUi(
                        id = it.id,
                        text = it.text,
                        voteCount = 0
                    )
                },
                allowMultipleSelection = allowMultipleSelection,
                allowVoteChange = allowVoteChange,
                isAnonymous = isAnonymous,
                attachmentSource = attachmentSource
            )
    }

internal fun MessageBubbleUi.toMessageAttachmentsUi(): List<MessageAttachmentUi> =
    buildList {
        imageVideoParts.forEach { part ->
            add(
                MessageAttachmentUi.ImageVideoAttachmentUi(
                    id = part.id,
                    media = part.media,
                    byteSize = part.byteSize,
                    fileName = part.fileName,
                    source = part.attachmentSource
                )
            )
        }

        fileParts.forEach { part ->
            add(
                MessageAttachmentUi.FileAttachmentUi(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = part.byteSize,
                    fileName = part.fileName,
                    source = part.attachmentSource
                )
            )
        }

        locationPart?.let { part ->
            add(
                MessageAttachmentUi.LocationAttachmentUi(
                    id = part.id,
                    source = part.attachmentSource
                )
            )
        }

        contactPart?.let { part ->
            add(
                MessageAttachmentUi.ContactAttachmentUi(
                    id = part.id,
                    source = part.attachmentSource
                )
            )
        }
    }
