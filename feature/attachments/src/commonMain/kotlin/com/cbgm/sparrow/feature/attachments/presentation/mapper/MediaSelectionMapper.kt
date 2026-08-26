package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelection

fun MediaSelection.toOutgoingMediaAttachment(): OutgoingMediaAttachment =
    OutgoingMediaAttachment(
        id = id,
        type = type,
        bytes = bytes,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
