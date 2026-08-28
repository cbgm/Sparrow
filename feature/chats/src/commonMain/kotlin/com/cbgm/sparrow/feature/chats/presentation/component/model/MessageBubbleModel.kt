package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUiModel

data class MessageBubbleModel(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val senderName: String? = null,
    val senderIsInContacts: Boolean = true,
    val deliveryProgress: DeliveryProgressModel = DeliveryProgressModel(),
    val safetyWarning: MessageSafetyWarningUiModel? = null,
    val attachments: List<MessageAttachmentUi> = emptyList()
)

data class DeliveryProgressModel(
    val recipientCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0
)
