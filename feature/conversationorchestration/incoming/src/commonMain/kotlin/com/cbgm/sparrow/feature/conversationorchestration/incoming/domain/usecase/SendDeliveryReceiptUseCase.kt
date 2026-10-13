package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.DeliveryReceiptPacket

/** Confirms to the sender that a message arrived. Group receipts carry the recipient in their packet ID. */
class SendDeliveryReceiptUseCase(
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun toContact(contactId: String, messageId: String): Result<Unit> =
        enqueue(contactId, messageId, packetId = "delivery-receipt-$messageId")

    suspend fun toGroupMember(contactId: String, messageId: String): Result<Unit> =
        enqueue(contactId, messageId, packetId = "delivery-receipt-$messageId-$contactId")

    private suspend fun enqueue(contactId: String, messageId: String, packetId: String): Result<Unit> =
        protocolOutbox.enqueue(
            contactId = contactId,
            packet =
                DeliveryReceiptPacket(
                    packetId = packetId,
                    messageId = messageId,
                    deliveredAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
        ).map { }
}
