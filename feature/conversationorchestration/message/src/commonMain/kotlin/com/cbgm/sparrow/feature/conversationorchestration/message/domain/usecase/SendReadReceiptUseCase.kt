package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.ReadReceiptPacket

class SendReadReceiptUseCase(
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(messageId: String, contactId: String): Result<Unit> =
        protocolOutbox.enqueue(
            contactId = contactId,
            packet =
                ReadReceiptPacket(
                    packetId = "read-receipt-$messageId",
                    messageId = messageId,
                    readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
        ).map { }
}
