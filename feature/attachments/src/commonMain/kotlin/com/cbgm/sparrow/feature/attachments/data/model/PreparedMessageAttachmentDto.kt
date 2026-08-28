package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment

data class PreparedMessageAttachmentDto(
    val attachment: MessageAttachment,
    val deleteCapability: String,
    val localFileName: String
)
