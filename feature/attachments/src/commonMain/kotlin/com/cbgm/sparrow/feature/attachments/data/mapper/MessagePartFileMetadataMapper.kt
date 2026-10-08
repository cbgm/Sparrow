package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto

internal data class MessagePartFileMetadata(
    val isMedia: Boolean,
    val mimeType: String
)

internal fun MessagePartDto.toSavedFileMetadata(): MessagePartFileMetadata? =
    when (this) {
        is ImageDto -> MessagePartFileMetadata(isMedia = true, mimeType = mimeType)
        is VideoDto -> MessagePartFileMetadata(isMedia = true, mimeType = mimeType)
        is FileDto -> MessagePartFileMetadata(isMedia = false, mimeType = mimeType)
        else -> null
    }
