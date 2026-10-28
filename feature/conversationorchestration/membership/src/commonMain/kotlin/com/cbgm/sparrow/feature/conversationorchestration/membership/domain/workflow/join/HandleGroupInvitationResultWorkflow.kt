package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.DiscardDeclinedGroupConversationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipOwnerKeys
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.requireAcceptableFor
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import kotlin.reflect.KClass

/**
 * accepted, staged:  FindHandshake -> StageOwnerIdentity -> AcceptOwnerIdentity -> AcceptMembership -> ShowConversation
 * accepted, joining: FindHandshake -> ShowConversation   (an accepted invitation is replayable after restart)
 * declined:          FindHandshake -> DeclineMembership -> DiscardConversation
 *
 * The owner keys were verified when this exact offer was staged. A half-joined group is never shown:
 * the conversation appears only after identity and join preflight succeeded.
 */
class HandleGroupInvitationResultWorkflow internal constructor(
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val acceptRemoteIdentityHandshake: AcceptRemoteIdentityHandshakeUseCase,
    private val acceptGroupMembership: AcceptGroupMembershipUseCase,
    private val declineGroupMembership: DeclineGroupMembershipUseCase,
    private val conversationRepository: GroupConversationRepository,
    private val discardDeclinedGroupConversation: DiscardDeclinedGroupConversationUseCase
) : WorkflowStateMachine<
        MembershipCommand.HandleGroupInvitationResult,
        HandleGroupInvitationResultWorkflow.Step,
        HandleGroupInvitationResultWorkflow.Outcome
    > {
    override val commandType: KClass<MembershipCommand.HandleGroupInvitationResult> =
        MembershipCommand.HandleGroupInvitationResult::class

    override fun firstStep(command: MembershipCommand.HandleGroupInvitationResult): Step = Step.FindHandshake(command)

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.FindHandshake -> findHandshake(step.command)
            is Step.StageOwnerIdentity -> {
                stageRemoteIdentity(
                    result.peerId,
                    step.ownerKeys.encryptionPublicKey,
                    step.ownerKeys.signingPublicKey
                ).getOrThrow()
                Outcome.OwnerIdentityStaged(step.ownerKeys)
            }
            is Step.AcceptOwnerIdentity -> {
                acceptRemoteIdentityHandshake(
                    result.peerId,
                    step.ownerKeys.encryptionPublicKey,
                    step.ownerKeys.signingPublicKey
                )
                    .getOrThrow()
                Outcome.OwnerIdentityAccepted
            }
            is Step.AcceptMembership -> {
                acceptGroupMembership(result.invitationId).getOrThrow()
                Outcome.MembershipAccepted
            }
            is Step.ShowConversation -> {
                conversationRepository.showAcceptedIncoming(result.payloadId).getOrThrow()
                Outcome.Done
            }
            is Step.DeclineMembership -> {
                declineGroupMembership(result.invitationId).getOrThrow()
                Outcome.MembershipDeclined(step.handshake)
            }
            is Step.DiscardConversation -> {
                discardDeclinedGroupConversation(result.payloadId, step.handshake).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NotForThisDevice -> Transition.Complete()
            is Outcome.AcceptedHandshakeFound -> afterAcceptedHandshake(outcome.handshake, step.command)
            is Outcome.DeclinedHandshakeFound -> Transition.Next(Step.DeclineMembership(outcome.handshake, step.command))
            is Outcome.OwnerIdentityStaged -> Transition.Next(Step.AcceptOwnerIdentity(outcome.ownerKeys, step.command))
            Outcome.OwnerIdentityAccepted -> Transition.Next(Step.AcceptMembership(step.command))
            Outcome.MembershipAccepted -> Transition.Next(Step.ShowConversation(step.command))
            is Outcome.MembershipDeclined ->
                Transition.Next(Step.DiscardConversation(outcome.handshake, step.command))
            Outcome.Done -> Transition.Complete()
        }

    private suspend fun findHandshake(command: MembershipCommand.HandleGroupInvitationResult): Outcome {
        val result = command.result
        require(result.action == null) { "Group invitations do not support invitation actions" }
        if (result.direction != InvitationDirection.INCOMING) return Outcome.NotForThisDevice
        val handshake = getMembershipHandshake(result.invitationId).getOrThrow()
        return when (result.response) {
            InvitationResponse.ACCEPTED ->
                Outcome.AcceptedHandshakeFound(handshake.requireAcceptableFor(result.payloadId, result.peerId))
            InvitationResponse.DECLINED -> Outcome.DeclinedHandshakeFound(handshake)
        }
    }

    /** A staged offer still needs the owner identity; a join underway only shows the conversation again. */
    private fun afterAcceptedHandshake(
        handshake: MembershipHandshake,
        command: MembershipCommand.HandleGroupInvitationResult
    ): Transition<Step> =
        when {
            handshake.status == MembershipStatus.STAGED ->
                Transition.Next(Step.StageOwnerIdentity(handshake.requireOwnerKeys(), command))
            handshake.status.isJoinInProgress -> Transition.Next(Step.ShowConversation(command))
            else -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.HandleGroupInvitationResult

        data class FindHandshake(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class StageOwnerIdentity(
            val ownerKeys: MembershipOwnerKeys,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class AcceptOwnerIdentity(
            val ownerKeys: MembershipOwnerKeys,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class AcceptMembership(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class ShowConversation(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class DeclineMembership(
            val handshake: MembershipHandshake?,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class DiscardConversation(
            val handshake: MembershipHandshake?,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step
    }

    sealed interface Outcome {
        data object NotForThisDevice : Outcome

        data class AcceptedHandshakeFound(
            val handshake: MembershipHandshake
        ) : Outcome

        data class DeclinedHandshakeFound(
            val handshake: MembershipHandshake?
        ) : Outcome

        data class OwnerIdentityStaged(
            val ownerKeys: MembershipOwnerKeys
        ) : Outcome

        data object OwnerIdentityAccepted : Outcome

        data object MembershipAccepted : Outcome

        data class MembershipDeclined(
            val handshake: MembershipHandshake?
        ) : Outcome

        data object Done : Outcome
    }
}
