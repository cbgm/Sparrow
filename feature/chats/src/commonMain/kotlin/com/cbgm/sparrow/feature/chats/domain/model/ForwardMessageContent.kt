package com.cbgm.sparrow.feature.chats.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart

data class ForwardMessageContent(
    val text: String,
    val parts: List<MessagePart>
)
