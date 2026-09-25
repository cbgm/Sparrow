package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi

internal fun List<LocalAttachment>.toMessageAttachmentsUi(): List<MessageAttachmentUi> =
    mapNotNull { attachment ->
        when (attachment.type) {
            MessageAttachmentType.IMAGE,
            MessageAttachmentType.VIDEO ->
                MessageAttachmentUi.ImageVideoAttachmentUi(
                    id = attachment.id,
                    type = attachment.type,
                    mimeType = attachment.mimeType,
                    byteSize = attachment.byteSize,
                    fileName = attachment.fileName,
                    width = attachment.width,
                    height = attachment.height,
                    durationMilliseconds = attachment.durationMilliseconds
                )

            MessageAttachmentType.FILE ->
                MessageAttachmentUi.FileAttachmentUi(
                    id = attachment.id,
                    mimeType = attachment.mimeType,
                    byteSize = attachment.byteSize,
                    fileName = attachment.fileName ?: attachment.id
                )

            MessageAttachmentType.LOCATION,
            MessageAttachmentType.CONTACT,
            MessageAttachmentType.VOICE -> null
        }
    }
