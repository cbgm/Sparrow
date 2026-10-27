package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindRetryableGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordRetryRequestedUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import kotlin.reflect.KClass

/**
 * FindFailedRecipients -> (ResendToRecipient)* .
 *
 * Retrying a group message resends only to the recipients whose delivery failed; it never
 * creates a new logical message. One failing recipient does not prevent the others' retry.
 */
class RetryGroupMessageWorkflow internal constructor(
    private val findRetryableGroupRecipients: FindRetryableGroupRecipientsUseCase,
    private val protocolOutbox: ProtocolOutbox,
    private val recordRetryRequested: RecordRetryRequestedUseCase
) : ConversationWorkflow<
        GroupMessageCommand.RetryMessage,
        RetryGroupMessageWorkflow.Step,
        RetryGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.RetryMessage> = GroupMessageCommand.RetryMessage::class

    override fun firstStep(command: GroupMessageCommand.RetryMessage): Step = Step.FindFailedRecipients(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindFailedRecipients -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                Outcome.FailedRecipientsFound(findRetryableGroupRecipients(step.command.messageId).getOrThrow())
            }
            is Step.ResendToRecipient -> {
                val recipient = step.remaining.first()
                val failure = runCatching { resend(recipient) }.exceptionOrNull()
                    ?.let { error -> "${recipient.contactId}: ${error.message ?: error::class.simpleName.orEmpty()}" }
                Outcome.RecipientAttempted(step.remaining.drop(1), step.failures + listOfNotNull(failure))
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.FailedRecipientsFound ->
                Transition.Next(Step.ResendToRecipient(outcome.recipients, emptyList(), step.command))
            is Outcome.RecipientAttempted ->
                when {
                    outcome.remaining.isNotEmpty() ->
                        Transition.Next(Step.ResendToRecipient(outcome.remaining, outcome.failures, step.command))
                    outcome.failures.isEmpty() -> Transition.Complete()
                    else ->
                        Transition.Reject(
                            ConversationFailure(
                                reason = ConversationFailureReason.UNEXPECTED,
                                message = "Group message retry failed for ${outcome.failures.joinToString()}"
                            )
                        )
                }
        }

    private suspend fun resend(recipient: GroupRecipientState) {
        val packetId = findRetryableGroupRecipients.requireRetryablePacketId(recipient).getOrThrow()
        protocolOutbox.resend(packetId).getOrThrow()
        recordRetryRequested.forGroupRecipient(recipient.messageId, recipient.contactId).getOrThrow()
    }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.RetryMessage

        data class FindFailedRecipients(
            override val command: GroupMessageCommand.RetryMessage
        ) : Step

        /** Resends to the first of the [remaining] recipients. */
        data class ResendToRecipient(
            val remaining: List<GroupRecipientState>,
            val failures: List<String>,
            override val command: GroupMessageCommand.RetryMessage
        ) : Step
    }

    sealed interface Outcome {
        data class FailedRecipientsFound(
            val recipients: List<GroupRecipientState>
        ) : Outcome

        data class RecipientAttempted(
            val remaining: List<GroupRecipientState>,
            val failures: List<String>
        ) : Outcome
    }
}
