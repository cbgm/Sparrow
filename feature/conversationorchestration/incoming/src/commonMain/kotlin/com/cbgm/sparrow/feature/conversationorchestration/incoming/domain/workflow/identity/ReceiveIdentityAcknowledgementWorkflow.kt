package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityAcknowledgementUseCase
import kotlin.reflect.KClass

/** ReceiveAcknowledgement -> [CompleteIdentity]: the contact confirmed our manually shared identity. */
class ReceiveIdentityAcknowledgementWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val receiveIdentityAcknowledgement: ReceiveIdentityAcknowledgementUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveIdentityAcknowledgement,
        ReceiveIdentityAcknowledgementWorkflow.Step,
        ReceiveIdentityAcknowledgementWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveIdentityAcknowledgement> =
        IncomingCommand.ReceiveIdentityAcknowledgement::class

    override fun firstStep(command: IncomingCommand.ReceiveIdentityAcknowledgement): Step = Step.Receive(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.Receive ->
                if (receiveIdentityAcknowledgement(step.command.context, step.command.packet).getOrThrow()) {
                    Outcome.IdentityMutual
                } else {
                    Outcome.IdentityOneWay
                }
            is Step.CompleteIdentity -> {
                engine.run(IncomingCommand.CompleteManualIdentity(step.command.context.contactId)).getOrThrow()
                Outcome.IdentityCompleted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.IdentityMutual -> Transition.Next(Step.CompleteIdentity(step.command))
            Outcome.IdentityOneWay,
            Outcome.IdentityCompleted -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveIdentityAcknowledgement

        data class Receive(
            override val command: IncomingCommand.ReceiveIdentityAcknowledgement
        ) : Step

        data class CompleteIdentity(
            override val command: IncomingCommand.ReceiveIdentityAcknowledgement
        ) : Step
    }

    sealed interface Outcome {
        data object IdentityMutual : Outcome

        data object IdentityOneWay : Outcome

        data object IdentityCompleted : Outcome
    }
}
