package com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper

import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState

internal fun MessageRecipientStateEntity.toGroupRecipientState(): GroupRecipientState =
    GroupRecipientState(
        messageId = messageId,
        contactId = contactId,
        packetId = packetId,
        deliveryStatus = deliveryStatus.toMessageDeliveryStatus(),
        lastError = lastError,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

internal fun GroupRecipientState.toMessageRecipientStateEntity(): MessageRecipientStateEntity =
    MessageRecipientStateEntity(
        messageId = messageId,
        contactId = contactId,
        packetId = packetId,
        deliveryStatus = deliveryStatus.name,
        lastError = lastError,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
