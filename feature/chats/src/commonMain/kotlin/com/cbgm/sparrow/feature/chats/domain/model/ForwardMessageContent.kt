package com.cbgm.sparrow.feature.chats.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart

data class ForwardMessageContent(
    val parts: List<MessagePart>
)
