package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessageAccess
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredOutgoingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy.OutgoingMessageContentPolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CheckGroupMessageAccessUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SaveQueuedGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingGroupMessageUseCase
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlin.reflect.KClass

/**
 * ValidateContent -> CheckMembership -> FindRecipients -> [FlushQueuedMessages] -> StoreMessage
 * -> [EncryptMessage -> SaveRecipientStates -> EnqueuePackets].
 *
 * Without an active recipient the message stays queued and is flushed once a member activates.
 * Older queued messages are flushed first so recipients receive them in order.
 */
class SendGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val messageRepository: GroupMessageRepository,
    private val checkGroupMessageAccess: CheckGroupMessageAccessUseCase,
    private val storeOutgoingGroupMessage: StoreOutgoingGroupMessageUseCase,
    private val discardGroupMessages: DiscardGroupMessagesUseCase,
    private val encryptGroupMessage: EncryptGroupMessageUseCase,
    private val saveQueuedGroupRecipients: SaveQueuedGroupRecipientsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.SendMessage,
        SendGroupMessageWorkflow.Step,
        SendGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.SendMessage> = GroupMessageCommand.SendMessage::class

    override fun lockKey(command: GroupMessageCommand.SendMessage): String = groupSendLockKey(command.groupId)

    override fun firstStep(command: GroupMessageCommand.SendMessage): Step = Step.ValidateContent(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ValidateContent ->
                Outcome.ContentValidated(OutgoingMessageContentPolicy.requireGroupContent(step.command.parts))
            is Step.CheckMembership ->
                when (val access = checkGroupMessageAccess(step.command.groupId).getOrThrow()) {
                    GroupMessageAccess.Granted -> Outcome.MembershipActive(step.content)
                    is GroupMessageAccess.Denied -> Outcome.MembershipInactive(access.reason)
                }
            is Step.FindRecipients ->
                Outcome.RecipientsFound(
                    step.content,
                    messageRepository.findParticipantIds(step.command.groupId).getOrThrow()
                )
            is Step.FlushQueuedMessages -> {
                // This workflow already holds the group send lock that the flush workflow uses.
                engine.run(
                    MessageCommand.FlushQueuedGroupMessages(step.command.groupId),
                    acquireLock = false
                )
                    .getOrThrow()
                Outcome.QueueFlushed(step.content, step.recipients)
            }
            is Step.StoreMessage ->
                Outcome.MessageStored(
                    stored = storeOutgoingGroupMessage(
                        groupId = step.command.groupId,
                        content = step.content,
                        replyToMessageId = step.command.replyToMessageId
                    ).getOrThrow(),
                    recipients = step.recipients
                )
            is Step.EncryptMessage ->
                encryptGroupMessage(
                    message = step.stored.message,
                    text = step.stored.text,
                    parts = step.stored.parts,
                    recipients = step.recipients
                ).fold(
                    onSuccess = { packets -> Outcome.MessageEncrypted(step.stored, packets) },
                    onFailure = { error -> Outcome.EncryptionFailed(step.stored, error) }
                )
            is Step.DiscardMessage -> {
                discardGroupMessages(listOf(step.stored.message)).getOrThrow()
                Outcome.MessageDiscarded(step.cause)
            }
            is Step.SaveRecipientStates -> {
                saveQueuedGroupRecipients(step.stored.message.id, step.packets).getOrThrow()
                Outcome.RecipientStatesSaved(step.stored, step.packets)
            }
            is Step.EnqueuePackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = true).getOrThrow()
                Outcome.PacketsEnqueued(step.stored)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContentValidated -> Transition.Next(Step.CheckMembership(outcome.content, step.command))
            is Outcome.MembershipActive -> Transition.Next(Step.FindRecipients(outcome.content, step.command))
            is Outcome.MembershipInactive ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.NOT_ALLOWED, outcome.reason))
            is Outcome.RecipientsFound -> flushOrStore(outcome.content, outcome.recipients, step.command)
            is Outcome.QueueFlushed ->
                Transition.Next(Step.StoreMessage(outcome.content, outcome.recipients, step.command))
            is Outcome.MessageStored -> sendOrKeepQueued(outcome.stored, outcome.recipients, step.command)
            is Outcome.EncryptionFailed ->
                Transition.Next(Step.DiscardMessage(outcome.stored, outcome.cause, step.command))
            is Outcome.MessageDiscarded -> Transition.Reject.because(outcome.cause)
            is Outcome.MessageEncrypted ->
                Transition.Next(Step.SaveRecipientStates(outcome.stored, outcome.packets, step.command))
            is Outcome.RecipientStatesSaved ->
                Transition.Next(Step.EnqueuePackets(outcome.stored, outcome.packets, step.command))
            is Outcome.PacketsEnqueued ->
                Transition.Complete(ConversationResult.GroupMessageSent(outcome.stored.message.id))
        }

    /** Older queued messages go first, but only when someone can receive them. */
    private fun flushOrStore(
        content: OutgoingMessageContent,
        recipients: List<String>,
        command: GroupMessageCommand.SendMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Next(Step.StoreMessage(content, recipients, command))
        } else {
            Transition.Next(Step.FlushQueuedMessages(content, recipients, command))
        }

    /** Without an active recipient the stored message stays queued until a member activates. */
    private fun sendOrKeepQueued(
        stored: StoredOutgoingMessage,
        recipients: List<String>,
        command: GroupMessageCommand.SendMessage
    ): Transition<Step> =
        if (recipients.isEmpty()) {
            Transition.Complete(ConversationResult.GroupMessageSent(stored.message.id))
        } else {
            Transition.Next(Step.EncryptMessage(stored, recipients, command))
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.SendMessage

        data class ValidateContent(
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class CheckMembership(
            val content: OutgoingMessageContent,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class FindRecipients(
            val content: OutgoingMessageContent,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class FlushQueuedMessages(
            val content: OutgoingMessageContent,
            val recipients: List<String>,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class StoreMessage(
            val content: OutgoingMessageContent,
            val recipients: List<String>,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class EncryptMessage(
            val stored: StoredOutgoingMessage,
            val recipients: List<String>,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class DiscardMessage(
            val stored: StoredOutgoingMessage,
            val cause: Throwable,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class SaveRecipientStates(
            val stored: StoredOutgoingMessage,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.SendMessage
        ) : Step

        data class EnqueuePackets(
            val stored: StoredOutgoingMessage,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: GroupMessageCommand.SendMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ContentValidated(
            val content: OutgoingMessageContent
        ) : Outcome

        data class MembershipActive(
            val content: OutgoingMessageContent
        ) : Outcome

        data class MembershipInactive(
            val reason: String
        ) : Outcome

        data class RecipientsFound(
            val content: OutgoingMessageContent,
            val recipients: List<String>
        ) : Outcome

        data class QueueFlushed(
            val content: OutgoingMessageContent,
            val recipients: List<String>
        ) : Outcome

        data class MessageStored(
            val stored: StoredOutgoingMessage,
            val recipients: List<String>
        ) : Outcome

        data class EncryptionFailed(
            val stored: StoredOutgoingMessage,
            val cause: Throwable
        ) : Outcome

        data class MessageDiscarded(
            val cause: Throwable
        ) : Outcome

        data class MessageEncrypted(
            val stored: StoredOutgoingMessage,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class RecipientStatesSaved(
            val stored: StoredOutgoingMessage,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class PacketsEnqueued(
            val stored: StoredOutgoingMessage
        ) : Outcome
    }
}
