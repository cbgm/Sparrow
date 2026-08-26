package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun MessageMediaAttachmentUi.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = when (type) {
            MediaContentType.IMAGE -> MediaType.IMAGE
            MediaContentType.VIDEO -> MediaType.VIDEO
        },
        mimeType = mimeType,
        bytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
