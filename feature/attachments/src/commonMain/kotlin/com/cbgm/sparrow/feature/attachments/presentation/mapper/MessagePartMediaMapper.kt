package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.messagepart.ui.model.VideoUi
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem
import com.cbgm.sparrow.feature.media.presentation.mapper.toDomain
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi

fun MessagePartUi.toMediaItemUi(): MediaItemUi =
    when (this) {
        is ImageUi ->
            MediaItemUi(
                id = id,
                type = MediaTypeUi.IMAGE,
                mimeType = mimeType,
                localFilePath = localFilePath,
                thumbnailFilePath = thumbnailFilePath,
                width = width,
                height = height
            )

        is VideoUi ->
            MediaItemUi(
                id = id,
                type = MediaTypeUi.VIDEO,
                mimeType = mimeType,
                localFilePath = localFilePath,
                thumbnailFilePath = thumbnailFilePath,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        else -> error("Message part $id is not visual media")
    }

internal fun MessagePartUi.toMediaExportItem(localFilePath: String): MediaExportItem {
    val media = toMediaItemUi()
    return MediaExportItem(
        id = id,
        type = media.type.toDomain(),
        mimeType = media.mimeType,
        localFilePath = localFilePath
    )
}
