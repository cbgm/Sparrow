package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus

/** The packet one group recipient receives for one message, and its delivery state. */
data class GroupRecipientState(
    val messageId: String,
    val contactId: String,
    val packetId: String?,
    val deliveryStatus: MessageDeliveryStatus,
    val lastError: String?,
    val updatedAtEpochMilliseconds: Long
)
