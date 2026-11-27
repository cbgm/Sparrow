package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindEditableMessage -> FindRecipients -> [EncryptEdit -> EnqueueEdit] -> ReplaceText.
 *
 * Runs only for a message nobody had received when the public edit looked at it. It holds the group send lock and
 * checks the recipients again, so a running flush either finished (and the edit is sent like any other) or sends the
 * new text later; it never sends the text from before the edit.
 */
class EditUnsentGroupMessageWorkflow internal constructor(
    private val messageRepository: GroupMessageRepository,
    private val findEditableGroupMessage: FindEditableGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        MessageCommand.EditUnsentGroupMessage,
        EditUnsentGroupMessageWorkflow.Step,
        EditUnsentGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.EditUnsentGroupMessage> = MessageCommand.EditUnsentGroupMessage::class

    override fun lockKey(command: MessageCommand.EditUnsentGroupMessage): String = groupSendLockKey(command.groupId)

    override fun firstStep(command: MessageCommand.EditUnsentGroupMessage): Step = Step.FindEditableMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindEditableMessage -> {
                findEditableGroupMessage(step.command.groupId, step.command.messageId).getOrThrow()
                Outcome.MessageEditable
            }
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    findGroupRecipients.forOwnMessage(step.command.groupId, step.command.messageId).getOrThrow()
                )
            is Step.EncryptEdit ->
                Outcome.EditEncrypted(
                    encryptGroupOperation(
                        groupId = step.command.groupId,
                        operation = MessageOperation.Edit(
                            messageId = step.command.messageId,
                            text = step.command.text.trim(),
                            editedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        ),
                        recipients = step.recipients
                    ).getOrThrow()
                )
            is Step.EnqueueEdit -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.EditEnqueued
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
            Outcome.MessageEditable -> Transition.Next(Step.FindRecipients(step.command))
            is Outcome.RecipientsFound -> encryptOrEditLocally(outcome.recipients, step.command)
            is Outcome.EditEncrypted -> Transition.Next(Step.EnqueueEdit(outcome.packets, step.command))
            Outcome.EditEnqueued -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.TextReplaced -> Transition.Complete()
        }

    /** Without a recipient the edit only changes the local text. */
    private fun encryptOrEditLocally(
        recipients: List<String>,
        command: MessageCommand.EditUnsentGroupMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Next(Step.ReplaceText(command))
        } else {
            Transition.Next(Step.EncryptEdit(recipients, command))
        }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.EditUnsentGroupMessage

        data class FindEditableMessage(
            override val command: MessageCommand.EditUnsentGroupMessage
        ) : Step

        data class FindRecipients(
            override val command: MessageCommand.EditUnsentGroupMessage
        ) : Step

        data class EncryptEdit(
            val recipients: List<String>,
            override val command: MessageCommand.EditUnsentGroupMessage
        ) : Step

        data class EnqueueEdit(
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: MessageCommand.EditUnsentGroupMessage
        ) : Step

        data class ReplaceText(
            override val command: MessageCommand.EditUnsentGroupMessage
        ) : Step
    }

    sealed interface Outcome {
        data object MessageEditable : Outcome

        data class RecipientsFound(
            val recipients: List<String>
        ) : Outcome

        data class EditEncrypted(
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data object EditEnqueued : Outcome

        data object TextReplaced : Outcome
    }
}
