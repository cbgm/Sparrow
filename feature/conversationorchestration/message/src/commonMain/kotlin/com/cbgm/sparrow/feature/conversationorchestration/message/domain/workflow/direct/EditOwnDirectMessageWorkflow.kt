package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.directReleaseLockKey
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindEditableMessage -> (ReplaceText) | (CheckAuthorization -> SendEdit -> ReplaceText).
 *
 * Runs only for a message that was still waiting when the public edit looked at it. A message still waiting for
 * authorization was never sent, so it is edited locally only. The workflow holds the contact's release lock, so a
 * waiting message cannot be released with its old text between the check and the edit; one that was released
 * just before gets a normal edit.
 */
class EditOwnDirectMessageWorkflow internal constructor(
    private val messageRepository: DirectMessageRepository,
    private val findEditableDirectMessage: FindEditableDirectMessageUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendDirectOperation: SendDirectOperationUseCase
) : WorkflowStateMachine<
        MessageCommand.EditOwnDirectMessage,
        EditOwnDirectMessageWorkflow.Step,
        EditOwnDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.EditOwnDirectMessage> =
        MessageCommand.EditOwnDirectMessage::class

    override fun lockKey(command: MessageCommand.EditOwnDirectMessage): String =
        directReleaseLockKey(command.target.contactId)

    override fun firstStep(command: MessageCommand.EditOwnDirectMessage): Step = Step.FindEditableMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindEditableMessage ->
                editableMessageFound(
                    findEditableDirectMessage(step.command.target.conversationId, step.command.messageId).getOrThrow()
                )
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.command.target.contactId).getOrThrow()
                Outcome.Authorized
            }
            is Step.SendEdit -> {
                sendDirectOperation(
                    contactId = step.command.target.contactId,
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
            Outcome.MessageWaitingForAuthorization -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.MessageSent -> Transition.Next(Step.CheckAuthorization(step.command))
            Outcome.Authorized -> Transition.Next(Step.SendEdit(step.command))
            Outcome.EditSent -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.TextReplaced -> Transition.Complete()
        }

    /** A message still waiting for authorization was never sent, so it is edited locally only. */
    private fun editableMessageFound(message: StoredMessage): Outcome =
        if (message.wasNeverSent) Outcome.MessageWaitingForAuthorization else Outcome.MessageSent

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.EditOwnDirectMessage

        data class FindEditableMessage(
            override val command: MessageCommand.EditOwnDirectMessage
        ) : Step

        data class CheckAuthorization(
            override val command: MessageCommand.EditOwnDirectMessage
        ) : Step

        data class SendEdit(
            override val command: MessageCommand.EditOwnDirectMessage
        ) : Step

        data class ReplaceText(
            override val command: MessageCommand.EditOwnDirectMessage
        ) : Step
    }

    sealed interface Outcome {
        data object MessageWaitingForAuthorization : Outcome

        data object MessageSent : Outcome

        data object Authorized : Outcome

        data object EditSent : Outcome

        data object TextReplaced : Outcome
    }
}
