package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSourceIds
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import kotlin.reflect.KClass

/**
 * group, owner side:  FindGroupMembership -> ResumeGroupJoin
 * group, member side: FindGroupMembership                      (the offer waits in the inbox)
 * direct:             FindGroupMembership -> EnsureConversation -> AcceptOriginalInvitation | RequestReauthorization
 *
 * A group approval never becomes a direct invitation, even when its membership was withdrawn before this
 * ran. A direct chat shell does not grant transport authorization or release queued messages.
 */
class StartRecoveryInvitationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val conversationRepository: DirectConversationRepository
) : WorkflowStateMachine<
        LifecycleCommand.StartRecoveryInvitation,
        StartRecoveryInvitationWorkflow.Step,
        StartRecoveryInvitationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.StartRecoveryInvitation> =
        LifecycleCommand.StartRecoveryInvitation::class

    override fun firstStep(command: LifecycleCommand.StartRecoveryInvitation): Step = Step.FindGroupMembership(command)

    override suspend fun perform(step: Step): Outcome {
        val approval = step.command.approval
        return when (step) {
            is Step.FindGroupMembership ->
                groupMembershipFound(getMembershipHandshake(approval.approvalId).getOrThrow(), approval)
            is Step.ResumeGroupJoin -> {
                engine.run(
                    MembershipCommand.ResumeApprovedGroupJoin(step.sourceId, approval.peerId)
                ).getOrThrow()
                Outcome.Done
            }
            is Step.EnsureConversation -> {
                conversationRepository.getOrCreate(approval.peerId).getOrThrow()
                originalInvitationChecked(approval.hasOriginalInvitation(SystemClock.nowEpochMilliseconds()))
            }
            is Step.AcceptOriginalInvitation -> {
                engine.run(LifecycleCommand.AcceptApprovedIdentityChange(approval)).getOrThrow()
                Outcome.Done
            }
            is Step.RequestReauthorization -> {
                engine.run(LifecycleCommand.RequestReauthorization(approval.peerId)).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.OwnerSideGroupJoin -> Transition.Next(Step.ResumeGroupJoin(outcome.sourceId, step.command))
            Outcome.MemberSideGroupOffer -> Transition.Complete()
            Outcome.WithdrawnGroupMembership -> Transition.Complete()
            Outcome.DirectApproval -> Transition.Next(Step.EnsureConversation(step.command))
            Outcome.OriginalInvitationValid -> Transition.Next(Step.AcceptOriginalInvitation(step.command))
            Outcome.OriginalInvitationUnusable -> Transition.Next(Step.RequestReauthorization(step.command))
            Outcome.Done -> Transition.Complete()
        }

    /**
     * On the owner side the approval belongs to a JOIN signed with replaced member keys. A group approval whose
     * membership is gone is recognised by its ID and never becomes a direct invitation.
     */
    private fun groupMembershipFound(handshake: MembershipHandshake?, approval: ApprovedIdentityReconnection): Outcome =
        when {
            handshake != null -> handshakeFound(handshake, approval)
            MembershipSourceIds.isGroupMembership(approval.approvalId) -> Outcome.WithdrawnGroupMembership
            else -> Outcome.DirectApproval
        }

    private fun handshakeFound(handshake: MembershipHandshake, approval: ApprovedIdentityReconnection): Outcome {
        check(handshake.peerId == approval.peerId) { "Approved group identity does not match its inviting contact" }
        return if (handshake.isOwnerSide) Outcome.OwnerSideGroupJoin(handshake.sourceId) else Outcome.MemberSideGroupOffer
    }

    private fun originalInvitationChecked(valid: Boolean): Outcome =
        if (valid) Outcome.OriginalInvitationValid else Outcome.OriginalInvitationUnusable

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.StartRecoveryInvitation

        data class FindGroupMembership(
            override val command: LifecycleCommand.StartRecoveryInvitation
        ) : Step

        data class ResumeGroupJoin(
            val sourceId: String,
            override val command: LifecycleCommand.StartRecoveryInvitation
        ) : Step

        data class EnsureConversation(
            override val command: LifecycleCommand.StartRecoveryInvitation
        ) : Step

        data class AcceptOriginalInvitation(
            override val command: LifecycleCommand.StartRecoveryInvitation
        ) : Step

        data class RequestReauthorization(
            override val command: LifecycleCommand.StartRecoveryInvitation
        ) : Step
    }

    sealed interface Outcome {
        data class OwnerSideGroupJoin(
            val sourceId: String
        ) : Outcome

        data object MemberSideGroupOffer : Outcome

        data object WithdrawnGroupMembership : Outcome

        data object DirectApproval : Outcome

        data object OriginalInvitationValid : Outcome

        data object OriginalInvitationUnusable : Outcome

        data object Done : Outcome
    }
}
