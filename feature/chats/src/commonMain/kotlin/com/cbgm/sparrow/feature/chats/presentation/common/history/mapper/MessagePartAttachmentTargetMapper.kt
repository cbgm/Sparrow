package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessagePartUi

internal fun MessagePartUi.ImageVideo.toAttachmentTarget(): AttachmentTarget =
    AttachmentTarget(
        id = id,
        type =
            when (type) {
                ImageVideoTypeUi.IMAGE -> MessageAttachmentType.IMAGE
                ImageVideoTypeUi.VIDEO -> MessageAttachmentType.VIDEO
            },
        source = attachmentSource
    )

internal fun MessagePartUi.File.toAttachmentTarget(): AttachmentTarget =
    AttachmentTarget(
        id = id,
        type = MessageAttachmentType.FILE,
        source = attachmentSource
    )

internal fun MessagePartUi.Location.toAttachmentTarget(): AttachmentTarget =
    AttachmentTarget(
        id = id,
        type = MessageAttachmentType.LOCATION,
        source = attachmentSource
    )

internal fun MessagePartUi.Contact.toAttachmentTarget(): AttachmentTarget =
    AttachmentTarget(
        id = id,
        type = MessageAttachmentType.CONTACT,
        source = attachmentSource
    )
