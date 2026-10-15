package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.join

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
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
    private val conversationRepository: GroupConversationRepository
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
            is Step.FindHandshake -> {
                require(result.action == null) { "Group invitations do not support invitation actions" }
                if (result.direction != InvitationDirection.INCOMING) return Outcome.NotForThisDevice
                val handshake = getMembershipHandshake(result.invitationId).getOrThrow()
                when (result.response) {
                    InvitationResponse.ACCEPTED ->
                        Outcome.AcceptedHandshakeFound(requireAcceptable(handshake, step.command))
                    InvitationResponse.DECLINED -> Outcome.DeclinedHandshakeFound(handshake)
                }
            }
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
                Outcome.MembershipDeclined(step.handshakeCreatedAtEpochMilliseconds)
            }
            is Step.DiscardConversation -> {
                conversationRepository.discardPending(
                    groupId = result.payloadId,
                    updatedAtEpochMilliseconds =
                        maxOf(step.handshakeCreatedAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
                ).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NotForThisDevice -> Transition.Complete()
            is Outcome.AcceptedHandshakeFound ->
                when (outcome.handshake.status) {
                    MembershipStatus.STAGED ->
                        Transition.Next(Step.StageOwnerIdentity(outcome.handshake.requireOwnerKeys(), step.command))
                    MembershipStatus.JOIN_REQUEST_SENT,
                    MembershipStatus.WAITING_FOR_ACTIVATION,
                    MembershipStatus.ACTIVE -> Transition.Next(Step.ShowConversation(step.command))
                    else -> Transition.Complete()
                }
            is Outcome.DeclinedHandshakeFound ->
                Transition.Next(
                    Step.DeclineMembership(outcome.handshake?.createdAtEpochMilliseconds ?: 0L, step.command)
                )
            is Outcome.OwnerIdentityStaged -> Transition.Next(Step.AcceptOwnerIdentity(outcome.ownerKeys, step.command))
            Outcome.OwnerIdentityAccepted -> Transition.Next(Step.AcceptMembership(step.command))
            Outcome.MembershipAccepted -> Transition.Next(Step.ShowConversation(step.command))
            is Outcome.MembershipDeclined ->
                Transition.Next(Step.DiscardConversation(outcome.handshakeCreatedAtEpochMilliseconds, step.command))
            Outcome.Done -> Transition.Complete()
        }

    private fun requireAcceptable(
        handshake: MembershipHandshake?,
        command: MembershipCommand.HandleGroupInvitationResult
    ): MembershipHandshake {
        val accepted = requireNotNull(handshake) { "Group membership handshake was not found" }
        check(accepted.groupId == command.result.payloadId) { "Invitation belongs to the wrong group" }
        check(accepted.peerId == command.result.peerId) { "Invitation belongs to the wrong peer" }
        accepted.requireOwnerKeys()
        return accepted
    }

    private fun MembershipHandshake.requireOwnerKeys(): OwnerKeys =
        OwnerKeys(
            encryptionPublicKey =
                requireNotNull(ownerEncryptionPublicKey) { "Group owner encryption identity was not staged" },
            signingPublicKey = requireNotNull(ownerSigningPublicKey) { "Group owner signing identity was not staged" }
        )

    /** The owner identity that was verified when the offer was staged. */
    class OwnerKeys(
        val encryptionPublicKey: ByteArray,
        val signingPublicKey: ByteArray
    )

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.HandleGroupInvitationResult

        data class FindHandshake(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class StageOwnerIdentity(
            val ownerKeys: OwnerKeys,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class AcceptOwnerIdentity(
            val ownerKeys: OwnerKeys,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class AcceptMembership(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class ShowConversation(
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class DeclineMembership(
            val handshakeCreatedAtEpochMilliseconds: Long,
            override val command: MembershipCommand.HandleGroupInvitationResult
        ) : Step

        data class DiscardConversation(
            val handshakeCreatedAtEpochMilliseconds: Long,
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
            val ownerKeys: OwnerKeys
        ) : Outcome

        data object OwnerIdentityAccepted : Outcome

        data object MembershipAccepted : Outcome

        data class MembershipDeclined(
            val handshakeCreatedAtEpochMilliseconds: Long
        ) : Outcome

        data object Done : Outcome
    }
}
