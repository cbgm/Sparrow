package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.model.GalleryMediaSelection
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

internal fun MessageMediaAttachmentUi.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = type.toMediaType(),
        mimeType = mimeType,
        bytes = bytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

internal fun GalleryMediaSelection.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = type.toMediaType(),
        mimeType = mimeType,
        bytes = bytes,
        thumbnailBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

private fun MessageMediaType.toMediaType(): MediaType =
    when (this) {
        MessageMediaType.IMAGE -> MediaType.IMAGE
        MessageMediaType.VIDEO -> MediaType.VIDEO
    }
