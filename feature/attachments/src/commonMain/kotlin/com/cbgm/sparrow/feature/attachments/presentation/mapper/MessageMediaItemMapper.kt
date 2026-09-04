package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun MessageAttachmentUi.ImageVideoAttachmentUi.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type =
            when (type) {
                MessageAttachmentType.IMAGE -> MediaType.IMAGE
                MessageAttachmentType.VIDEO -> MediaType.VIDEO
                else -> error("Unsupported image/video attachment type: $type")
            },
        mimeType = mimeType,
        localFilePath = localFilePath,
        bytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
