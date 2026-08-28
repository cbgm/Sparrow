package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity

data class DirectMessage(
    val id: String,
    val contactId: String,
    val isMine: Boolean,
    val timestamp: Long,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val parts: List<MessagePart> = emptyList()
)
