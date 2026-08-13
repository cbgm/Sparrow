package com.cbgm.securechat.feature.chats.domain.model.direct

import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity

data class DirectMessage(
    val id: String,
    val contactId: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus
)
