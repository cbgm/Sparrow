package com.cbgm.sparrow.feature.chats.data.attachment

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment

internal data class PreparedMessageAttachment(
    val attachment: MessageAttachment,
    val deleteCapability: String,
    val localFileName: String
)
