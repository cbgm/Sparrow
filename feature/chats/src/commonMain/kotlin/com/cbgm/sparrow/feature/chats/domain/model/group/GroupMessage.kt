package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.attachments.domain.model.MessageFileAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaAttachment
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity

data class GroupMessage(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val type: ChatMessageType = ChatMessageType.USER,
    val senderContactId: String? = null,
    val deliveryProgress: MessageDeliveryProgress = MessageDeliveryProgress(),
    val attachments: List<MessageMediaAttachment> = emptyList(),
    val fileAttachments: List<MessageFileAttachment> = emptyList()
)
