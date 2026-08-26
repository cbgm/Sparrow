package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelection
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelectionType

fun AttachmentSelection.toOutgoingMessageAttachment(): OutgoingMessageAttachment =
    OutgoingMessageAttachment(
        id = id,
        type = when (type) {
            AttachmentSelectionType.IMAGE -> MessageAttachmentType.IMAGE
            AttachmentSelectionType.VIDEO -> MessageAttachmentType.VIDEO
            AttachmentSelectionType.FILE -> MessageAttachmentType.FILE
        },
        bytes = bytes,
        mimeType = mimeType,
        fileName = if (type == AttachmentSelectionType.FILE) requireNotNull(fileName) else null,
        width = if (type == AttachmentSelectionType.FILE) null else width,
        height = if (type == AttachmentSelectionType.FILE) null else height,
        durationMilliseconds = if (type == AttachmentSelectionType.FILE) null else durationMilliseconds
    )
