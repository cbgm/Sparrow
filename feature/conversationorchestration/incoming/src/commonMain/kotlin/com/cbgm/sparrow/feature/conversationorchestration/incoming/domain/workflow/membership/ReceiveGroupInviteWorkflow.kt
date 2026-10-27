package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper.toInvitationRecord
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityDisposition
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckGroupInvitationTargetUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.EvaluateGroupInvitationIdentityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.InspectIncomingGroupOfferUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StageIncomingIdentityReviewUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import kotlin.reflect.KClass

/**
 * InspectOffer -> CheckRecordable -> CheckTarget -> StageConversation -> EvaluateOwnerIdentity -> [StageOwnerIdentity]
 * -> StageOffer -> [StageIdentityReview] -> RecordInvitation -> [ApplyProfilePicture]
 *
 * Expired or blocked offers are dropped so they never enter the inbox or poison the retry queue. A replayed or
 * no longer stageable offer is still handed to Membership without staging. A signed group offer proves the
 * PROPOSED keys only: changed owner keys become a separate identity review and never touch trusted keys,
 * conversations or profile data. An offer whose group ID names an existing conversation other than its own
 * pending invitation is rejected before anything is staged.
 */
@Suppress("LongParameterList")
class ReceiveGroupInviteWorkflow internal constructor(
    private val inspectIncomingGroupOffer: InspectIncomingGroupOfferUseCase,
    private val shouldRecordPendingInvitation: ShouldRecordPendingInvitationUseCase,
    private val receiveIncomingGroupMembership: ReceiveIncomingGroupMembershipUseCase,
    private val checkGroupInvitationTarget: CheckGroupInvitationTargetUseCase,
    private val evaluateGroupInvitationIdentity: EvaluateGroupInvitationIdentityUseCase,
    private val conversationRepository: GroupConversationRepository,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val stageIncomingIdentityReview: StageIncomingIdentityReviewUseCase,
    private val recordPendingInvitation: RecordPendingInvitationUseCase,
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupInvite,
        ReceiveGroupInviteWorkflow.Step,
        ReceiveGroupInviteWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupInvite> = IncomingCommand.ReceiveGroupInvite::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupInvite): Step = Step.InspectOffer(command)

    @Suppress("LongMethod")
    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.InspectOffer ->
                inspectIncomingGroupOffer(context.contactId, packet, context.receivedAtEpochMilliseconds).getOrThrow()
                    ?.let(Outcome::OfferValid)
                    ?: Outcome.Dropped
            is Step.CheckRecordable -> checkRecordable(step.offer, context.receivedAtEpochMilliseconds)
            is Step.ReceiveWithoutStaging -> {
                receiveIncomingGroupMembership(
                    context.contactId,
                    packet,
                    context.receivedAtEpochMilliseconds,
                    shouldStage = false
                ).getOrThrow()
                Outcome.ReceivedWithoutStaging(step.offer, step.refreshProfilePicture)
            }
            is Step.CheckPinnedOwner -> checkPinnedOwner(step.offer, context.contactId)
            is Step.CheckTarget -> {
                checkGroupInvitationTarget(step.offer, context.contactId).getOrThrow()
                Outcome.TargetAllowed(step.offer)
            }
            is Step.StageConversation -> stageConversation(step.offer, context.receivedAtEpochMilliseconds)
            is Step.EvaluateOwnerIdentity ->
                Outcome.OwnerIdentityEvaluated(
                    step.offer,
                    evaluateGroupInvitationIdentity(
                        context.contactId,
                        step.offer.ownerEncryptionPublicKey,
                        step.offer.ownerSigningPublicKey
                    ).getOrThrow()
                )
            is Step.StageOwnerIdentity -> {
                stageRemoteIdentity(
                    context.contactId,
                    step.offer.ownerEncryptionPublicKey,
                    step.offer.ownerSigningPublicKey
                ).getOrThrow()
                Outcome.OwnerIdentityStaged(step.offer)
            }
            is Step.StageOffer -> {
                receiveIncomingGroupMembership(
                    context.contactId,
                    packet,
                    context.receivedAtEpochMilliseconds,
                    shouldStage = true
                ).getOrThrow()
                Outcome.OfferStaged(step.offer, step.ownerKeysChanged)
            }
            is Step.StageIdentityReview -> {
                stageIncomingIdentityReview.forGroupOffer(
                    step.offer,
                    context.contactId,
                    context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.IdentityReviewStaged(step.offer)
            }
            is Step.RecordInvitation -> {
                recordPendingInvitation(step.offer.toInvitationRecord(context.receivedAtEpochMilliseconds)).getOrThrow()
                invitationRecorded(step.ownerKeysChanged)
            }
            is Step.ApplyProfilePicture -> {
                applyPeerProfilePicture(context.contactId, packet.profilePicture)
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Dropped -> Transition.Complete()
            is Outcome.OfferValid -> Transition.Next(Step.CheckRecordable(outcome.offer, step.command))
            is Outcome.Recordable -> Transition.Next(Step.CheckTarget(outcome.offer, step.command))
            is Outcome.TargetAllowed -> Transition.Next(Step.StageConversation(outcome.offer, step.command))
            is Outcome.AlreadyHandled ->
                Transition.Next(
                    Step.ReceiveWithoutStaging(outcome.offer, refreshProfilePicture = true, command = step.command)
                )
            is Outcome.ConversationDeletedLocally ->
                Transition.Next(
                    Step.ReceiveWithoutStaging(outcome.offer, refreshProfilePicture = false, command = step.command)
                )
            is Outcome.ReceivedWithoutStaging -> afterReceivedWithoutStaging(outcome, step.command)
            Outcome.OwnerKeysTrusted -> Transition.Next(Step.ApplyProfilePicture(step.command))
            is Outcome.ConversationStaged -> Transition.Next(Step.EvaluateOwnerIdentity(outcome.offer, step.command))
            is Outcome.OwnerIdentityEvaluated -> afterOwnerIdentityEvaluated(outcome, step.command)
            is Outcome.OwnerIdentityStaged ->
                Transition.Next(Step.StageOffer(outcome.offer, ownerKeysChanged = false, command = step.command))
            is Outcome.OfferStaged -> afterOfferStaged(outcome, step.command)
            is Outcome.IdentityReviewStaged ->
                Transition.Next(Step.RecordInvitation(outcome.offer, ownerKeysChanged = true, command = step.command))
            Outcome.Done -> Transition.Complete()
        }

    private suspend fun checkRecordable(offer: IncomingMembershipOffer, receivedAtEpochMilliseconds: Long): Outcome =
        if (shouldRecordPendingInvitation(offer.toInvitationRecord(receivedAtEpochMilliseconds)).getOrThrow()) {
            Outcome.Recordable(offer)
        } else {
            Outcome.AlreadyHandled(offer)
        }

    private suspend fun checkPinnedOwner(offer: IncomingMembershipOffer, ownerContactId: String): Outcome =
        if (
            evaluateGroupInvitationIdentity.isPinned(
                ownerContactId,
                offer.ownerEncryptionPublicKey,
                offer.ownerSigningPublicKey
            ).getOrThrow()
        ) {
            Outcome.OwnerKeysTrusted
        } else {
            Outcome.Done
        }

    private suspend fun stageConversation(offer: IncomingMembershipOffer, receivedAtEpochMilliseconds: Long): Outcome =
        if (
            conversationRepository.stageIncoming(
                groupId = offer.groupId,
                title = offer.title,
                createdAtEpochMilliseconds = offer.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = receivedAtEpochMilliseconds
            ).getOrThrow()
        ) {
            Outcome.ConversationStaged(offer)
        } else {
            Outcome.ConversationDeletedLocally(offer)
        }

    /** Profile data is applied only under trusted owner keys. */
    private fun invitationRecorded(ownerKeysChanged: Boolean): Outcome =
        if (ownerKeysChanged) Outcome.Done else Outcome.OwnerKeysTrusted

    private fun afterReceivedWithoutStaging(
        outcome: Outcome.ReceivedWithoutStaging,
        command: IncomingCommand.ReceiveGroupInvite
    ): Transition<Step> =
        if (outcome.refreshProfilePicture) {
            Transition.Next(Step.CheckPinnedOwner(outcome.offer, command))
        } else {
            Transition.Complete()
        }

    private fun afterOwnerIdentityEvaluated(
        outcome: Outcome.OwnerIdentityEvaluated,
        command: IncomingCommand.ReceiveGroupInvite
    ): Transition<Step> =
        when (outcome.disposition) {
            GroupInvitationIdentityDisposition.FIRST_CONTACT ->
                Transition.Next(Step.StageOwnerIdentity(outcome.offer, command))
            GroupInvitationIdentityDisposition.MATCHES_STORED_IDENTITY ->
                Transition.Next(Step.StageOffer(outcome.offer, ownerKeysChanged = false, command = command))
            GroupInvitationIdentityDisposition.REQUIRES_REVIEW ->
                Transition.Next(Step.StageOffer(outcome.offer, ownerKeysChanged = true, command = command))
        }

    private fun afterOfferStaged(
        outcome: Outcome.OfferStaged,
        command: IncomingCommand.ReceiveGroupInvite
    ): Transition<Step> =
        if (outcome.ownerKeysChanged) {
            Transition.Next(Step.StageIdentityReview(outcome.offer, command))
        } else {
            Transition.Next(Step.RecordInvitation(outcome.offer, ownerKeysChanged = false, command = command))
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupInvite

        data class InspectOffer(
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class CheckRecordable(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        /** An already handled replay may refresh the profile picture, but only under the pinned owner keys. */
        data class ReceiveWithoutStaging(
            val offer: IncomingMembershipOffer,
            val refreshProfilePicture: Boolean,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class CheckPinnedOwner(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class CheckTarget(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class StageConversation(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class EvaluateOwnerIdentity(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class StageOwnerIdentity(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class StageOffer(
            val offer: IncomingMembershipOffer,
            val ownerKeysChanged: Boolean,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class StageIdentityReview(
            val offer: IncomingMembershipOffer,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class RecordInvitation(
            val offer: IncomingMembershipOffer,
            val ownerKeysChanged: Boolean,
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step

        data class ApplyProfilePicture(
            override val command: IncomingCommand.ReceiveGroupInvite
        ) : Step
    }

    sealed interface Outcome {
        data object Dropped : Outcome

        data class OfferValid(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class Recordable(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class TargetAllowed(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class AlreadyHandled(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class ReceivedWithoutStaging(
            val offer: IncomingMembershipOffer,
            val refreshProfilePicture: Boolean
        ) : Outcome

        /** The sender's keys are the trusted ones, so its profile data may be applied. */
        data object OwnerKeysTrusted : Outcome

        data class ConversationStaged(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class ConversationDeletedLocally(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class OwnerIdentityEvaluated(
            val offer: IncomingMembershipOffer,
            val disposition: GroupInvitationIdentityDisposition
        ) : Outcome

        data class OwnerIdentityStaged(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data class OfferStaged(
            val offer: IncomingMembershipOffer,
            val ownerKeysChanged: Boolean
        ) : Outcome

        data class IdentityReviewStaged(
            val offer: IncomingMembershipOffer
        ) : Outcome

        data object Done : Outcome
    }
}
