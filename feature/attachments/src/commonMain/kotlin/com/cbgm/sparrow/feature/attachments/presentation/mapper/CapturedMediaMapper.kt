package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia

internal fun CapturedMedia.toMediaSelection(): MediaSelection =
    MediaSelection(
        id =
            IdGenerator.generate(
                prefix =
                    when (type) {
                        CameraCaptureType.PHOTO -> "camera-image"
                        CameraCaptureType.VIDEO -> "camera-video"
                    }
            ),
        type =
            when (type) {
                CameraCaptureType.PHOTO -> MessageMediaType.IMAGE
                CameraCaptureType.VIDEO -> MessageMediaType.VIDEO
            },
        bytes = bytes,
        mimeType = mimeType,
        source = MediaSelectionSource.CAMERA,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
