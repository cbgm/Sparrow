package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupRecipientDelivery

interface GroupRecipientDeliveryRepository {
    suspend fun findByPacketId(packetId: String): Result<GroupRecipientDelivery?>

    suspend fun findByRecipient(messageId: String, contactId: String): Result<GroupRecipientDelivery?>

    suspend fun findStatuses(messageId: String): Result<List<MessageDeliveryStatus>>

    suspend fun updateRecipientStatus(
        recipient: GroupRecipientDelivery,
        status: MessageDeliveryStatus,
        lastError: String?
    ): Result<Unit>

    suspend fun updateMessageStatus(messageId: String, status: MessageDeliveryStatus): Result<Unit>
}
