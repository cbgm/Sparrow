package com.cbgm.sparrow.feature.chats.data.direct.datasource

import com.cbgm.sparrow.data.database.dao.MessageDeliveryStatusDao

/** Owns Chats delivery-state persistence; the transition policy stays in the coordinator. */
class DirectDeliveryDataSource(
    private val deliveryDao: MessageDeliveryStatusDao
) {
    suspend fun findByPacketId(packetId: String): String? =
        deliveryDao.findOutgoingDeliveryStatusByPacketId(packetId)

    suspend fun findByMessageAndContact(messageId: String, contactId: String): String? =
        deliveryDao.findOutgoingDeliveryStatus(messageId, contactId)

    suspend fun findByMessageId(messageId: String): String? =
        deliveryDao.findOutgoingDeliveryStatusByMessageId(messageId)

    suspend fun updatePreparedTransport(packetId: String, payload: String, mode: String) {
        deliveryDao.updatePreparedTransport(packetId, payload, mode)
    }

    suspend fun updateByPacketId(packetId: String, status: String) {
        deliveryDao.updateDeliveryStatus(packetId, status)
    }

    suspend fun updateByMessageId(messageId: String, status: String) {
        deliveryDao.updateDeliveryStatusByMessageId(messageId, status)
    }
}
