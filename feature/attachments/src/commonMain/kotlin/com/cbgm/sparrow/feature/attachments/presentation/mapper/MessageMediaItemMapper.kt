package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun MessageAttachmentUi.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type =
            when (type) {
                MessageAttachmentType.IMAGE -> MediaType.IMAGE
                MessageAttachmentType.VIDEO -> MediaType.VIDEO
                MessageAttachmentType.LOCATION -> error("Location attachment cannot be mapped to a media item")
                MessageAttachmentType.FILE -> error("File attachment cannot be mapped to a media item")
            },
        mimeType = mimeType,
        bytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
