package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi

internal fun List<LocalAttachment>.toAttachmentManagementUiModels(
    loadedBytes: Map<String, ByteArray>
): List<MessageAttachmentUi> =
    map { attachment ->
        MessageAttachmentUi(
            id = attachment.id,
            type = attachment.type,
            mimeType = attachment.mimeType,
            byteSize = attachment.byteSize,
            fileName = attachment.fileName,
            width = attachment.width,
            height = attachment.height,
            durationMilliseconds = attachment.durationMilliseconds,
            bytes = loadedBytes[attachment.id]
        )
    }
