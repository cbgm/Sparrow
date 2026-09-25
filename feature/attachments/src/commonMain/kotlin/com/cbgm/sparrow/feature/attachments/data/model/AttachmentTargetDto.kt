package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

/** A null groupId denotes a regular message attachment. */
internal data class AttachmentTargetDto(
    val id: String,
    val type: MessageAttachmentType,
    val groupId: String? = null
)
