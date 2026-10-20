package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.OperationMessagePacket

/** Queues an edit, delete or reaction for the contact of a direct conversation. */
class SendDirectOperationUseCase(
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(contactId: String, operation: MessageOperation): Result<Unit> =
        protocolOutbox.enqueue(
            contactId,
            OperationMessagePacket(
                packetId = IdGenerator.generate(prefix = "operation-packet"),
                message = OperationMessage(operation)
            )
        ).map { }
}
