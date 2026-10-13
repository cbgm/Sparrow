package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.HandleDirectInvitationResultWorkflow
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
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
            is Step.CheckExchangeStillOpen -> {
                if (
                    result.direction != InvitationDirection.INCOMING || result.response != InvitationResponse.ACCEPTED
                ) {
                    return Outcome.NothingToRecover
                }
                val exchange = getIdentityExchangeClosure(result.peerId).getOrThrow()
                val open =
                    exchange != null &&
                        exchange.exchangeId == result.invitationId &&
                        exchange.phase != IdentityExchangeClosurePhase.TERMINAL &&
                        exchange.phase != IdentityExchangeClosurePhase.ALREADY_CLOSED
                if (open) Outcome.ExchangeStillOpen else Outcome.NothingToRecover
            }
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
