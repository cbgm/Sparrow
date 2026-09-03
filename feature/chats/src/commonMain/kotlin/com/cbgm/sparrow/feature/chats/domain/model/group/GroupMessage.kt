package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity

data class GroupMessage(
    val id: String,
    val isMine: Boolean,
    val timestamp: Long,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val replyToMessageId: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val type: ChatMessageType = ChatMessageType.USER,
    val senderContactId: String? = null,
    val deliveryProgress: MessageDeliveryProgress = MessageDeliveryProgress(),
    val parts: List<MessagePart> = emptyList()
)
