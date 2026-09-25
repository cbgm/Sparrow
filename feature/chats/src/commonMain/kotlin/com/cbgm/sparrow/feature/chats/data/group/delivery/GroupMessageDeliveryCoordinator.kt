package com.cbgm.sparrow.feature.chats.data.group.delivery

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupRecipientDeliveryDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupMessageDeliveryCoordinator(
    private val dataSource: GroupRecipientDeliveryDataSource
) {
    private val mutex = Mutex()

    suspend fun handlesPacket(packetId: String): Boolean =
        dataSource.findByPacketId(packetId) != null

    suspend fun handlesReceipt(
        messageId: String,
        contactId: String
    ): Boolean =
        dataSource.findByMessageId(messageId).any { it.contactId == contactId }

    suspend fun applyPacketEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        mutex.withLock {
            val state = dataSource.findByPacketId(packetId) ?: return@withLock
            updateRecipientState(state, event, errorMessage)
            updateAggregatedStatus(state.messageId)
        }
    }

    suspend fun applyReceiptEvent(
        messageId: String,
        contactId: String,
        event: MessageDeliveryEvent
    ) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(event == MessageDeliveryEvent.DELIVERY_CONFIRMED || event == MessageDeliveryEvent.READ_CONFIRMED) {
            "Only receipt events can be applied by message ID"
        }
        mutex.withLock {
            val state = dataSource.findByMessageId(messageId).firstOrNull { it.contactId == contactId } ?: return@withLock
            updateRecipientState(state, event)
            updateAggregatedStatus(messageId)
        }
    }

    suspend fun applyRetryEvent(
        messageId: String,
        contactId: String
    ) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        mutex.withLock {
            val state = dataSource.findByMessageId(messageId).firstOrNull { it.contactId == contactId } ?: return@withLock
            updateRecipientState(state, MessageDeliveryEvent.RETRY_REQUESTED)
            updateAggregatedStatus(messageId)
        }
    }

    private suspend fun updateRecipientState(
        state: MessageRecipientStateEntity,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        val current = state.deliveryStatus.toMessageDeliveryStatus()
        val next = GroupMessageDeliveryStateMachine.transition(current, event)
        if (next == current) return
        dataSource.updateDeliveryStatus(
            messageId = state.messageId,
            contactId = state.contactId,
            deliveryStatus = next.name,
            lastError = if (next == MessageDeliveryStatus.FAILED) errorMessage else null,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
    }

    private suspend fun updateAggregatedStatus(messageId: String) {
        val statuses = dataSource.findByMessageId(messageId).map { it.deliveryStatus.toMessageDeliveryStatus() }
        val aggregated = GroupMessageDeliveryStateMachine.aggregate(statuses)
        dataSource.updateAggregatedDeliveryStatus(messageId, aggregated.name)
    }
}
