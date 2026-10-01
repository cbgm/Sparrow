package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.mapper.toUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi

internal fun List<LocalAttachment>.toMessageAttachmentsUi(): List<MessageAttachmentUi> =
    mapNotNull { attachment ->
        when (attachment) {
            is LocalAttachment.Media -> attachment.toMessageAttachmentUi()
            is LocalAttachment.File ->
                MessageAttachmentUi.FileAttachmentUi(
                    id = attachment.id,
                    mimeType = attachment.mimeType,
                    byteSize = attachment.byteSize,
                    fileName = attachment.fileName ?: attachment.id
                )

            is LocalAttachment.Voice -> null
        }
    }

private fun LocalAttachment.Media.toMessageAttachmentUi(): MessageAttachmentUi.ImageVideoAttachmentUi =
    MessageAttachmentUi.ImageVideoAttachmentUi(
        id = id,
        media =
            MediaItemUi(
                id = id,
                type = mediaType.toUi(),
                mimeType = mimeType,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            ),
        byteSize = byteSize,
        fileName = fileName
    )
