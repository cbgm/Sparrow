package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.dispatch

import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper.toControlCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ReceivedPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.HandleIdentityVerificationReceiptUseCase
import com.cbgm.sparrow.protocol.handler.ProtocolPacketHandler
import com.cbgm.sparrow.protocol.packet.ContactVerificationReceiptPacket

/**
 * Starts the workflow for an identity or invitation packet. Packets of other features (for example mailbox
 * routes) go to the protocol handlers they registered.
 */
class ControlPacketDispatcher internal constructor(
    private val engine: WorkflowEngine,
    private val handleIdentityVerificationReceipt: HandleIdentityVerificationReceiptUseCase,
    private val otherFeaturePacketHandler: ProtocolPacketHandler
) {
    suspend fun dispatch(received: ReceivedPacket): Result<Unit> {
        val packet = received.packet
        if (packet is ContactVerificationReceiptPacket) {
            return handleIdentityVerificationReceipt(received.context, packet)
        }
        val command = received.toControlCommand() ?: return otherFeaturePacketHandler.handle(received.context, packet)
        return engine.run(command).map { }
    }
}
