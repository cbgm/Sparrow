package com.cbgm.sparrow.feature.conversationorchestration.delivery.data.mapper

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupRecipientDelivery

internal fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this }
        ?: MessageDeliveryStatus.NOT_APPLICABLE

internal fun MessageRecipientStateEntity.toGroupRecipientDelivery(): GroupRecipientDelivery =
    GroupRecipientDelivery(
        messageId = messageId,
        contactId = contactId,
        status = deliveryStatus.toMessageDeliveryStatus()
    )
