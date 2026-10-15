package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine

/** A failed packet was handed to the outbox again; its message leaves the failed state. */
class RecordRetryRequestedUseCase(
    private val engine: WorkflowEngine
) {
    suspend fun forDirectMessage(messageId: String): Result<Unit> =
        apply(DeliveryEventTarget.DirectMessage(messageId))

    suspend fun forGroupRecipient(messageId: String, contactId: String): Result<Unit> =
        apply(DeliveryEventTarget.Recipient(messageId, contactId))

    private suspend fun apply(target: DeliveryEventTarget): Result<Unit> =
        engine.run(
            DeliveryCommand.ApplyDeliveryEvent(target = target, event = MessageDeliveryEvent.RETRY_REQUESTED)
        ).map { }
}
