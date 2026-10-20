package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindConversation -> FindEditableMessage -> (ReplaceText) | (CheckAuthorization -> SendEdit -> ReplaceText).
 *
 * A message still waiting for authorization was never sent, so it is edited locally only.
 */
class EditDirectMessageWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendDirectOperation: SendDirectOperationUseCase
) : ConversationWorkflow<
        DirectConversationCommand.EditMessage,
        EditDirectMessageWorkflow.Step,
        EditDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.EditMessage> =
        DirectConversationCommand.EditMessage::class

    override fun firstStep(command: DirectConversationCommand.EditMessage): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                require(step.command.text.trim().isNotBlank()) { "Edited message text must not be blank" }
                Outcome.ConversationFound(
                    conversationRepository.requireTarget(step.command.conversationId).getOrThrow()
                )
            }
            is Step.FindEditableMessage -> findEditableMessage(step)
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.target.contactId).getOrThrow()
                Outcome.Authorized(step.target)
            }
            is Step.SendEdit -> {
                sendDirectOperation(
                    contactId = step.target.contactId,
                    operation = MessageOperation.Edit(
                        messageId = step.command.messageId,
                        text = step.command.text.trim(),
                        editedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                ).getOrThrow()
                Outcome.EditSent
            }
            is Step.ReplaceText -> {
                messageRepository.replaceText(step.command.messageId, step.command.text.trim()).getOrThrow()
                Outcome.TextReplaced
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.FindEditableMessage(outcome.target, step.command))
            Outcome.MessageWaitingForAuthorization -> Transition.Next(Step.ReplaceText(step.command))
            is Outcome.MessageSent -> Transition.Next(Step.CheckAuthorization(outcome.target, step.command))
            is Outcome.Authorized -> Transition.Next(Step.SendEdit(outcome.target, step.command))
            Outcome.EditSent -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.TextReplaced -> Transition.Complete()
        }

    private suspend fun findEditableMessage(step: Step.FindEditableMessage): Outcome {
        val messageId = step.command.messageId
        val message = messageRepository.find(messageId).getOrThrow() ?: error("Message was not found")
        check(message.conversationId == step.command.conversationId) { "Message does not belong to this conversation" }
        check(message.isMine) { "Only your own messages can be edited" }
        check(message.deliveryStatus != MessageDeliveryStatus.READ) { "Read messages cannot be edited" }
        check(
            !messageRepository.findText(messageId).getOrThrow().isNullOrBlank()
        ) { "Only text messages can be edited" }
        check(attachmentRepository.messageParts(messageId).getOrThrow().isEmpty()) {
            "Messages with attachments cannot be edited"
        }
        return if (message.isWaitingForAuthorization()) {
            Outcome.MessageWaitingForAuthorization
        } else {
            Outcome.MessageSent(step.target)
        }
    }

    private fun StoredMessage.isWaitingForAuthorization(): Boolean =
        deliveryStatus == MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.EditMessage

        data class FindConversation(
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class FindEditableMessage(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class SendEdit(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class ReplaceText(
            override val command: DirectConversationCommand.EditMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data object MessageWaitingForAuthorization : Outcome

        data class MessageSent(
            val target: DirectConversationTarget
        ) : Outcome

        data class Authorized(
            val target: DirectConversationTarget
        ) : Outcome

        data object EditSent : Outcome

        data object TextReplaced : Outcome
    }
}
