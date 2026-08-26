package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem

internal fun MessageMediaAttachmentUi.toMediaExportItem(): MediaExportItem =
    MediaExportItem(
        id = id,
        type = type,
        mimeType = mimeType,
        bytes = requireNotNull(bytes) { "Media attachment must be loaded before export" }
    )
