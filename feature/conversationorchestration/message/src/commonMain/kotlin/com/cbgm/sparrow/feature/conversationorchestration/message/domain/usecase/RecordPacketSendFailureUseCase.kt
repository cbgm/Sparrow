package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine

/** A packet could not be written to the outbox; its message shows the failure. */
class RecordPacketSendFailureUseCase(
    private val engine: WorkflowEngine
) {
    suspend operator fun invoke(packetId: String, errorMessage: String?): Result<Unit> =
        engine.run(
            DeliveryCommand.ApplyDeliveryEvent(
                target = DeliveryEventTarget.Packet(packetId),
                event = MessageDeliveryEvent.SEND_FAILED,
                errorMessage = errorMessage
            )
        ).map { }
}
