package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactInvitationRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.RequireDirectReconnectAllowedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import kotlin.reflect.KClass

/**
 * CheckReconnectable -> PreflightRouting -> LoadAuthorization -> [RevokeExchange] -> StartInvitation
 *
 * The user explicitly reconnects an EXISTING chat: a prior authorization is closed before a fresh
 * invitation starts. The contact, keys and conversation history are never deleted. A contact without
 * a phone number can still reconnect when the other installation restored the original signing identity;
 * routing is checked before anything is revoked.
 */
class ReconnectDirectConversationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireDirectReconnectAllowed: RequireDirectReconnectAllowedUseCase,
    private val resolveContactInvitationRoutingId: ResolveContactInvitationRoutingIdUseCase,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) : ConversationWorkflow<
        DirectConversationCommand.ReconnectConversation,
        ReconnectDirectConversationWorkflow.Step,
        ReconnectDirectConversationWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.ReconnectConversation> =
        DirectConversationCommand.ReconnectConversation::class

    override fun firstStep(command: DirectConversationCommand.ReconnectConversation): Step =
        Step.CheckReconnectable(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckReconnectable -> {
                val contactId = step.command.contactId
                require(contactId.isNotBlank()) { "Peer ID must not be blank" }
                requireDirectReconnectAllowed(contactId).getOrThrow()
                Outcome.Reconnectable
            }
            is Step.PreflightRouting -> {
                resolveContactInvitationRoutingId(step.command.contactId).getOrThrow()
                Outcome.RoutingAvailable
            }
            is Step.LoadAuthorization ->
                authorizationLoaded(getIdentityPeerState(step.command.contactId).getOrThrow().hasEstablishedExchange)
            is Step.RevokeExchange -> {
                engine.run(LifecycleCommand.RevokePeerExchange(step.command.contactId)).getOrThrow()
                Outcome.ExchangeRevoked
            }
            is Step.StartInvitation -> {
                engine.run(LifecycleCommand.StartDirectInvitation(step.command.contactId)).getOrThrow()
                Outcome.InvitationStarted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Reconnectable -> Transition.Next(Step.PreflightRouting(step.command))
            Outcome.RoutingAvailable -> Transition.Next(Step.LoadAuthorization(step.command))
            Outcome.Authorized -> Transition.Next(Step.RevokeExchange(step.command))
            Outcome.NotAuthorized -> Transition.Next(Step.StartInvitation(step.command))
            Outcome.ExchangeRevoked -> Transition.Next(Step.StartInvitation(step.command))
            Outcome.InvitationStarted -> Transition.Complete()
        }

    private fun authorizationLoaded(hasEstablishedExchange: Boolean): Outcome =
        if (hasEstablishedExchange) Outcome.Authorized else Outcome.NotAuthorized

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.ReconnectConversation

        data class CheckReconnectable(
            override val command: DirectConversationCommand.ReconnectConversation
        ) : Step

        data class PreflightRouting(
            override val command: DirectConversationCommand.ReconnectConversation
        ) : Step

        data class LoadAuthorization(
            override val command: DirectConversationCommand.ReconnectConversation
        ) : Step

        data class RevokeExchange(
            override val command: DirectConversationCommand.ReconnectConversation
        ) : Step

        data class StartInvitation(
            override val command: DirectConversationCommand.ReconnectConversation
        ) : Step
    }

    sealed interface Outcome {
        data object Reconnectable : Outcome

        data object RoutingAvailable : Outcome

        data object Authorized : Outcome

        data object NotAuthorized : Outcome

        data object ExchangeRevoked : Outcome

        data object InvitationStarted : Outcome
    }
}
