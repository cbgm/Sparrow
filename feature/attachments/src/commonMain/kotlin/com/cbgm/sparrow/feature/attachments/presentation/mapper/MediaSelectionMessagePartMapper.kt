package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

fun MediaSelectionUi.toMessagePart(): MessagePart =
    when (this) {
        is VisualMediaSelectionUi ->
            when (type) {
                MediaTypeUi.IMAGE ->
                    Image(
                        id = id,
                        mimeType = mimeType,
                        byteSize = byteSize,
                        width = requireNotNull(width),
                        height = requireNotNull(height),
                        localFilePath = localFilePath,
                        thumbnailFilePath = thumbnailFilePath
                    )

                MediaTypeUi.VIDEO ->
                    Video(
                        id = id,
                        mimeType = mimeType,
                        byteSize = byteSize,
                        width = width,
                        height = height,
                        durationMilliseconds = durationMilliseconds,
                        localFilePath = localFilePath,
                        thumbnailFilePath = thumbnailFilePath
                    )
            }

        is FileMediaSelectionUi ->
            File(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                localFilePath = localFilePath
            )
    }
