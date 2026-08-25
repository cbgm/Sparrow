package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment

data class PreparedMessageAttachment(
    val attachment: MessageAttachment,
    val deleteCapability: String,
    val localFileName: String
)
