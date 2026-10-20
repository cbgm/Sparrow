package com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.DirectDeliveryDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.DirectMessageDeliveryRepository

internal class DirectMessageDeliveryRepositoryImpl(
    private val dataSource: DirectDeliveryDataSource
) : DirectMessageDeliveryRepository {
    override suspend fun findStatusByPacketId(packetId: String): Result<MessageDeliveryStatus?> =
        safeSuspendCall { dataSource.findByPacketId(packetId)?.toMessageDeliveryStatus() }

    override suspend fun findStatusByRecipient(messageId: String, contactId: String): Result<MessageDeliveryStatus?> =
        safeSuspendCall { dataSource.findByMessageAndContact(messageId, contactId)?.toMessageDeliveryStatus() }

    override suspend fun findStatusByMessageId(messageId: String): Result<MessageDeliveryStatus?> =
        safeSuspendCall { dataSource.findByMessageId(messageId)?.toMessageDeliveryStatus() }

    override suspend fun updateStatusByPacketId(packetId: String, status: MessageDeliveryStatus): Result<Unit> =
        safeSuspendCall { dataSource.updateByPacketId(packetId, status.name) }

    override suspend fun updateStatusByMessageId(messageId: String, status: MessageDeliveryStatus): Result<Unit> =
        safeSuspendCall { dataSource.updateByMessageId(messageId, status.name) }

    override suspend fun storePreparedTransport(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ): Result<Unit> =
        safeSuspendCall {
            require(packetId.isNotBlank()) { "Packet ID must not be blank" }
            require(encodedTransportPayload.isNotBlank()) { "Transport payload must not be blank" }
            require(transportMode.isNotBlank()) { "Transport mode must not be blank" }
            dataSource.updatePreparedTransport(packetId, encodedTransportPayload, transportMode)
        }
}
