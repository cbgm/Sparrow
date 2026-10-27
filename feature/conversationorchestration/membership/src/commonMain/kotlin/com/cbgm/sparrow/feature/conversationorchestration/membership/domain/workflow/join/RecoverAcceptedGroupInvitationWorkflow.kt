package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import kotlin.reflect.KClass

/** CheckHandshakeExists -> HandleResult. */
class RecoverAcceptedGroupInvitationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase
) : WorkflowStateMachine<
        MembershipCommand.RecoverAcceptedGroupInvitation,
        RecoverAcceptedGroupInvitationWorkflow.Step,
        RecoverAcceptedGroupInvitationWorkflow.Outcome
    > {
    override val commandType: KClass<MembershipCommand.RecoverAcceptedGroupInvitation> =
        MembershipCommand.RecoverAcceptedGroupInvitation::class

    override fun firstStep(command: MembershipCommand.RecoverAcceptedGroupInvitation): Step =
        Step.CheckHandshakeExists(command)

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.CheckHandshakeExists -> checkHandshakeExists(step.command)
            is Step.HandleResult -> {
                engine.run(MembershipCommand.HandleGroupInvitationResult(result)).getOrThrow()
                Outcome.ResultHandled
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.HandshakeExists -> Transition.Next(Step.HandleResult(step.command))
            Outcome.NothingToRecover -> Transition.Complete()
            Outcome.ResultHandled -> Transition.Complete()
        }

    /** Only an incoming acceptance whose handshake still exists can be replayed. */
    private suspend fun checkHandshakeExists(command: MembershipCommand.RecoverAcceptedGroupInvitation): Outcome {
        val result = command.result
        return if (result.direction == InvitationDirection.INCOMING &&
            result.response == InvitationResponse.ACCEPTED &&
            getMembershipHandshake(result.invitationId).getOrThrow() != null
        ) {
            Outcome.HandshakeExists
        } else {
            Outcome.NothingToRecover
        }
    }

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.RecoverAcceptedGroupInvitation

        data class CheckHandshakeExists(
            override val command: MembershipCommand.RecoverAcceptedGroupInvitation
        ) : Step

        data class HandleResult(
            override val command: MembershipCommand.RecoverAcceptedGroupInvitation
        ) : Step
    }

    sealed interface Outcome {
        data object HandshakeExists : Outcome

        data object NothingToRecover : Outcome

        data object ResultHandled : Outcome
    }
}
