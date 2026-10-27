package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactInvitationRoutingIdUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RecordOutgoingDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RequireContactInvitableUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartIdentityExchangeUseCase
import kotlin.reflect.KClass

/**
 * CheckNotBlocked -> LoadAuthorization -> LoadLabels -> StartExchange -> RecordInvitation
 *
 * An authorized contact without a chat (it was deleted locally while the peer kept its keys) first goes
 * through FindConversation -> PreflightRouting -> RevokeExchange. Only that old conversation authorization
 * is revoked; remote identity, trust records and the user's own keys are never reset. An authorized
 * contact with a chat needs no invitation.
 */
class StartDirectInvitationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireContactInvitable: RequireContactInvitableUseCase,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val conversationRepository: DirectConversationRepository,
    private val resolveContactInvitationRoutingId: ResolveContactInvitationRoutingIdUseCase,
    private val getIdentityPeerDisplayName: GetIdentityPeerDisplayNameUseCase,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val startIdentityExchange: StartIdentityExchangeUseCase,
    private val recordOutgoingDirectInvitation: RecordOutgoingDirectInvitationUseCase
) : WorkflowStateMachine<
        LifecycleCommand.StartDirectInvitation,
        StartDirectInvitationWorkflow.Step,
        StartDirectInvitationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.StartDirectInvitation> =
        LifecycleCommand.StartDirectInvitation::class

    override fun firstStep(command: LifecycleCommand.StartDirectInvitation): Step = Step.CheckNotBlocked(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckNotBlocked -> {
                require(step.command.contactId.isNotBlank()) { "Peer ID must not be blank" }
                requireContactInvitable(step.command.contactId).getOrThrow()
                Outcome.NotBlocked
            }
            is Step.LoadAuthorization ->
                authorizationLoaded(getIdentityPeerState(step.command.contactId).getOrThrow().hasEstablishedExchange)
            is Step.FindConversation ->
                conversationFound(conversationRepository.findConversationId(step.command.contactId).getOrThrow())
            is Step.PreflightRouting -> {
                // Do not revoke a still valid authorization when the contact cannot be reached anyway.
                resolveContactInvitationRoutingId(step.command.contactId).getOrThrow()
                Outcome.RoutingAvailable
            }
            is Step.RevokeExchange -> {
                engine.run(LifecycleCommand.RevokePeerExchange(step.command.contactId)).getOrThrow()
                Outcome.ExchangeRevoked
            }
            is Step.LoadLabels ->
                Outcome.LabelsLoaded(
                    peerDisplayName =
                        getIdentityPeerDisplayName(step.command.contactId).getOrThrow(),
                    senderLabel = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
                )
            is Step.StartExchange -> {
                val exchange =
                    startIdentityExchange(step.command.contactId, step.senderLabel).getOrThrow()
                        ?: error("No new invitation was created; an existing exchange must be resolved first")
                Outcome.ExchangeStarted(exchange, step.peerDisplayName)
            }
            is Step.RecordInvitation -> {
                recordOutgoingDirectInvitation(step.exchange, step.peerDisplayName).getOrThrow()
                Outcome.InvitationRecorded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NotBlocked -> Transition.Next(Step.LoadAuthorization(step.command))
            Outcome.Authorized -> Transition.Next(Step.FindConversation(step.command))
            Outcome.NotAuthorized -> Transition.Next(Step.LoadLabels(step.command))
            Outcome.ConversationExists -> Transition.Complete()
            Outcome.ConversationMissing -> Transition.Next(Step.PreflightRouting(step.command))
            Outcome.RoutingAvailable -> Transition.Next(Step.RevokeExchange(step.command))
            Outcome.ExchangeRevoked -> Transition.Next(Step.LoadLabels(step.command))
            is Outcome.LabelsLoaded ->
                Transition.Next(Step.StartExchange(outcome.peerDisplayName, outcome.senderLabel, step.command))
            is Outcome.ExchangeStarted ->
                Transition.Next(Step.RecordInvitation(outcome.exchange, outcome.peerDisplayName, step.command))
            Outcome.InvitationRecorded -> Transition.Complete()
        }

    private fun authorizationLoaded(hasEstablishedExchange: Boolean): Outcome =
        if (hasEstablishedExchange) Outcome.Authorized else Outcome.NotAuthorized

    private fun conversationFound(conversationId: String?): Outcome =
        if (conversationId != null) Outcome.ConversationExists else Outcome.ConversationMissing

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.StartDirectInvitation

        data class CheckNotBlocked(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class LoadAuthorization(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class FindConversation(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class PreflightRouting(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class RevokeExchange(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class LoadLabels(
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class StartExchange(
            val peerDisplayName: String?,
            val senderLabel: String,
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step

        data class RecordInvitation(
            val exchange: IdentityExchange,
            val peerDisplayName: String?,
            override val command: LifecycleCommand.StartDirectInvitation
        ) : Step
    }

    sealed interface Outcome {
        data object NotBlocked : Outcome

        data object Authorized : Outcome

        data object NotAuthorized : Outcome

        data object ConversationExists : Outcome

        data object ConversationMissing : Outcome

        data object RoutingAvailable : Outcome

        data object ExchangeRevoked : Outcome

        data class LabelsLoaded(
            val peerDisplayName: String?,
            val senderLabel: String
        ) : Outcome

        data class ExchangeStarted(
            val exchange: IdentityExchange,
            val peerDisplayName: String?
        ) : Outcome

        data object InvitationRecorded : Outcome
    }
}
