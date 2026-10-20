package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityDisposition
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityPolicy
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipJoinRequestUseCase
import kotlin.reflect.KClass

/**
 * ReceiveJoin -> EvaluateMemberIdentity -> StageIdentityReview                          (changed keys)
 * ReceiveJoin -> EvaluateMemberIdentity -> StageIdentity -> EstablishMutualIdentity
 *   -> ConfirmMemberIdentity -> ApplyProfilePicture
 *
 * Unlike the receipt, a JOIN binds both member keys. A different signing OR encryption key always waits
 * for the user's one-tap approval; the membership stays staged meanwhile. A duplicate JOIN after the
 * welcome has nothing left to authorize.
 */
@Suppress("LongParameterList")
class ReceiveGroupJoinRequestWorkflow internal constructor(
    private val receiveGroupMembershipJoinRequest: ReceiveGroupMembershipJoinRequestUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val establishMutualIdentity: EstablishMutualIdentityUseCase,
    private val confirmGroupMemberIdentity: ConfirmGroupMemberIdentityUseCase,
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupJoinRequest,
        ReceiveGroupJoinRequestWorkflow.Step,
        ReceiveGroupJoinRequestWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupJoinRequest> =
        IncomingCommand.ReceiveGroupJoinRequest::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupJoinRequest): Step = Step.ReceiveJoin(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        return when (step) {
            is Step.ReceiveJoin ->
                receiveGroupMembershipJoinRequest(context.contactId, step.command.packet).getOrThrow()
                    ?.takeUnless(MembershipJoinRequest::alreadyAccepted)
                    ?.let(Outcome::JoinReceived)
                    ?: Outcome.Done
            is Step.EvaluateMemberIdentity -> {
                val stored = getRemoteIdentity(step.join.peerId).getOrThrow()
                val disposition =
                    GroupInvitationIdentityPolicy.evaluate(
                        storedEncryptionPublicKey = stored?.encryptionPublicKey,
                        storedSigningPublicKey = stored?.signingPublicKey,
                        offeredEncryptionPublicKey = step.join.memberEncryptionPublicKey,
                        offeredSigningPublicKey = step.join.memberSigningPublicKey
                    )
                if (disposition == GroupInvitationIdentityDisposition.REQUIRES_REVIEW) {
                    Outcome.IdentityChanged(step.join)
                } else {
                    Outcome.IdentityAcceptable(step.join)
                }
            }
            is Step.StageIdentityReview -> {
                stageIdentityReview(step.join, context.receivedAtEpochMilliseconds)
                Outcome.Done
            }
            is Step.StageIdentity -> {
                stageRemoteIdentity(
                    step.join.peerId,
                    step.join.memberEncryptionPublicKey,
                    step.join.memberSigningPublicKey
                ).getOrThrow()
                Outcome.IdentityStaged(step.join)
            }
            is Step.EstablishMutualIdentity -> {
                establishMutualIdentity(
                    step.join.peerId,
                    step.join.memberEncryptionPublicKey,
                    step.join.memberSigningPublicKey
                ).getOrThrow()
                Outcome.MutualIdentityEstablished(step.join)
            }
            is Step.ConfirmMemberIdentity -> {
                confirmGroupMemberIdentity(
                    sourceId = step.join.sourceId,
                    groupId = step.join.groupId,
                    memberContactId = step.join.peerId,
                    memberEncryptionPublicKey = step.join.memberEncryptionPublicKey,
                    memberSigningPublicKey = step.join.memberSigningPublicKey,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.MemberIdentityConfirmed
            }
            is Step.ApplyProfilePicture -> {
                applyPeerProfilePicture(context.contactId, step.command.packet.profilePicture)
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.JoinReceived -> Transition.Next(Step.EvaluateMemberIdentity(outcome.join, step.command))
            is Outcome.IdentityChanged -> Transition.Next(Step.StageIdentityReview(outcome.join, step.command))
            is Outcome.IdentityAcceptable -> Transition.Next(Step.StageIdentity(outcome.join, step.command))
            is Outcome.IdentityStaged -> Transition.Next(Step.EstablishMutualIdentity(outcome.join, step.command))
            is Outcome.MutualIdentityEstablished ->
                Transition.Next(Step.ConfirmMemberIdentity(outcome.join, step.command))
            Outcome.MemberIdentityConfirmed -> Transition.Next(Step.ApplyProfilePicture(step.command))
            Outcome.Done -> Transition.Complete()
        }

    /** The verified JOIN is the evidence for the review; it must belong to our own pending invitation. */
    private suspend fun stageIdentityReview(join: MembershipJoinRequest, receivedAtEpochMilliseconds: Long) {
        val handshake =
            requireNotNull(getMembershipHandshake(join.sourceId).getOrThrow()) {
                "Verified JOIN has no pending group membership"
            }
        check(
            handshake.peerId == join.peerId && handshake.groupId == join.groupId && handshake.ownerSigningPublicKey == null
        ) {
            "Verified JOIN does not belong to the owner-side group invitation"
        }
        val expiresAt = handshake.createdAtEpochMilliseconds + GROUP_INVITATION_VALIDITY_MILLISECONDS
        check(expiresAt > SystemClock.nowEpochMilliseconds()) {
            "Group invitation expired before identity change could be approved"
        }
        stagePendingRemoteIdentityChange(
            PendingRemoteIdentityChange(
                peerId = join.peerId,
                sourcePeerId = join.peerId,
                invitationId = join.sourceId,
                proposedEncryptionPublicKey = join.memberEncryptionPublicKey.copyOf(),
                proposedSigningPublicKey = join.memberSigningPublicKey.copyOf(),
                receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                expiresAtEpochMilliseconds = expiresAt
            )
        ).getOrThrow()
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupJoinRequest

        data class ReceiveJoin(
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class EvaluateMemberIdentity(
            val join: MembershipJoinRequest,
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class StageIdentityReview(
            val join: MembershipJoinRequest,
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class StageIdentity(
            val join: MembershipJoinRequest,
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class EstablishMutualIdentity(
            val join: MembershipJoinRequest,
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class ConfirmMemberIdentity(
            val join: MembershipJoinRequest,
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step

        data class ApplyProfilePicture(
            override val command: IncomingCommand.ReceiveGroupJoinRequest
        ) : Step
    }

    sealed interface Outcome {
        data class JoinReceived(
            val join: MembershipJoinRequest
        ) : Outcome

        data class IdentityChanged(
            val join: MembershipJoinRequest
        ) : Outcome

        data class IdentityAcceptable(
            val join: MembershipJoinRequest
        ) : Outcome

        data class IdentityStaged(
            val join: MembershipJoinRequest
        ) : Outcome

        data class MutualIdentityEstablished(
            val join: MembershipJoinRequest
        ) : Outcome

        data object MemberIdentityConfirmed : Outcome

        data object Done : Outcome
    }

    private companion object {
        const val GROUP_INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
