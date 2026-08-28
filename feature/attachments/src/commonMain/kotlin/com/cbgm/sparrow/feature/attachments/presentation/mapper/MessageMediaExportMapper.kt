package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem

internal fun MessageAttachmentUi.ImageVideoAttachment.toMediaExportItem(): MediaExportItem =
    MediaExportItem(
        id = id,
        type =
            when (type) {
                MessageAttachmentType.IMAGE -> MediaContentType.IMAGE
                MessageAttachmentType.VIDEO -> MediaContentType.VIDEO
                else -> error("Unsupported image/video attachment type: $type")
            },
        mimeType = mimeType,
        bytes = requireNotNull(bytes) { "Media attachment must be loaded before export" }
    )
