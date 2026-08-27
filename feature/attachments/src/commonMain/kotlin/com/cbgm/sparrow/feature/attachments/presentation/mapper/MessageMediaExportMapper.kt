package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem

internal fun MessageMediaAttachmentUi.toMediaExportItem(): MediaExportItem =
    MediaExportItem(
        id = id,
        type =
            when (type) {
                MessageAttachmentType.IMAGE -> MediaContentType.IMAGE
                MessageAttachmentType.VIDEO -> MediaContentType.VIDEO
                MessageAttachmentType.LOCATION -> error("Location attachment cannot be exported to the camera roll")
                MessageAttachmentType.FILE -> error("File attachment cannot be exported to the camera roll")
            },
        mimeType = mimeType,
        bytes = requireNotNull(bytes) { "Media attachment must be loaded before export" }
    )
