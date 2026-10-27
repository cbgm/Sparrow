package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindConversation -> FindEditableMessage -> (EditWaitingMessage) | (CheckAuthorization -> SendEdit -> ReplaceText).
 *
 * A message still waiting for authorization was never sent, so it is edited locally only; that edit runs as
 * [MessageCommand.EditOwnDirectMessage] under the contact's release lock, which checks the message again, so a
 * release never sends the old text. A sent message cannot become waiting again, so its edit takes no lock and never
 * waits for a release or a send to the same contact.
 */
class EditDirectMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val findEditableDirectMessage: FindEditableDirectMessageUseCase,
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
            is Step.FindEditableMessage ->
                editableMessageFound(
                    findEditableDirectMessage(step.command.conversationId, step.command.messageId).getOrThrow(),
                    step.target
                )
            is Step.EditWaitingMessage -> {
                engine.run(
                    MessageCommand.EditOwnDirectMessage(step.target, step.command.messageId, step.command.text)
                ).getOrThrow()
                Outcome.WaitingMessageEdited
            }
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
                messageRepository.replaceText(
                    step.command.messageId,
                    step.command.text.trim(),
                    SystemClock.nowEpochMilliseconds()
                ).getOrThrow()
                Outcome.TextReplaced
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.FindEditableMessage(outcome.target, step.command))
            is Outcome.MessageWaitingForAuthorization ->
                Transition.Next(Step.EditWaitingMessage(outcome.target, step.command))
            Outcome.WaitingMessageEdited -> Transition.Complete()
            is Outcome.MessageSent -> Transition.Next(Step.CheckAuthorization(outcome.target, step.command))
            is Outcome.Authorized -> Transition.Next(Step.SendEdit(outcome.target, step.command))
            Outcome.EditSent -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.TextReplaced -> Transition.Complete()
        }

    /** A message still waiting for authorization was never sent, so it is edited locally only. */
    private fun editableMessageFound(message: StoredMessage, target: DirectConversationTarget): Outcome =
        if (message.wasNeverSent) Outcome.MessageWaitingForAuthorization(target) else Outcome.MessageSent(target)

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.EditMessage

        data class FindConversation(
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class FindEditableMessage(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.EditMessage
        ) : Step

        data class EditWaitingMessage(
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

        data class MessageWaitingForAuthorization(
            val target: DirectConversationTarget
        ) : Outcome

        data object WaitingMessageEdited : Outcome

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
