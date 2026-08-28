package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionType

fun MediaSelection.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = id,
        type = when (type) {
            MediaSelectionType.IMAGE -> MessageAttachmentType.IMAGE
            MediaSelectionType.VIDEO -> MessageAttachmentType.VIDEO
            MediaSelectionType.FILE -> MessageAttachmentType.FILE
        },
        bytes = bytes,
        mimeType = mimeType,
        fileName = if (type == MediaSelectionType.FILE) requireNotNull(fileName) else null,
        width = if (type == MediaSelectionType.FILE) null else width,
        height = if (type == MediaSelectionType.FILE) null else height,
        durationMilliseconds = if (type == MediaSelectionType.FILE) null else durationMilliseconds
    )
