package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

data class MessageBubbleUi(
    val id: String,
    val isMine: Boolean,
    val security: MessageSecurity,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val senderName: String? = null,
    val senderIsInContacts: Boolean = true,
    val deliveryProgress: DeliveryProgressUi = DeliveryProgressUi(),
    val safetyWarning: MessageSafetyWarningUi? = null,
    val fileParts: List<MessagePartUi.File> = emptyList(),
    val imageVideoParts: List<MessagePartUi.ImageVideo> = emptyList(),
    val locationPart: MessagePartUi.Location? = null,
    val contactPart: MessagePartUi.Contact? = null,
    val textPart: MessagePartUi.Text? = null
)

data class DeliveryProgressUi(
    val recipientCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0
)
