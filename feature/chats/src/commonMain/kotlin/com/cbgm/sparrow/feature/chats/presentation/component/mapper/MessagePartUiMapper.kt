package com.cbgm.sparrow.feature.chats.presentation.component.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import com.cbgm.sparrow.feature.chats.domain.model.ImageVideoType
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.presentation.component.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun List<MessagePart>.toMessagePartsUi(
    attachmentBytes: Map<String, ByteArray>
): List<MessagePartUi> =
    map { part ->
        part.toMessagePartUi(attachmentBytes)
    }

private fun MessagePart.toMessagePartUi(
    attachmentBytes: Map<String, ByteArray>
): MessagePartUi =
    when (this) {
        is MessagePart.Text ->
            MessagePartUi.Text(
                text = text,
                isContentFailed = false
            )

        is MessagePart.ImageVideo ->
            MessagePartUi.ImageVideo(
                id = id,
                type =
                    when (type) {
                        ImageVideoType.IMAGE -> ImageVideoTypeUi.IMAGE
                        ImageVideoType.VIDEO -> ImageVideoTypeUi.VIDEO
                    },
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath,
                bytes = attachmentBytes[id]
            )

        is MessagePart.File ->
            MessagePartUi.File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath,
                bytes = attachmentBytes[id]
            )

        is MessagePart.Location ->
            MessagePartUi.Location(
                id = id,
                location = attachmentBytes[id]?.let(LocationAttachmentPayload::decode)
            )

        is MessagePart.Contact ->
            MessagePartUi.Contact(
                id = id,
                contact = attachmentBytes[id]?.let(ContactAttachmentPayload::decode)
            )
    }

fun MessagePartUi.ImageVideo.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type =
            when (type) {
                ImageVideoTypeUi.IMAGE -> MediaType.IMAGE
                ImageVideoTypeUi.VIDEO -> MediaType.VIDEO
            },
        mimeType = mimeType,
        bytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

internal fun MessageBubbleUi.toMessageAttachmentsUi(): List<MessageAttachmentUi> =
    buildList {
        imageVideoParts.forEach { part ->
            add(
                MessageAttachmentUi.ImageVideoAttachmentUi(
                    id = part.id,
                    type =
                        when (part.type) {
                            ImageVideoTypeUi.IMAGE -> MessageAttachmentType.IMAGE
                            ImageVideoTypeUi.VIDEO -> MessageAttachmentType.VIDEO
                        },
                    mimeType = part.mimeType,
                    byteSize = part.byteSize,
                    fileName = part.fileName,
                    width = part.width,
                    height = part.height,
                    durationMilliseconds = part.durationMilliseconds,
                    localFilePath = part.localFilePath,
                    bytes = part.bytes
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
                    localFilePath = part.localFilePath,
                    bytes = part.bytes
                )
            )
        }

        locationPart?.location?.let { location ->
            add(
                MessageAttachmentUi.LocationAttachmentUi(
                    id = locationPart.id,
                    location = location
                )
            )
        }

        contactPart?.contact?.let { contact ->
            add(
                MessageAttachmentUi.ContactAttachmentUi(
                    id = contactPart.id,
                    contact = contact
                )
            )
        }
    }
