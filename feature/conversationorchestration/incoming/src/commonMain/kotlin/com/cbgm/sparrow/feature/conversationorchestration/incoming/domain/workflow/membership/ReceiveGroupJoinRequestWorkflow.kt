package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityDisposition
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.EvaluateGroupInvitationIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StageIncomingIdentityReviewUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmGroupMemberIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
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
    private val evaluateGroupInvitationIdentity: EvaluateGroupInvitationIdentityUseCase,
    private val stageIncomingIdentityReview: StageIncomingIdentityReviewUseCase,
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
            is Step.EvaluateMemberIdentity -> evaluateMemberIdentity(step.join)
            is Step.StageIdentityReview -> {
                stageIncomingIdentityReview.forGroupJoin(step.join, context.receivedAtEpochMilliseconds).getOrThrow()
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

    private suspend fun evaluateMemberIdentity(join: MembershipJoinRequest): Outcome =
        when (
            evaluateGroupInvitationIdentity(
                join.peerId,
                join.memberEncryptionPublicKey,
                join.memberSigningPublicKey
            ).getOrThrow()
        ) {
            GroupInvitationIdentityDisposition.REQUIRES_REVIEW -> Outcome.IdentityChanged(join)
            GroupInvitationIdentityDisposition.FIRST_CONTACT,
            GroupInvitationIdentityDisposition.MATCHES_STORED_IDENTITY -> Outcome.IdentityAcceptable(join)
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
}
