package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.workflow.transport

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.DirectMessageDeliveryRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlin.reflect.KClass

/** FindDirectMessage -> StorePreparedTransport. Group packets keep no prepared payload. */
class RecordPreparedTransportWorkflow internal constructor(
    private val directDeliveryRepository: DirectMessageDeliveryRepository
) : WorkflowStateMachine<
        DeliveryCommand.RecordPreparedTransport,
        RecordPreparedTransportWorkflow.Step,
        RecordPreparedTransportWorkflow.Outcome
    > {
    override val commandType: KClass<DeliveryCommand.RecordPreparedTransport> =
        DeliveryCommand.RecordPreparedTransport::class

    override fun firstStep(command: DeliveryCommand.RecordPreparedTransport): Step = Step.FindDirectMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindDirectMessage ->
                directMessageFound(directDeliveryRepository.findStatusByPacketId(step.command.packetId).getOrThrow())
            is Step.StorePreparedTransport -> {
                directDeliveryRepository.storePreparedTransport(
                    packetId = step.command.packetId,
                    encodedTransportPayload = step.command.encodedTransportPayload,
                    transportMode = step.command.transportMode
                ).getOrThrow()
                Outcome.Stored
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.DirectMessageFound -> Transition.Next(Step.StorePreparedTransport(step.command))
            Outcome.NotDirectMessage,
            Outcome.Stored -> Transition.Complete()
        }

    private fun directMessageFound(status: MessageDeliveryStatus?): Outcome =
        if (status != null) Outcome.DirectMessageFound else Outcome.NotDirectMessage

    sealed interface Step : WorkflowStep {
        val command: DeliveryCommand.RecordPreparedTransport

        data class FindDirectMessage(
            override val command: DeliveryCommand.RecordPreparedTransport
        ) : Step

        data class StorePreparedTransport(
            override val command: DeliveryCommand.RecordPreparedTransport
        ) : Step
    }

    sealed interface Outcome {
        data object DirectMessageFound : Outcome

        data object NotDirectMessage : Outcome

        data object Stored : Outcome
    }
}
