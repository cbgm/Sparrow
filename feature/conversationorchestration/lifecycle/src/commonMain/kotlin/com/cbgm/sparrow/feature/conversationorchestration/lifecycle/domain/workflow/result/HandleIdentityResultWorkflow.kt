package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeDirection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.SendIdentityVerificationReceiptUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.InvalidatePendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase
import kotlin.reflect.KClass

/**
 * INCOMING_EXCHANGE:    ResolveDisplayName -> RecordIncomingInvitation
 * ESTABLISHED: [MarkInvitationAccepted] -> ActivateConversation -> LoadSetupMode -> [SendVerificationReceipt]
 * REMOTE_DECLINED:      MarkInvitationDeclined -> DiscardWaitingMessages
 * FAILED:               MarkTransportFailed
 * EXCHANGE_INVALIDATED: InvalidateInvitation
 *
 * Invitation acceptance, not the identity setup mode, authorizes the conversation: queued messages are
 * released in both modes.
 */
class HandleIdentityResultWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getIdentityPeerDisplayName: GetIdentityPeerDisplayNameUseCase,
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val sendIdentityVerificationReceipt: SendIdentityVerificationReceiptUseCase,
    private val markInvitationTransportFailed: MarkInvitationTransportFailedUseCase,
    private val invalidatePendingInvitation: InvalidatePendingInvitationUseCase
) : WorkflowStateMachine<
        LifecycleCommand.HandleIdentityResult,
        HandleIdentityResultWorkflow.Step,
        HandleIdentityResultWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("HandleIdentityResultWorkflow")

    override val commandType: KClass<LifecycleCommand.HandleIdentityResult> =
        LifecycleCommand.HandleIdentityResult::class

    override fun firstStep(command: LifecycleCommand.HandleIdentityResult): Step =
        when (command.result.status) {
            IdentityResultStatus.INCOMING_EXCHANGE -> Step.ResolveDisplayName(command)
            IdentityResultStatus.ESTABLISHED ->
                if (command.result.direction == IdentityExchangeDirection.OUTGOING) {
                    Step.MarkInvitationAccepted(command)
                } else {
                    Step.ActivateConversation(command)
                }
            IdentityResultStatus.REMOTE_DECLINED -> Step.MarkInvitationDeclined(command)
            IdentityResultStatus.FAILED -> Step.MarkTransportFailed(command)
            IdentityResultStatus.EXCHANGE_INVALIDATED -> Step.InvalidateInvitation(command)
        }

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.ResolveDisplayName ->
                Outcome.DisplayNameResolved(getIdentityPeerDisplayName(result.peerId).getOrThrow())
            is Step.RecordIncomingInvitation -> {
                engine.run(
                    LifecycleCommand.RecordIncomingDirectInvitation(
                        result,
                        step.peerDisplayName,
                        declineIfRejected = true
                    )
                ).getOrThrow()
                Outcome.Done
            }
            is Step.MarkInvitationAccepted -> {
                handleInvitationResponse(
                    InvitationPayloadType.DIRECT,
                    result.exchangeId,
                    InvitationResponse.ACCEPTED
                ).getOrThrow()
                Outcome.InvitationAccepted
            }
            is Step.ActivateConversation -> {
                engine.run(MessageCommand.ActivateAuthorizedConversation(result.peerId)).getOrThrow()
                Outcome.ConversationActivated
            }
            is Step.LoadSetupMode ->
                Outcome.SetupModeLoaded(identitySetupModeRepository.getMode().getOrThrow())
            is Step.SendVerificationReceipt -> {
                sendIdentityVerificationReceipt(result.peerId)
                    .onFailure { error ->
                        logger.warn(error) { "Could not queue contact verification receipt for ${result.peerId}" }
                    }
                Outcome.Done
            }
            is Step.MarkInvitationDeclined -> {
                handleInvitationResponse(
                    InvitationPayloadType.DIRECT,
                    result.exchangeId,
                    InvitationResponse.DECLINED
                ).getOrThrow()
                Outcome.InvitationDeclined
            }
            is Step.DiscardWaitingMessages -> {
                engine.run(MessageCommand.DiscardWaitingDirectMessages(result.peerId)).getOrThrow()
                Outcome.Done
            }
            is Step.MarkTransportFailed -> {
                markInvitationTransportFailed(InvitationPayloadType.DIRECT, result.exchangeId).getOrThrow()
                Outcome.Done
            }
            is Step.InvalidateInvitation -> {
                invalidatePendingInvitation(result.exchangeId).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.DisplayNameResolved ->
                Transition.Next(Step.RecordIncomingInvitation(outcome.peerDisplayName, step.command))
            Outcome.InvitationAccepted -> Transition.Next(Step.ActivateConversation(step.command))
            Outcome.ConversationActivated -> Transition.Next(Step.LoadSetupMode(step.command))
            is Outcome.SetupModeLoaded ->
                if (outcome.mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                    Transition.Next(Step.SendVerificationReceipt(step.command))
                } else {
                    Transition.Complete()
                }
            Outcome.InvitationDeclined -> Transition.Next(Step.DiscardWaitingMessages(step.command))
            Outcome.Done -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.HandleIdentityResult

        data class ResolveDisplayName(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class RecordIncomingInvitation(
            val peerDisplayName: String?,
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class MarkInvitationAccepted(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class ActivateConversation(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class LoadSetupMode(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class SendVerificationReceipt(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class MarkInvitationDeclined(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class DiscardWaitingMessages(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class MarkTransportFailed(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step

        data class InvalidateInvitation(
            override val command: LifecycleCommand.HandleIdentityResult
        ) : Step
    }

    sealed interface Outcome {
        data class DisplayNameResolved(
            val peerDisplayName: String?
        ) : Outcome

        data object InvitationAccepted : Outcome

        data object ConversationActivated : Outcome

        data class SetupModeLoaded(
            val mode: DirectIdentitySetupMode
        ) : Outcome

        data object InvitationDeclined : Outcome

        /** The last step of the result's path finished. */
        data object Done : Outcome
    }
}
