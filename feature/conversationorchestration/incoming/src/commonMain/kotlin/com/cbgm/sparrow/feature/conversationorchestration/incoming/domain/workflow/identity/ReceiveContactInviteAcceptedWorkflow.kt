package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.error.IdentityAcceptanceRequiresReviewException
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeBindingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeAcceptedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.protocol.invitation.ContactInvitationHandshakeProtocol
import kotlin.reflect.KClass

/**
 * VerifyAcceptance -> FindExchange -> ReceiveAcceptance -> ActivateConversation -> MarkInvitationAccepted
 * -> ApplyProfilePicture
 *
 * An acceptance whose exchange was cancelled or replaced locally cannot establish a chat and is acknowledged
 * as handled instead of being retried forever. A verified acceptance signed with keys other than the original
 * contact's is no proof of that contact's identity: it becomes an identity review and never activates the chat.
 */
@Suppress("LongParameterList")
class ReceiveContactInviteAcceptedWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val invitationHandshakeProtocol: ContactInvitationHandshakeProtocol,
    private val getIdentityExchangeBinding: GetIdentityExchangeBindingUseCase,
    private val receiveIdentityExchangeAccepted: ReceiveIdentityExchangeAcceptedUseCase,
    private val blocklistRepository: ContactBlocklistRepository,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase,
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveContactInviteAccepted,
        ReceiveContactInviteAcceptedWorkflow.Step,
        ReceiveContactInviteAcceptedWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReceiveContactInviteAcceptedWorkflow")

    override val commandType: KClass<IncomingCommand.ReceiveContactInviteAccepted> =
        IncomingCommand.ReceiveContactInviteAccepted::class

    override fun firstStep(command: IncomingCommand.ReceiveContactInviteAccepted): Step = Step.VerifyAcceptance(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.VerifyAcceptance -> {
                invitationHandshakeProtocol.verifyAccepted(packet, context.receivedAtEpochMilliseconds).getOrThrow()
                Outcome.AcceptanceVerified
            }
            is Step.FindExchange ->
                if (getIdentityExchangeBinding(packet.invitationId).getOrThrow() == null) {
                    logger.warn {
                        "Ignoring unmatched invitation acceptance: invitationId=${packet.invitationId}, contactId=${context.contactId}"
                    }
                    Outcome.Done
                } else {
                    Outcome.ExchangeFound
                }
            is Step.ReceiveAcceptance ->
                try {
                    receiveIdentityExchangeAccepted(
                        context,
                        IdentityExchangeAcceptance(
                            exchangeId = packet.invitationId,
                            acceptedAtEpochMilliseconds = packet.acceptedAtEpochMilliseconds,
                            inviteChallenge = packet.inviteChallenge,
                            responseChallenge = packet.responseChallenge,
                            inviterEncryptionPublicKey = packet.inviterEncryptionPublicKey,
                            inviterSigningPublicKey = packet.inviterSigningPublicKey,
                            responderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                            responderSigningPublicKey = packet.responderSigningPublicKey,
                            autoSharesIdentity = packet.autoSharesIdentity
                        )
                    ).getOrThrow()
                    Outcome.AcceptanceReceived
                } catch (review: IdentityAcceptanceRequiresReviewException) {
                    Outcome.ReviewRequired(review.peerId, review.invitationExpiresAtEpochMilliseconds)
                }
            is Step.StageIdentityReview -> {
                if (!blocklistRepository.isBlocked(step.peerId).getOrThrow()) {
                    check(step.peerId == context.contactId) { "Identity review does not match the invitation contact" }
                    stagePendingRemoteIdentityChange(
                        PendingRemoteIdentityChange(
                            peerId = step.peerId,
                            sourcePeerId = context.contactId,
                            invitationId = packet.invitationId,
                            proposedEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                            proposedSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                            expiresAtEpochMilliseconds = step.invitationExpiresAtEpochMilliseconds
                        )
                    ).getOrThrow()
                }
                Outcome.Done
            }
            is Step.ActivateConversation -> {
                engine.run(
                    MessageCommand.ActivateAuthorizedConversation(context.contactId)
                ).getOrThrow()
                Outcome.ConversationActivated
            }
            is Step.MarkInvitationAccepted -> {
                handleInvitationResponse(
                    InvitationPayloadType.DIRECT,
                    packet.invitationId,
                    InvitationResponse.ACCEPTED
                ).getOrThrow()
                Outcome.InvitationAccepted
            }
            is Step.ApplyProfilePicture -> {
                applyPeerProfilePicture(context.contactId, packet.profilePicture)
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AcceptanceVerified -> Transition.Next(Step.FindExchange(step.command))
            Outcome.ExchangeFound -> Transition.Next(Step.ReceiveAcceptance(step.command))
            Outcome.AcceptanceReceived -> Transition.Next(Step.ActivateConversation(step.command))
            is Outcome.ReviewRequired ->
                Transition.Next(
                    Step.StageIdentityReview(outcome.peerId, outcome.invitationExpiresAtEpochMilliseconds, step.command)
                )
            Outcome.ConversationActivated -> Transition.Next(Step.MarkInvitationAccepted(step.command))
            Outcome.InvitationAccepted -> Transition.Next(Step.ApplyProfilePicture(step.command))
            Outcome.Done -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveContactInviteAccepted

        data class VerifyAcceptance(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class FindExchange(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class ReceiveAcceptance(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class StageIdentityReview(
            val peerId: String,
            val invitationExpiresAtEpochMilliseconds: Long,
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class ActivateConversation(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class MarkInvitationAccepted(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step

        data class ApplyProfilePicture(
            override val command: IncomingCommand.ReceiveContactInviteAccepted
        ) : Step
    }

    sealed interface Outcome {
        data object AcceptanceVerified : Outcome

        data object ExchangeFound : Outcome

        data object AcceptanceReceived : Outcome

        data class ReviewRequired(
            val peerId: String,
            val invitationExpiresAtEpochMilliseconds: Long
        ) : Outcome

        data object ConversationActivated : Outcome

        data object InvitationAccepted : Outcome

        data object Done : Outcome
    }
}
