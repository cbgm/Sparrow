package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus

/** Delivery state of one group message for one recipient. */
data class GroupRecipientDelivery(
    val messageId: String,
    val contactId: String,
    val status: MessageDeliveryStatus
)
