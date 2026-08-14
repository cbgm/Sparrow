package com.cbgm.sparrow.feature.chats.data.group.delivery

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupMessageDeliveryCoordinator(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) {
    private val mutex = Mutex()

    suspend fun handlesPacket(packetId: String): Boolean =
        messageRecipientStateDao.findByPacketId(packetId) != null

    suspend fun handlesReceipt(
        messageId: String,
        contactId: String
    ): Boolean =
        messageRecipientStateDao.findByMessageId(messageId).any { it.contactId == contactId }

    suspend fun applyPacketEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        mutex.withLock {
            val state = messageRecipientStateDao.findByPacketId(packetId) ?: return@withLock
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
            val state = messageRecipientStateDao.findByMessageId(messageId).firstOrNull { it.contactId == contactId } ?: return@withLock
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
            val state = messageRecipientStateDao.findByMessageId(messageId).firstOrNull { it.contactId == contactId } ?: return@withLock
            updateRecipientState(state, MessageDeliveryEvent.RETRY_REQUESTED)
            updateAggregatedStatus(messageId)
        }
    }

    suspend fun expireUnconfirmedRecipients(
        groupId: String,
        timeoutMilliseconds: Long = DELIVERY_TIMEOUT_MILLISECONDS
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(timeoutMilliseconds > 0L) { "Delivery timeout must be positive" }
        val cutoff = SystemClock.nowEpochMilliseconds() - timeoutMilliseconds
        mutex.withLock {
            messageRecipientStateDao
                .findByConversationAndDeliveryStatusBefore(groupId, MessageDeliveryStatus.SENT.name, cutoff)
                .forEach { state ->
                    updateRecipientState(state, MessageDeliveryEvent.DELIVERY_TIMED_OUT, "Recipient did not confirm delivery")
                    updateAggregatedStatus(state.messageId)
                }
        }
    }

    private suspend fun updateRecipientState(
        state: MessageRecipientStateEntity,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        val current = state.deliveryStatus.toGroupDeliveryStatus()
        val next = GroupMessageDeliveryStateMachine.transition(current, event)
        if (next == current) return
        messageRecipientStateDao.updateDeliveryStatus(
            messageId = state.messageId,
            contactId = state.contactId,
            deliveryStatus = next.name,
            lastError = if (next == MessageDeliveryStatus.FAILED) errorMessage else null,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
    }

    private suspend fun updateAggregatedStatus(messageId: String) {
        val statuses = messageRecipientStateDao.findByMessageId(messageId).map { it.deliveryStatus.toGroupDeliveryStatus() }
        val aggregated = GroupMessageDeliveryStateMachine.aggregate(statuses)
        messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, aggregated.name)
    }

    private companion object {
        const val DELIVERY_TIMEOUT_MILLISECONDS = 60_000L
    }
}
