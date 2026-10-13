package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/** FindConversation -> CheckAuthorization -> FindMessage -> (AddReaction | RemoveReaction) -> SendReaction. */
class ToggleDirectReactionWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val reactionRepository: MessageReactionRepository,
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
                val message =
                    messageRepository.find(step.command.messageId).getOrThrow() ?: error("Message was not found")
                check(message.conversationId == step.command.conversationId) {
                    "Message does not belong to this conversation"
                }
                val reacted = reactionRepository.exists(step.command.messageId, null, step.command.emoji).getOrThrow()
                if (reacted) Outcome.ReactionPresent(step.target) else Outcome.ReactionAbsent(step.target)
            }
            is Step.RemoveReaction -> {
                reactionRepository.delete(step.command.messageId, null, step.command.emoji).getOrThrow()
                Outcome.ReactionChanged(step.target, removed = true)
            }
            is Step.AddReaction -> {
                reactionRepository.save(
                    step.command.messageId,
                    step.command.conversationId,
                    null,
                    step.command.emoji
                ).getOrThrow()
                Outcome.ReactionChanged(step.target, removed = false)
            }
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
            is Outcome.ReactionPresent -> Transition.Next(Step.RemoveReaction(outcome.target, step.command))
            is Outcome.ReactionAbsent -> Transition.Next(Step.AddReaction(outcome.target, step.command))
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

        data class RemoveReaction(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.ToggleReaction
        ) : Step

        data class AddReaction(
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

        data class ReactionPresent(
            val target: DirectConversationTarget
        ) : Outcome

        data class ReactionAbsent(
            val target: DirectConversationTarget
        ) : Outcome

        data class ReactionChanged(
            val target: DirectConversationTarget,
            val removed: Boolean
        ) : Outcome

        data object ReactionSent : Outcome
    }
}
