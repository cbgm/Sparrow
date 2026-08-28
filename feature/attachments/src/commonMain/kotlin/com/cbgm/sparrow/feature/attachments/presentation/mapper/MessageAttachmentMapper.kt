package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi

fun MessageAttachment.toUi(bytes: ByteArray? = null): MessageAttachmentUi =
    MessageAttachmentUi(
        id = id,
        type = type,
        mimeType = mimeType,
        byteSize = byteSize,
        fileName = fileName,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds,
        localFilePath = localFilePath,
        bytes = bytes
    )
