package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.RecordRemoteIdentityDeclineUseCase
import com.cbgm.sparrow.protocol.invitation.ContactInvitationDeclineProtocol
import kotlin.reflect.KClass

/** VerifyDecline -> RecordDecline. */
class ReceiveContactInviteDeclinedWorkflow internal constructor(
    private val invitationDeclineProtocol: ContactInvitationDeclineProtocol,
    private val recordRemoteIdentityDecline: RecordRemoteIdentityDeclineUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveContactInviteDeclined,
        ReceiveContactInviteDeclinedWorkflow.Step,
        ReceiveContactInviteDeclinedWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveContactInviteDeclined> =
        IncomingCommand.ReceiveContactInviteDeclined::class

    override fun firstStep(command: IncomingCommand.ReceiveContactInviteDeclined): Step = Step.VerifyDecline(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.VerifyDecline -> {
                invitationDeclineProtocol.verifyPacket(packet, context.receivedAtEpochMilliseconds).getOrThrow()
                Outcome.DeclineVerified
            }
            is Step.RecordDecline -> {
                recordRemoteIdentityDecline(
                    exchangeId = packet.invitationId,
                    peerId = context.contactId,
                    inviteChallenge = packet.inviteChallenge,
                    remoteSigningPublicKey = packet.declinerSigningPublicKey,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.DeclineRecorded
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.DeclineVerified -> Transition.Next(Step.RecordDecline(step.command))
            Outcome.DeclineRecorded -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveContactInviteDeclined

        data class VerifyDecline(
            override val command: IncomingCommand.ReceiveContactInviteDeclined
        ) : Step

        data class RecordDecline(
            override val command: IncomingCommand.ReceiveContactInviteDeclined
        ) : Step
    }

    sealed interface Outcome {
        data object DeclineVerified : Outcome

        data object DeclineRecorded : Outcome
    }
}
