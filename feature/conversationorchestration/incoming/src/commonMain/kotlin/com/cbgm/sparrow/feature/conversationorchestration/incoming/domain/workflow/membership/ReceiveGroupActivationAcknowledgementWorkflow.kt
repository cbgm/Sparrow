package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupActivationAcknowledgementUseCase
import kotlin.reflect.KClass

/** ReceiveAcknowledgement: Membership advances the member's activation rounds. */
class ReceiveGroupActivationAcknowledgementWorkflow internal constructor(
    private val receiveGroupActivationAcknowledgement: ReceiveGroupActivationAcknowledgementUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupActivationAcknowledgement,
        ReceiveGroupActivationAcknowledgementWorkflow.Step,
        ReceiveGroupActivationAcknowledgementWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupActivationAcknowledgement> =
        IncomingCommand.ReceiveGroupActivationAcknowledgement::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupActivationAcknowledgement): Step =
        Step.ReceiveAcknowledgement(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        receiveGroupActivationAcknowledgement(
            step.command.packet,
            context.contactId,
            context.transportMode
        ).getOrThrow()
        return Outcome.AcknowledgementReceived
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupActivationAcknowledgement

        data class ReceiveAcknowledgement(
            override val command: IncomingCommand.ReceiveGroupActivationAcknowledgement
        ) : Step
    }

    sealed interface Outcome {
        data object AcknowledgementReceived : Outcome
    }
}
