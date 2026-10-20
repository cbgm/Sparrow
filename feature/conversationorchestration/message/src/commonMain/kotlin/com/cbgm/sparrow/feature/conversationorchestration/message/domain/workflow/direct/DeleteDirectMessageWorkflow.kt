package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindConversation -> FindOwnMessage -> (DiscardMessage) | (CheckAuthorization -> SendDelete -> DiscardMessage).
 */
class DeleteDirectMessageWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendDirectOperation: SendDirectOperationUseCase,
    private val discardDirectMessages: DiscardDirectMessagesUseCase
) : ConversationWorkflow<
        DirectConversationCommand.DeleteMessage,
        DeleteDirectMessageWorkflow.Step,
        DeleteDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.DeleteMessage> =
        DirectConversationCommand.DeleteMessage::class

    override fun firstStep(command: DirectConversationCommand.DeleteMessage): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                Outcome.ConversationFound(
                    conversationRepository.requireTarget(step.command.conversationId).getOrThrow()
                )
            }
            is Step.FindOwnMessage -> {
                val message =
                    messageRepository.find(step.command.messageId).getOrThrow() ?: error("Message was not found")
                check(message.conversationId == step.command.conversationId) {
                    "Message does not belong to this conversation"
                }
                check(message.isMine) { "Only your own messages can be deleted for everyone" }
                if (message.deliveryStatus == MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION) {
                    Outcome.MessageNeverSent(message)
                } else {
                    Outcome.MessageSent(message, step.target)
                }
            }
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.target.contactId).getOrThrow()
                Outcome.Authorized(step.message, step.target)
            }
            is Step.SendDelete -> {
                sendDirectOperation(
                    contactId = step.target.contactId,
                    operation = MessageOperation.Delete(
                        messageId = step.message.id,
                        deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                ).getOrThrow()
                Outcome.DeleteSent(step.message)
            }
            is Step.DiscardMessage -> {
                discardDirectMessages(listOf(step.message)).getOrThrow()
                Outcome.MessageDiscarded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.FindOwnMessage(outcome.target, step.command))
            is Outcome.MessageNeverSent -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            is Outcome.MessageSent ->
                Transition.Next(Step.CheckAuthorization(outcome.message, outcome.target, step.command))
            is Outcome.Authorized -> Transition.Next(Step.SendDelete(outcome.message, outcome.target, step.command))
            is Outcome.DeleteSent -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            Outcome.MessageDiscarded -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.DeleteMessage

        data class FindConversation(
            override val command: DirectConversationCommand.DeleteMessage
        ) : Step

        data class FindOwnMessage(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.DeleteMessage
        ) : Step

        data class CheckAuthorization(
            val message: StoredMessage,
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.DeleteMessage
        ) : Step

        data class SendDelete(
            val message: StoredMessage,
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.DeleteMessage
        ) : Step

        data class DiscardMessage(
            val message: StoredMessage,
            override val command: DirectConversationCommand.DeleteMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class MessageNeverSent(
            val message: StoredMessage
        ) : Outcome

        data class MessageSent(
            val message: StoredMessage,
            val target: DirectConversationTarget
        ) : Outcome

        data class Authorized(
            val message: StoredMessage,
            val target: DirectConversationTarget
        ) : Outcome

        data class DeleteSent(
            val message: StoredMessage
        ) : Outcome

        data object MessageDiscarded : Outcome
    }
}
