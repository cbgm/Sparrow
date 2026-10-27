package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDeletableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindOwnMessage -> FindRecipients -> [EncryptDelete -> EnqueueDelete] -> DiscardMessage.
 *
 * Runs only for a message nobody had received when the public delete looked at it. It holds the group send lock and
 * checks the recipients again, so a running flush either finished (and the delete is sent like any other) or never
 * sends the message.
 */
class DeleteUnsentGroupMessageWorkflow internal constructor(
    private val findDeletableGroupMessage: FindDeletableGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase
) : WorkflowStateMachine<
        MessageCommand.DeleteUnsentGroupMessage,
        DeleteUnsentGroupMessageWorkflow.Step,
        DeleteUnsentGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.DeleteUnsentGroupMessage> = MessageCommand.DeleteUnsentGroupMessage::class

    override fun lockKey(command: MessageCommand.DeleteUnsentGroupMessage): String = groupSendLockKey(command.groupId)

    override fun firstStep(command: MessageCommand.DeleteUnsentGroupMessage): Step = Step.FindOwnMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindOwnMessage ->
                Outcome.OwnMessageFound(
                    findDeletableGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                )
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    step.message,
                    findGroupRecipients.forOwnMessage(step.command.groupId, step.message.id).getOrThrow()
                )
            is Step.EncryptDelete ->
                Outcome.DeleteEncrypted(
                    message = step.message,
                    packets = encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.Delete(
                            messageId = step.message.id,
                            deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueDelete -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.DeleteEnqueued(step.message)
            }
            is Step.DiscardMessage -> {
                discardGroupMessages(listOf(step.message)).getOrThrow()
                Outcome.MessageDiscarded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.OwnMessageFound -> Transition.Next(Step.FindRecipients(outcome.message, step.command))
            is Outcome.RecipientsFound -> encryptOrDeleteLocally(outcome.message, outcome.recipients, step.command)
            is Outcome.DeleteEncrypted ->
                Transition.Next(Step.EnqueueDelete(outcome.message, outcome.packets, step.command))
            is Outcome.DeleteEnqueued -> Transition.Next(Step.DiscardMessage(outcome.message, step.command))
            Outcome.MessageDiscarded -> Transition.Complete()
        }

    /** Without a recipient the message is only removed locally. */
    private fun encryptOrDeleteLocally(
        message: StoredMessage,
        recipients: List<String>,
        command: MessageCommand.DeleteUnsentGroupMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Next(Step.DiscardMessage(message, command))
        } else {
            Transition.Next(Step.EncryptDelete(message, recipients, command))
        }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.DeleteUnsentGroupMessage

        data class FindOwnMessage(
            override val command: MessageCommand.DeleteUnsentGroupMessage
        ) : Step

        data class FindRecipients(
            val message: StoredMessage,
            override val command: MessageCommand.DeleteUnsentGroupMessage
        ) : Step

        data class EncryptDelete(
            val message: StoredMessage,
            val recipients: List<String>,
            override val command: MessageCommand.DeleteUnsentGroupMessage
        ) : Step

        data class EnqueueDelete(
            val message: StoredMessage,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: MessageCommand.DeleteUnsentGroupMessage
        ) : Step

        data class DiscardMessage(
            val message: StoredMessage,
            override val command: MessageCommand.DeleteUnsentGroupMessage
        ) : Step
    }

    sealed interface Outcome {
        data class OwnMessageFound(
            val message: StoredMessage
        ) : Outcome

        data class RecipientsFound(
            val message: StoredMessage,
            val recipients: List<String>
        ) : Outcome

        data class DeleteEncrypted(
            val message: StoredMessage,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class DeleteEnqueued(
            val message: StoredMessage
        ) : Outcome

        data object MessageDiscarded : Outcome
    }
}
