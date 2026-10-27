package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendDirectOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.directReleaseLockKey
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlin.reflect.KClass

/**
 * FindOwnMessage -> (DiscardMessage) | (CheckAuthorization -> SendDelete -> DiscardMessage).
 *
 * Runs only for a message that was still waiting when the public delete looked at it. The workflow holds the
 * contact's release lock, so a waiting message cannot be released (and written back) between the check and the
 * local delete; one that was released just before gets a normal delete.
 */
class DeleteOwnDirectMessageWorkflow internal constructor(
    private val findDeletableDirectMessage: FindDeletableDirectMessageUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendDirectOperation: SendDirectOperationUseCase,
    private val discardDirectMessages: DiscardDirectMessagesUseCase
) : WorkflowStateMachine<
        MessageCommand.DeleteOwnDirectMessage,
        DeleteOwnDirectMessageWorkflow.Step,
        DeleteOwnDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.DeleteOwnDirectMessage> =
        MessageCommand.DeleteOwnDirectMessage::class

    override fun lockKey(command: MessageCommand.DeleteOwnDirectMessage): String =
        directReleaseLockKey(command.target.contactId)

    override fun firstStep(command: MessageCommand.DeleteOwnDirectMessage): Step = Step.FindOwnMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindOwnMessage ->
                ownMessageFound(
                    findDeletableDirectMessage(step.command.target.conversationId, step.command.messageId).getOrThrow()
                )
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.command.target.contactId).getOrThrow()
                Outcome.Authorized(step.message)
            }
            is Step.SendDelete -> {
                sendDirectOperation(
                    contactId = step.command.target.contactId,
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
            is Outcome.MessageNeverSent -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            is Outcome.MessageSent -> Transition.Next(Step.CheckAuthorization(outcome.message, step.command))
            is Outcome.Authorized -> Transition.Next(Step.SendDelete(outcome.message, step.command))
            is Outcome.DeleteSent -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            Outcome.MessageDiscarded -> Transition.Complete()
        }

    /** A message still waiting for authorization was never sent, so it is deleted locally only. */
    private fun ownMessageFound(message: StoredMessage): Outcome =
        if (message.wasNeverSent) Outcome.MessageNeverSent(message) else Outcome.MessageSent(message)

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.DeleteOwnDirectMessage

        data class FindOwnMessage(
            override val command: MessageCommand.DeleteOwnDirectMessage
        ) : Step

        data class CheckAuthorization(
            val message: StoredMessage,
            override val command: MessageCommand.DeleteOwnDirectMessage
        ) : Step

        data class SendDelete(
            val message: StoredMessage,
            override val command: MessageCommand.DeleteOwnDirectMessage
        ) : Step

        data class DiscardMessage(
            val message: StoredMessage,
            override val command: MessageCommand.DeleteOwnDirectMessage
        ) : Step
    }

    sealed interface Outcome {
        data class MessageNeverSent(
            val message: StoredMessage
        ) : Outcome

        data class MessageSent(
            val message: StoredMessage
        ) : Outcome

        data class Authorized(
            val message: StoredMessage
        ) : Outcome

        data class DeleteSent(
            val message: StoredMessage
        ) : Outcome

        data object MessageDiscarded : Outcome
    }
}
