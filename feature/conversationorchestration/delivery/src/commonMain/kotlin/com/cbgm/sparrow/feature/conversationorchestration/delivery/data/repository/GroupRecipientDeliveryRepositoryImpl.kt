package com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.GroupRecipientDeliveryDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.mapper.toGroupRecipientDelivery
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupRecipientDelivery
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.GroupRecipientDeliveryRepository

internal class GroupRecipientDeliveryRepositoryImpl(
    private val dataSource: GroupRecipientDeliveryDataSource
) : GroupRecipientDeliveryRepository {
    override suspend fun findByPacketId(packetId: String): Result<GroupRecipientDelivery?> =
        safeSuspendCall { dataSource.findByPacketId(packetId)?.toGroupRecipientDelivery() }

    override suspend fun findByRecipient(messageId: String, contactId: String): Result<GroupRecipientDelivery?> =
        safeSuspendCall {
            dataSource.findByMessageId(messageId)
                .firstOrNull { state -> state.contactId == contactId }
                ?.toGroupRecipientDelivery()
        }

    override suspend fun findStatuses(messageId: String): Result<List<MessageDeliveryStatus>> =
        safeSuspendCall {
            dataSource.findByMessageId(messageId).map { state -> state.deliveryStatus.toMessageDeliveryStatus() }
        }

    override suspend fun updateRecipientStatus(
        recipient: GroupRecipientDelivery,
        status: MessageDeliveryStatus,
        lastError: String?
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.updateDeliveryStatus(
                messageId = recipient.messageId,
                contactId = recipient.contactId,
                deliveryStatus = status.name,
                lastError = lastError,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

    override suspend fun updateMessageStatus(messageId: String, status: MessageDeliveryStatus): Result<Unit> =
        safeSuspendCall { dataSource.updateAggregatedDeliveryStatus(messageId, status.name) }
}
