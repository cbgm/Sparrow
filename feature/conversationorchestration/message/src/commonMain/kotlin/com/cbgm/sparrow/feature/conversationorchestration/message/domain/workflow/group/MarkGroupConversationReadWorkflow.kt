package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendReadReceiptUseCase
import kotlin.reflect.KClass

/**
 * ClearUnseenCount -> FindUnreadMessages -> (AcknowledgeMessage)* .
 *
 * Each sender gets its own receipt; one failing receipt does not hold back the others.
 */
class MarkGroupConversationReadWorkflow internal constructor(
    private val unseenCountRepository: ConversationUnseenCountRepository,
    private val messageRepository: GroupMessageRepository,
    private val sendReadReceipt: SendReadReceiptUseCase
) : ConversationWorkflow<
        GroupMessageCommand.MarkConversationRead,
        MarkGroupConversationReadWorkflow.Step,
        MarkGroupConversationReadWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("MarkGroupConversationReadWorkflow")

    override val commandType: KClass<GroupMessageCommand.MarkConversationRead> =
        GroupMessageCommand.MarkConversationRead::class

    override fun firstStep(command: GroupMessageCommand.MarkConversationRead): Step = Step.ClearUnseenCount(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ClearUnseenCount -> {
                unseenCountRepository.clear(step.command.groupId).getOrThrow()
                Outcome.UnseenCountCleared
            }
            is Step.FindUnreadMessages -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                val conversation = messageRepository.findConversation(step.command.groupId).getOrThrow()
                    ?: error("Group conversation was not found")
                check(conversation.isGroup) { "Conversation is not a group" }
                Outcome.UnreadMessagesFound(
                    messageRepository.findAwaitingReadReceipt(step.command.groupId).getOrThrow()
                )
            }
            is Step.AcknowledgeMessage -> acknowledge(step)
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.UnseenCountCleared -> Transition.Next(Step.FindUnreadMessages(step.command))
            is Outcome.UnreadMessagesFound -> nextMessage(outcome.messages, emptyList(), step.command)
            is Outcome.MessageAcknowledged -> nextMessage(outcome.remaining, outcome.failures, step.command)
        }

    private fun nextMessage(
        remaining: List<UnreadIncomingMessage>,
        failures: List<String>,
        command: GroupMessageCommand.MarkConversationRead
    ): Transition<Step> =
        when {
            remaining.isNotEmpty() -> Transition.Next(Step.AcknowledgeMessage(remaining, failures, command))
            failures.isEmpty() -> Transition.Complete()
            else ->
                Transition.Reject(
                    ConversationFailure(
                        reason = ConversationFailureReason.UNEXPECTED,
                        message = "Group read receipt failed for ${failures.joinToString()}"
                    )
                )
        }

    private suspend fun acknowledge(step: Step.AcknowledgeMessage): Outcome {
        val message = step.remaining.first()
        val failure =
            sendReadReceipt(message.messageId, message.contactId).exceptionOrNull()?.describe(message)
                ?: messageRepository.markReadReceiptSent(message.messageId).fold(
                    onSuccess = { marked ->
                        if (marked) {
                            logger.debug {
                                "Group read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                            }
                            null
                        } else {
                            "${message.contactId}: incoming group message could not be marked as read"
                        }
                    },
                    onFailure = { error -> error.describe(message) }
                )
        return Outcome.MessageAcknowledged(step.remaining.drop(1), step.failures + listOfNotNull(failure))
    }

    private fun Throwable.describe(message: UnreadIncomingMessage): String =
        "${message.contactId}: ${this.message ?: this::class.simpleName.orEmpty()}"

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.MarkConversationRead

        data class ClearUnseenCount(
            override val command: GroupMessageCommand.MarkConversationRead
        ) : Step

        data class FindUnreadMessages(
            override val command: GroupMessageCommand.MarkConversationRead
        ) : Step

        /** Sends and records the receipt for the first of the [remaining] messages. */
        data class AcknowledgeMessage(
            val remaining: List<UnreadIncomingMessage>,
            val failures: List<String>,
            override val command: GroupMessageCommand.MarkConversationRead
        ) : Step
    }

    sealed interface Outcome {
        data object UnseenCountCleared : Outcome

        data class UnreadMessagesFound(
            val messages: List<UnreadIncomingMessage>
        ) : Outcome

        data class MessageAcknowledged(
            val remaining: List<UnreadIncomingMessage>,
            val failures: List<String>
        ) : Outcome
    }
}
