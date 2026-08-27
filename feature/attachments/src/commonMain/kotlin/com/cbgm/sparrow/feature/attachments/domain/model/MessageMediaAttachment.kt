package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class MessageMediaAttachment(
    val id: String,
    val type: MessageAttachmentType,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
)
