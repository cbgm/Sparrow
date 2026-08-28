package com.cbgm.sparrow.feature.chats.presentation.component.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import com.cbgm.sparrow.feature.chats.presentation.component.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun List<MessageAttachment>.toMessagePartsUi(
    attachmentBytes: Map<String, ByteArray>
): List<MessagePartUi> =
    mapNotNull { attachment ->
        attachment.toMessagePartUi(attachmentBytes[attachment.id])
    }

private fun MessageAttachment.toMessagePartUi(bytes: ByteArray?): MessagePartUi? =
    when (type) {
        MessageAttachmentType.IMAGE,
        MessageAttachmentType.VIDEO ->
            MessagePartUi.ImageVideoUi(
                id = id,
                type =
                    when (type) {
                        MessageAttachmentType.IMAGE -> ImageVideoTypeUi.IMAGE
                        MessageAttachmentType.VIDEO -> ImageVideoTypeUi.VIDEO
                        else -> error("Unsupported image/video attachment type: $type")
                    },
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath,
                bytes = bytes
            )

        MessageAttachmentType.FILE ->
            MessagePartUi.FileUi(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: id,
                localFilePath = localFilePath,
                bytes = bytes
            )

        MessageAttachmentType.LOCATION ->
            bytes
                ?.let(LocationAttachmentPayload::decode)
                ?.let { location ->
                    MessagePartUi.LocationUi(
                        id = id,
                        location = location
                    )
                }
    }

fun MessagePartUi.ImageVideoUi.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type =
            when (type) {
                ImageVideoTypeUi.IMAGE -> MediaType.IMAGE
                ImageVideoTypeUi.VIDEO -> MediaType.VIDEO
            },
        mimeType = mimeType,
        bytes = bytes,
        thumbnailBytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

internal fun MessageBubbleUi.toMessageAttachmentUi(): List<MessageAttachmentUi> =
    buildList {
        imageVideoParts.forEach { part ->
            add(
                MessageAttachmentUi.ImageVideoAttachment(
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
                MessageAttachmentUi.FileAttachment(
                    id = part.id,
                    mimeType = part.mimeType,
                    byteSize = part.byteSize,
                    fileName = part.fileName,
                    localFilePath = part.localFilePath,
                    bytes = part.bytes
                )
            )
        }

        locationPart?.let { part ->
            add(
                MessageAttachmentUi.LocationAttachment(
                    id = part.id,
                    location = part.location
                )
            )
        }
    }
