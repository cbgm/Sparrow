package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.CheckIncomingInvitationAllowedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RecordIncomingDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclineIdentityExchangeUseCase
import kotlin.reflect.KClass

/** CheckAllowed -> RecordInvitation | CheckAllowed -> [DeclineExchange]. */
class RecordIncomingDirectInvitationWorkflow internal constructor(
    private val checkIncomingInvitationAllowed: CheckIncomingInvitationAllowedUseCase,
    private val recordIncomingDirectInvitation: RecordIncomingDirectInvitationUseCase,
    private val declineIdentityExchange: DeclineIdentityExchangeUseCase
) : WorkflowStateMachine<
        LifecycleCommand.RecordIncomingDirectInvitation,
        RecordIncomingDirectInvitationWorkflow.Step,
        RecordIncomingDirectInvitationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.RecordIncomingDirectInvitation> =
        LifecycleCommand.RecordIncomingDirectInvitation::class

    override fun firstStep(command: LifecycleCommand.RecordIncomingDirectInvitation): Step = Step.CheckAllowed(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckAllowed ->
                allowedChecked(
                    checkIncomingInvitationAllowed(
                        step.command.result.peerId,
                        step.command.result.wasKnownPeerAtReceive
                    ).getOrThrow()
                )
            is Step.RecordInvitation -> {
                recordIncomingDirectInvitation(step.command.result, step.command.peerDisplayName).getOrThrow()
                Outcome.InvitationRecorded
            }
            is Step.DeclineExchange -> {
                declineIdentityExchange(step.command.result.exchangeId).getOrThrow()
                Outcome.ExchangeDeclined
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Allowed -> Transition.Next(Step.RecordInvitation(step.command))
            Outcome.Rejected -> afterRejected(step.command)
            Outcome.InvitationRecorded -> Transition.Complete()
            Outcome.ExchangeDeclined -> Transition.Complete()
        }

    private fun allowedChecked(allowed: Boolean): Outcome = if (allowed) Outcome.Allowed else Outcome.Rejected

    private fun afterRejected(command: LifecycleCommand.RecordIncomingDirectInvitation): Transition<Step> =
        if (command.declineIfRejected) Transition.Next(Step.DeclineExchange(command)) else Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.RecordIncomingDirectInvitation

        data class CheckAllowed(
            override val command: LifecycleCommand.RecordIncomingDirectInvitation
        ) : Step

        data class RecordInvitation(
            override val command: LifecycleCommand.RecordIncomingDirectInvitation
        ) : Step

        data class DeclineExchange(
            override val command: LifecycleCommand.RecordIncomingDirectInvitation
        ) : Step
    }

    sealed interface Outcome {
        data object Allowed : Outcome

        data object Rejected : Outcome

        data object InvitationRecorded : Outcome

        data object ExchangeDeclined : Outcome
    }
}
