package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem
import com.cbgm.sparrow.feature.media.presentation.mapper.toDomain

internal fun MessageAttachmentUi.ImageVideoAttachmentUi.toMediaExportItem(
    localFilePath: String
): MediaExportItem =
    MediaExportItem(
        id = id,
        type = media.type.toDomain(),
        mimeType = media.mimeType,
        localFilePath = localFilePath
    )
