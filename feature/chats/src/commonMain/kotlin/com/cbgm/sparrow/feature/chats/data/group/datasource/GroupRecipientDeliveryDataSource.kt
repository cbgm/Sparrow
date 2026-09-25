package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity

/** Chats-owned group recipient status persistence; no repository or cross-module operations. */
class GroupRecipientDeliveryDataSource(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) {
    suspend fun findByPacketId(packetId: String): MessageRecipientStateEntity? =
        messageRecipientStateDao.findByPacketId(packetId)

    suspend fun findByMessageId(messageId: String): List<MessageRecipientStateEntity> =
        messageRecipientStateDao.findByMessageId(messageId)

    suspend fun updateDeliveryStatus(
        messageId: String,
        contactId: String,
        deliveryStatus: String,
        lastError: String?,
        updatedAtEpochMilliseconds: Long
    ) {
        messageRecipientStateDao.updateDeliveryStatus(
            messageId = messageId,
            contactId = contactId,
            deliveryStatus = deliveryStatus,
            lastError = lastError,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )
    }

    suspend fun updateAggregatedDeliveryStatus(messageId: String, deliveryStatus: String) {
        messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, deliveryStatus)
    }
}
