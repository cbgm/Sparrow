package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.MessageFileAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageFileAttachmentUi
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

fun MessageFileAttachment.toUi(bytes: ByteArray? = null): MessageFileAttachmentUi =
    MessageFileAttachmentUi(
        id = id,
        fileName = fileName,
        mimeType = mimeType,
        sizeText = byteSize.toReadableByteSize(),
        localFilePath = localFilePath,
        bytes = bytes
    )
