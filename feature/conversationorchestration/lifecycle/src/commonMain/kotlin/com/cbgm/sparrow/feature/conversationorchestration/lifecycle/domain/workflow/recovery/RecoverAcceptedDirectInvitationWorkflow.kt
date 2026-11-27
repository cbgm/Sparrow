package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import kotlin.reflect.KClass

/** CheckExchangeStillOpen -> HandleResult. A closed or revoked exchange is never replayed. */
class RecoverAcceptedDirectInvitationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getIdentityExchangeClosure: GetIdentityExchangeClosureUseCase
) : WorkflowStateMachine<
        LifecycleCommand.RecoverAcceptedDirectInvitation,
        RecoverAcceptedDirectInvitationWorkflow.Step,
        RecoverAcceptedDirectInvitationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.RecoverAcceptedDirectInvitation> =
        LifecycleCommand.RecoverAcceptedDirectInvitation::class

    override fun firstStep(command: LifecycleCommand.RecoverAcceptedDirectInvitation): Step =
        Step.CheckExchangeStillOpen(command)

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.CheckExchangeStillOpen -> checkExchangeStillOpen(step.command)
            is Step.HandleResult -> {
                engine.run(LifecycleCommand.HandleDirectInvitationResult(result)).getOrThrow()
                Outcome.ResultHandled
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.ExchangeStillOpen -> Transition.Next(Step.HandleResult(step.command))
            Outcome.NothingToRecover -> Transition.Complete()
            Outcome.ResultHandled -> Transition.Complete()
        }

    /** Only an incoming acceptance of the peer's still open exchange is replayed. */
    private suspend fun checkExchangeStillOpen(command: LifecycleCommand.RecoverAcceptedDirectInvitation): Outcome {
        val result = command.result
        if (result.direction != InvitationDirection.INCOMING || result.response != InvitationResponse.ACCEPTED) {
            return Outcome.NothingToRecover
        }
        val exchange = getIdentityExchangeClosure(result.peerId).getOrThrow()
        return if (exchange?.isOpenFor(result.invitationId) == true) Outcome.ExchangeStillOpen else Outcome.NothingToRecover
    }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.RecoverAcceptedDirectInvitation

        data class CheckExchangeStillOpen(
            override val command: LifecycleCommand.RecoverAcceptedDirectInvitation
        ) : Step

        data class HandleResult(
            override val command: LifecycleCommand.RecoverAcceptedDirectInvitation
        ) : Step
    }

    sealed interface Outcome {
        data object ExchangeStillOpen : Outcome

        data object NothingToRecover : Outcome

        data object ResultHandled : Outcome
    }
}
