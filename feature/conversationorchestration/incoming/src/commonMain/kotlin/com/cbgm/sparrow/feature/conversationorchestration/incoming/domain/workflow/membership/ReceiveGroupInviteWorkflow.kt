package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.mapper.toInvitationRecord
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityDisposition
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupInvitationIdentityPolicy
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import kotlin.reflect.KClass

/**
 * InspectOffer -> CheckRecordable -> StageConversation -> EvaluateOwnerIdentity -> [StageOwnerIdentity] -> StageOffer
 * -> [StageIdentityReview] -> RecordInvitation -> [ApplyProfilePicture]
 *
 * Expired or blocked offers are dropped so they never enter the inbox or poison the retry queue. A replayed or
 * no longer stageable offer is still handed to Membership without staging. A signed group offer proves the
 * PROPOSED keys only: changed owner keys become a separate identity review and never touch trusted keys,
 * conversations or profile data.
 */
@Suppress("LongParameterList")
class ReceiveGroupInviteWorkflow internal constructor(
    private val inspectIncomingGroupMembership: InspectIncomingGroupMembershipUseCase,
    private val blocklistRepository: ContactBlocklistRepository,
    private val shouldRecordPendingInvitation: ShouldRecordPendingInvitationUseCase,
    private val receiveIncomingGroupMembership: ReceiveIncomingGroupMembershipUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val conversationRepository: GroupConversationRepository,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase,
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
            is Step.InspectOffer -> {
                val offer = inspectIncomingGroupMembership(context.contactId, packet).getOrThrow()
                if (
                    blocklistRepository.isBlocked(context.contactId) || offer.expiresAtEpochMilliseconds <= context.receivedAtEpochMilliseconds
                ) {
                    Outcome.Dropped
                } else {
                    Outcome.OfferValid(offer)
                }
            }
            is Step.CheckRecordable ->
                if (
                    shouldRecordPendingInvitation(
                        step.offer.toInvitationRecord(context.receivedAtEpochMilliseconds)
                    ).getOrThrow()
                ) {
                    Outcome.Recordable(step.offer)
                } else {
                    Outcome.AlreadyHandled(step.offer)
                }
            is Step.ReceiveWithoutStaging -> {
                receiveIncomingGroupMembership(
                    context.contactId,
                    packet,
                    context.receivedAtEpochMilliseconds,
                    shouldStage = false
                ).getOrThrow()
                Outcome.ReceivedWithoutStaging(step.offer, step.refreshProfilePicture)
            }
            is Step.CheckPinnedOwner -> {
                val pinned = getRemoteIdentity(context.contactId).getOrThrow()
                val matches =
                    pinned != null &&
                        pinned.encryptionPublicKey.contentEquals(step.offer.ownerEncryptionPublicKey) &&
                        pinned.signingPublicKey.contentEquals(step.offer.ownerSigningPublicKey)
                if (matches) Outcome.OwnerKeysTrusted else Outcome.Done
            }
            is Step.StageConversation ->
                if (
                    conversationRepository.stageIncoming(
                        groupId = step.offer.groupId,
                        title = step.offer.title,
                        createdAtEpochMilliseconds = step.offer.createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    ).getOrThrow()
                ) {
                    Outcome.ConversationStaged(step.offer)
                } else {
                    Outcome.ConversationDeletedLocally(step.offer)
                }
            is Step.EvaluateOwnerIdentity -> {
                val stored = getRemoteIdentity(context.contactId).getOrThrow()
                Outcome.OwnerIdentityEvaluated(
                    step.offer,
                    GroupInvitationIdentityPolicy.evaluate(
                        storedEncryptionPublicKey = stored?.encryptionPublicKey,
                        storedSigningPublicKey = stored?.signingPublicKey,
                        offeredEncryptionPublicKey = step.offer.ownerEncryptionPublicKey,
                        offeredSigningPublicKey = step.offer.ownerSigningPublicKey
                    )
                )
            }
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
                stagePendingRemoteIdentityChange(
                    PendingRemoteIdentityChange(
                        peerId = context.contactId,
                        sourcePeerId = context.contactId,
                        invitationId = step.offer.sourceId,
                        proposedEncryptionPublicKey = step.offer.ownerEncryptionPublicKey.copyOf(),
                        proposedSigningPublicKey = step.offer.ownerSigningPublicKey.copyOf(),
                        receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = step.offer.expiresAtEpochMilliseconds
                    )
                ).getOrThrow()
                Outcome.IdentityReviewStaged(step.offer)
            }
            is Step.RecordInvitation -> {
                recordPendingInvitation(step.offer.toInvitationRecord(context.receivedAtEpochMilliseconds)).getOrThrow()
                if (step.ownerKeysChanged) Outcome.Done else Outcome.OwnerKeysTrusted
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
            is Outcome.Recordable -> Transition.Next(Step.StageConversation(outcome.offer, step.command))
            is Outcome.AlreadyHandled ->
                Transition.Next(
                    Step.ReceiveWithoutStaging(outcome.offer, refreshProfilePicture = true, command = step.command)
                )
            is Outcome.ConversationDeletedLocally ->
                Transition.Next(
                    Step.ReceiveWithoutStaging(outcome.offer, refreshProfilePicture = false, command = step.command)
                )
            is Outcome.ReceivedWithoutStaging ->
                if (outcome.refreshProfilePicture) {
                    Transition.Next(Step.CheckPinnedOwner(outcome.offer, step.command))
                } else {
                    Transition.Complete()
                }
            Outcome.OwnerKeysTrusted -> Transition.Next(Step.ApplyProfilePicture(step.command))
            is Outcome.ConversationStaged -> Transition.Next(Step.EvaluateOwnerIdentity(outcome.offer, step.command))
            is Outcome.OwnerIdentityEvaluated ->
                when (outcome.disposition) {
                    GroupInvitationIdentityDisposition.FIRST_CONTACT ->
                        Transition.Next(Step.StageOwnerIdentity(outcome.offer, step.command))
                    GroupInvitationIdentityDisposition.MATCHES_STORED_IDENTITY ->
                        Transition.Next(
                            Step.StageOffer(outcome.offer, ownerKeysChanged = false, command = step.command)
                        )
                    GroupInvitationIdentityDisposition.REQUIRES_REVIEW ->
                        Transition.Next(Step.StageOffer(outcome.offer, ownerKeysChanged = true, command = step.command))
                }
            is Outcome.OwnerIdentityStaged ->
                Transition.Next(Step.StageOffer(outcome.offer, ownerKeysChanged = false, command = step.command))
            is Outcome.OfferStaged ->
                if (outcome.ownerKeysChanged) {
                    Transition.Next(Step.StageIdentityReview(outcome.offer, step.command))
                } else {
                    Transition.Next(
                        Step.RecordInvitation(outcome.offer, ownerKeysChanged = false, command = step.command)
                    )
                }
            is Outcome.IdentityReviewStaged ->
                Transition.Next(Step.RecordInvitation(outcome.offer, ownerKeysChanged = true, command = step.command))
            Outcome.Done -> Transition.Complete()
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
