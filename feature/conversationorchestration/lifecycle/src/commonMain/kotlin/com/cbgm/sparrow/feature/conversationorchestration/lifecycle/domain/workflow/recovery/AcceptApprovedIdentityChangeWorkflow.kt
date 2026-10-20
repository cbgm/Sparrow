package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.InvalidatePendingInvitationUseCase
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import kotlin.reflect.KClass

/**
 * ValidateApproval -> CheckPinnedIdentity -> AcceptOriginalInvitation -> InvalidateStaleInvitation
 *
 * The approved path never publishes INCOMING_CHALLENGE_RECEIVED, so the Identity result observer cannot
 * turn it into a normal invitation again. READY still activates the existing conversation later.
 */
class AcceptApprovedIdentityChangeWorkflow internal constructor(
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val receiveIdentityExchange: ReceiveIdentityExchangeUseCase,
    private val invalidatePendingInvitation: InvalidatePendingInvitationUseCase
) : WorkflowStateMachine<
        LifecycleCommand.AcceptApprovedIdentityChange,
        AcceptApprovedIdentityChangeWorkflow.Step,
        AcceptApprovedIdentityChangeWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.AcceptApprovedIdentityChange> =
        LifecycleCommand.AcceptApprovedIdentityChange::class

    override fun firstStep(command: LifecycleCommand.AcceptApprovedIdentityChange): Step =
        Step.ValidateApproval(command)

    override suspend fun perform(step: Step): Outcome {
        val approval = step.command.approval
        return when (step) {
            is Step.ValidateApproval -> Outcome.ApprovalValid(approval.toOriginalOffer())
            is Step.CheckPinnedIdentity -> {
                val pinned =
                    requireNotNull(getRemoteIdentity(approval.peerId).getOrThrow()) {
                        "Approved replacement identity is missing"
                    }
                check(
                    pinned.encryptionPublicKey.contentEquals(step.offer.encryptionPublicKey) &&
                        pinned.signingPublicKey.contentEquals(step.offer.signingPublicKey)
                ) { "Approved invitation no longer matches the pinned contact identity" }
                Outcome.PinnedIdentityMatches(step.offer)
            }
            is Step.AcceptOriginalInvitation -> {
                receiveIdentityExchange.acceptApproved(
                    context = approval.toPacketContext(),
                    offer = step.offer,
                    wasKnownPeerAtReceive = true
                ).getOrThrow()
                Outcome.OriginalInvitationAccepted
            }
            is Step.InvalidateStaleInvitation -> {
                // Older recovery builds created a normal inbox row under the original invitation ID.
                invalidatePendingInvitation(approval.approvalId).getOrThrow()
                Outcome.StaleInvitationInvalidated
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ApprovalValid -> Transition.Next(Step.CheckPinnedIdentity(outcome.offer, step.command))
            is Outcome.PinnedIdentityMatches ->
                Transition.Next(Step.AcceptOriginalInvitation(outcome.offer, step.command))
            Outcome.OriginalInvitationAccepted -> Transition.Next(Step.InvalidateStaleInvitation(step.command))
            Outcome.StaleInvitationInvalidated -> Transition.Complete()
        }

    private fun ApprovedIdentityReconnection.toOriginalOffer(): IdentityExchangeOffer {
        val challenge = requireNotNull(originalInviteChallenge)
        val createdAt = requireNotNull(originalInviteCreatedAtEpochMilliseconds)
        val expiresAt = requireNotNull(originalInviteExpiresAtEpochMilliseconds)
        val encryption = requireNotNull(originalInviterEncryptionPublicKey)
        val signing = requireNotNull(originalInviterSigningPublicKey)
        check(challenge.size == KEY_SIZE && encryption.size == KEY_SIZE && signing.size == KEY_SIZE)
        check(createdAt < expiresAt && expiresAt > SystemClock.nowEpochMilliseconds()) {
            "Approved original identity invitation expired"
        }
        return IdentityExchangeOffer(
            exchangeId = approvalId,
            createdAtEpochMilliseconds = createdAt,
            expiresAtEpochMilliseconds = expiresAt,
            inviteChallenge = challenge,
            encryptionPublicKey = encryption,
            signingPublicKey = signing,
            autoSharesIdentity = originalInviteAutoSharesIdentity
        )
    }

    private fun ApprovedIdentityReconnection.toPacketContext(): IncomingPacketContext =
        IncomingPacketContext(
            contactId = peerId,
            conversationId = peerId,
            encodedTransportPayload = "persisted-signed-invitation:$approvalId",
            transportMode = "approved-local-proposal",
            receivedAtEpochMilliseconds = approvedAtEpochMilliseconds
        )

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.AcceptApprovedIdentityChange

        data class ValidateApproval(
            override val command: LifecycleCommand.AcceptApprovedIdentityChange
        ) : Step

        data class CheckPinnedIdentity(
            val offer: IdentityExchangeOffer,
            override val command: LifecycleCommand.AcceptApprovedIdentityChange
        ) : Step

        data class AcceptOriginalInvitation(
            val offer: IdentityExchangeOffer,
            override val command: LifecycleCommand.AcceptApprovedIdentityChange
        ) : Step

        data class InvalidateStaleInvitation(
            override val command: LifecycleCommand.AcceptApprovedIdentityChange
        ) : Step
    }

    sealed interface Outcome {
        data class ApprovalValid(
            val offer: IdentityExchangeOffer
        ) : Outcome

        data class PinnedIdentityMatches(
            val offer: IdentityExchangeOffer
        ) : Outcome

        data object OriginalInvitationAccepted : Outcome

        data object StaleInvitationInvalidated : Outcome
    }

    private companion object {
        const val KEY_SIZE = 32
    }
}
