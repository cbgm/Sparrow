package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveManualIdentityUseCase
import kotlin.reflect.KClass

/** ReceiveIdentity -> [CompleteIdentity]: a manually shared identity arrived. */
class ReceiveManualIdentityWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val receiveManualIdentity: ReceiveManualIdentityUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveManualIdentity,
        ReceiveManualIdentityWorkflow.Step,
        ReceiveManualIdentityWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveManualIdentity> =
        IncomingCommand.ReceiveManualIdentity::class

    override fun firstStep(command: IncomingCommand.ReceiveManualIdentity): Step = Step.Receive(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.Receive -> receive(step.command)
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

    private suspend fun receive(command: IncomingCommand.ReceiveManualIdentity): Outcome =
        if (receiveManualIdentity(command.context, command.packet).getOrThrow()) {
            Outcome.IdentityMutual
        } else {
            Outcome.IdentityOneWay
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveManualIdentity

        data class Receive(
            override val command: IncomingCommand.ReceiveManualIdentity
        ) : Step

        data class CompleteIdentity(
            override val command: IncomingCommand.ReceiveManualIdentity
        ) : Step
    }

    sealed interface Outcome {
        data object IdentityMutual : Outcome

        data object IdentityOneWay : Outcome

        data object IdentityCompleted : Outcome
    }
}
