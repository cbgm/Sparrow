package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupOperationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindEditableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * CheckMembership -> FindEditableMessage -> FindRecipients -> (EncryptEdit -> EnqueueEdit -> ReplaceText)
 * | EditUnsentMessage.
 *
 * A message nobody can receive or that is still queued unsent is edited locally only; the flush sends the new
 * text. Only that local edit ([MessageCommand.EditUnsentGroupMessage]) takes the group send lock, so a running
 * flush never sends the text from before the edit. The edit of a message members already have takes no lock and
 * never waits for a send or flush of the group.
 */
class EditGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val messageRepository: GroupMessageRepository,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val findEditableGroupMessage: FindEditableGroupMessageUseCase,
    private val findGroupRecipients: FindGroupRecipientsUseCase,
    private val encryptGroupOperation: EncryptGroupOperationUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.EditMessage,
        EditGroupMessageWorkflow.Step,
        EditGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.EditMessage> = GroupMessageCommand.EditMessage::class

    override fun firstStep(command: GroupMessageCommand.EditMessage): Step = Step.CheckMembership(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckMembership -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                require(step.command.text.trim().isNotBlank()) { "Edited message text must not be blank" }
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            }
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
            is Step.EditUnsentMessage -> {
                engine.run(
                    MessageCommand.EditUnsentGroupMessage(
                        groupId = step.command.groupId,
                        messageId = step.command.messageId,
                        text = step.command.text
                    )
                ).getOrThrow()
                Outcome.UnsentMessageEdited
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
            Outcome.MembershipActive -> Transition.Next(Step.FindEditableMessage(step.command))
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            Outcome.MessageEditable -> Transition.Next(Step.FindRecipients(step.command))
            is Outcome.RecipientsFound -> encryptOrEditLocally(outcome.recipients, step.command)
            is Outcome.EditEncrypted -> Transition.Next(Step.EnqueueEdit(outcome.packets, step.command))
            Outcome.EditEnqueued -> Transition.Next(Step.ReplaceText(step.command))
            Outcome.TextReplaced -> Transition.Complete()
            Outcome.UnsentMessageEdited -> Transition.Complete()
        }

    /** Without a recipient the edit only changes the local text, under the group send lock. */
    private fun encryptOrEditLocally(
        recipients: List<String>,
        command: GroupMessageCommand.EditMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Next(Step.EditUnsentMessage(command))
        } else {
            Transition.Next(Step.EncryptEdit(recipients, command))
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.EditMessage

        data class CheckMembership(
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class FindEditableMessage(
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class FindRecipients(
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class EncryptEdit(
            val recipients: List<String>,
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class EnqueueEdit(
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class ReplaceText(
            override val command: GroupMessageCommand.EditMessage
        ) : Step

        data class EditUnsentMessage(
            override val command: GroupMessageCommand.EditMessage
        ) : Step
    }

    sealed interface Outcome {
        data object MembershipActive : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data object MessageEditable : Outcome

        data class RecipientsFound(
            val recipients: List<String>
        ) : Outcome

        data class EditEncrypted(
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data object EditEnqueued : Outcome

        data object TextReplaced : Outcome

        data object UnsentMessageEdited : Outcome
    }
}
