package com.cbgm.sparrow.feature.chats.data.direct.delivery

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent

class DirectOutboxDeliveryHandler(
    private val deliveryCoordinator: DirectMessageDeliveryCoordinator
) {
    suspend fun canHandle(packetId: String): Boolean =
        deliveryCoordinator.handlesPacket(packetId)

    suspend fun onPrepared(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ) {
        deliveryCoordinator.storePreparedTransport(
            packetId = packetId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode
        )
    }

    suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        deliveryCoordinator.applyPacketEvent(packetId, event, errorMessage)
    }
}
