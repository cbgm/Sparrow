package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand

/**
 * Every delivery job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface DeliveryCommand : WorkflowCommand {
    /** Applies one delivery event to the message or recipient it belongs to. */
    data class ApplyDeliveryEvent(
        val target: DeliveryEventTarget,
        val event: MessageDeliveryEvent,
        val errorMessage: String? = null
    ) : DeliveryCommand

    /** The outbox encrypted [packetId]; a direct message keeps the prepared payload for display and retry. */
    data class RecordPreparedTransport(
        val packetId: String,
        val encodedTransportPayload: String,
        val transportMode: String
    ) : DeliveryCommand
}
