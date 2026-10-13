package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import kotlin.reflect.KClass

/**
 * FindConversation -> CheckAuthorization                 (opens the authorized chat)
 * FindConversation -> [CheckAuthorization] -> StartInvitation
 *
 * Authorization alone is no evidence that a locally deleted chat still exists: reopening a missing chat
 * goes through an invitation, otherwise the other device may still wait for authorization.
 */
class OpenDirectConversationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: DirectConversationRepository,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase
) : ConversationWorkflow<
        DirectConversationCommand.OpenConversation,
        OpenDirectConversationWorkflow.Step,
        OpenDirectConversationWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.OpenConversation> =
        DirectConversationCommand.OpenConversation::class

    override fun firstStep(command: DirectConversationCommand.OpenConversation): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation -> {
                require(step.command.contactId.isNotBlank()) { "Peer ID must not be blank" }
                conversationRepository.findConversationId(step.command.contactId).getOrThrow()
                    ?.let(Outcome::ConversationFound)
                    ?: Outcome.ConversationMissing
            }
            is Step.CheckAuthorization ->
                if (requireDirectChatAuthorization(step.command.contactId).isSuccess) {
                    Outcome.Authorized(step.conversationId)
                } else {
                    Outcome.NotAuthorized
                }
            is Step.StartInvitation -> {
                engine.run(LifecycleCommand.StartDirectInvitation(step.command.contactId)).getOrThrow()
                Outcome.InvitationStarted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound ->
                Transition.Next(Step.CheckAuthorization(outcome.conversationId, step.command))
            Outcome.ConversationMissing -> Transition.Next(Step.StartInvitation(step.command))
            is Outcome.Authorized -> Transition.Complete(ConversationResult.ConversationOpened(outcome.conversationId))
            Outcome.NotAuthorized -> Transition.Next(Step.StartInvitation(step.command))
            Outcome.InvitationStarted ->
                Transition.Complete(ConversationResult.ConversationOpened(conversationId = null))
        }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.OpenConversation

        data class FindConversation(
            override val command: DirectConversationCommand.OpenConversation
        ) : Step

        data class CheckAuthorization(
            val conversationId: String,
            override val command: DirectConversationCommand.OpenConversation
        ) : Step

        data class StartInvitation(
            override val command: DirectConversationCommand.OpenConversation
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationFound(
            val conversationId: String
        ) : Outcome

        data object ConversationMissing : Outcome

        data class Authorized(
            val conversationId: String
        ) : Outcome

        data object NotAuthorized : Outcome

        data object InvitationStarted : Outcome
    }
}
