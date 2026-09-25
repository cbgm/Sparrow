package com.cbgm.sparrow.feature.chats.domain.model

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment

data class ForwardMessageContent(
    val text: String,
    val attachments: List<OutgoingMessageAttachment>
)
