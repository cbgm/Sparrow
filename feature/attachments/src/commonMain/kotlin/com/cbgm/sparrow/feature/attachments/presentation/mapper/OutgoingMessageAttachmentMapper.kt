package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.media.presentation.model.FileSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

fun MediaSelection.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = id,
        type = when (type) {
            MediaType.IMAGE -> MessageAttachmentType.IMAGE
            MediaType.VIDEO -> MessageAttachmentType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun FileSelection.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = id,
        type = MessageAttachmentType.FILE,
        bytes = bytes,
        mimeType = mimeType,
        fileName = fileName
    )
