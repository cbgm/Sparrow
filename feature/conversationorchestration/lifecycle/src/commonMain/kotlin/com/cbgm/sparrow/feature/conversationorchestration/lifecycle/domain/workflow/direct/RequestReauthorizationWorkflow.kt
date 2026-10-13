package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import kotlin.reflect.KClass

/**
 * FindConversation -> LoadAuthorization -> Reconnect        (a retained chat with stale authorization)
 * FindConversation -> [LoadAuthorization] -> StartInvitation (no chat, or nothing to revoke)
 *
 * Runs only after an actual authorization failure; a merely offline contact never reaches it.
 * Reconnecting is required because the plain invitation skips contacts with an authorized chat.
 */
class RequestReauthorizationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: DirectConversationRepository,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) : WorkflowStateMachine<
        LifecycleCommand.RequestReauthorization,
        RequestReauthorizationWorkflow.Step,
        RequestReauthorizationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.RequestReauthorization> =
        LifecycleCommand.RequestReauthorization::class

    override fun firstStep(command: LifecycleCommand.RequestReauthorization): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation -> {
                require(step.command.contactId.isNotBlank()) { "Peer ID must not be blank" }
                if (conversationRepository.findConversationId(step.command.contactId).getOrThrow() != null) {
                    Outcome.ConversationExists
                } else {
                    Outcome.ConversationMissing
                }
            }
            is Step.LoadAuthorization ->
                if (getIdentityPeerState(step.command.contactId).getOrThrow().hasEstablishedExchange) {
                    Outcome.Authorized
                } else {
                    Outcome.NotAuthorized
                }
            is Step.Reconnect -> {
                engine.run(
                    DirectConversationCommand.ReconnectConversation(step.command.contactId)
                )
                    .getOrThrow()
                Outcome.InvitationStarted
            }
            is Step.StartInvitation -> {
                engine.run(LifecycleCommand.StartDirectInvitation(step.command.contactId)).getOrThrow()
                Outcome.InvitationStarted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.ConversationExists -> Transition.Next(Step.LoadAuthorization(step.command))
            Outcome.ConversationMissing -> Transition.Next(Step.StartInvitation(step.command))
            Outcome.Authorized -> Transition.Next(Step.Reconnect(step.command))
            Outcome.NotAuthorized -> Transition.Next(Step.StartInvitation(step.command))
            Outcome.InvitationStarted -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.RequestReauthorization

        data class FindConversation(
            override val command: LifecycleCommand.RequestReauthorization
        ) : Step

        data class LoadAuthorization(
            override val command: LifecycleCommand.RequestReauthorization
        ) : Step

        data class Reconnect(
            override val command: LifecycleCommand.RequestReauthorization
        ) : Step

        data class StartInvitation(
            override val command: LifecycleCommand.RequestReauthorization
        ) : Step
    }

    sealed interface Outcome {
        data object ConversationExists : Outcome

        data object ConversationMissing : Outcome

        data object Authorized : Outcome

        data object NotAuthorized : Outcome

        data object InvitationStarted : Outcome
    }
}
