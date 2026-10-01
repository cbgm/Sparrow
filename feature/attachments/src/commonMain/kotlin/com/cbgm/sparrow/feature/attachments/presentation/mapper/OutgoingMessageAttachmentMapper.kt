package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

suspend fun MediaSelectionUi.toOutgoingMessageAttachment(
    files: MediaSelectionFileRepository
): OutgoingMessageAttachment =
    when (this) {
        is VisualMediaSelectionUi ->
            when (type) {
                MediaTypeUi.IMAGE ->
                    OutgoingMessageAttachment.Image(
                        id = id,
                        bytes = files.read(localFilePath),
                        mimeType = mimeType,
                        width = requireNotNull(width),
                        height = requireNotNull(height)
                    )

                MediaTypeUi.VIDEO ->
                    OutgoingMessageAttachment.Video(
                        id = id,
                        bytes = files.read(localFilePath),
                        mimeType = mimeType,
                        width = width,
                        height = height,
                        durationMilliseconds = durationMilliseconds
                    )
            }

        is FileMediaSelectionUi ->
            OutgoingMessageAttachment.File(
                id = id,
                bytes = files.read(localFilePath),
                mimeType = mimeType,
                fileName = fileName
            )
    }
