package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.ToggleLocalReactionUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/** FindConversation -> CheckAuthorization -> FindMessage -> ToggleReaction -> SendReaction. */
class ToggleDirectReactionWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val findDirectMessage: FindDirectMessageUseCase,
    private val toggleLocalReaction: ToggleLocalReactionUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendDirectOperation: SendDirectOperationUseCase
) : ConversationWorkflow<
        DirectConversationCommand.ToggleReaction,
        ToggleDirectReactionWorkflow.Step,
        ToggleDirectReactionWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.ToggleReaction> =
        DirectConversationCommand.ToggleReaction::class

    override fun firstStep(command: DirectConversationCommand.ToggleReaction): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                require(step.command.emoji.isNotBlank()) { "Reaction emoji must not be blank" }
                Outcome.ConversationFound(
                    conversationRepository.requireTarget(step.command.conversationId).getOrThrow()
                )
            }
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.target.contactId).getOrThrow()
                Outcome.Authorized(step.target)
            }
            is Step.FindMessage -> {
                findDirectMessage(step.command.conversationId, step.command.messageId).getOrThrow()
                Outcome.MessageFound(step.target)
            }
            is Step.ToggleReaction ->
                Outcome.ReactionChanged(
                    target = step.target,
                    removed = toggleLocalReaction(
                        messageId = step.command.messageId,
                        conversationId = step.command.conversationId,
                        emoji = step.command.emoji
                    ).getOrThrow()
                )
            is Step.SendReaction -> {
                sendDirectOperation(
                    contactId = step.target.contactId,
                    operation = MessageOperation.Reaction(
                        messageId = step.command.messageId,
                        emoji = step.command.emoji,
                        removed = step.removed
                    )
                ).getOrThrow()
                Outcome.ReactionSent
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.CheckAuthorization(outcome.target, step.command))
            is Outcome.Authorized -> Transition.Next(Step.FindMessage(outcome.target, step.command))
            is Outcome.MessageFound -> Transition.Next(Step.ToggleReaction(outcome.target, step.command))
            is Outcome.ReactionChanged ->
                Transition.Next(Step.SendReaction(outcome.target, outcome.removed, step.command))
            Outcome.ReactionSent -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.ToggleReaction

        data class FindConversation(
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step

        data class FindMessage(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step

        data class ToggleReaction(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step

        data class SendReaction(
            val target: DirectConversationTarget,
            val removed: Boolean,
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class Authorized(
            val target: DirectConversationTarget
        ) : Outcome

        data class MessageFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class ReactionChanged(
            val target: DirectConversationTarget,
            val removed: Boolean
        ) : Outcome

        data object ReactionSent : Outcome
    }
}
