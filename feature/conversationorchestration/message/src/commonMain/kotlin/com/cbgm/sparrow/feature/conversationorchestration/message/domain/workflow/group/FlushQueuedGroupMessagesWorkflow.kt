package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UndeliveredGroupRecipients
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessagesToFlushUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindUndeliveredGroupRecipientsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SaveQueuedGroupRecipientsUseCase
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import kotlinx.coroutines.CancellationException
import kotlin.reflect.KClass

/**
 * FindRecipients -> FindQueuedMessages
 * -> (FindMissingRecipients -> EncryptMessage -> [SaveRecipientStates] -> EnqueuePackets)* .
 *
 * The persisted queue survives process death and is replayed whenever membership activates a
 * recipient. A damaged or temporarily undeliverable message stays queued and never blocks the
 * following ones.
 */
class FlushQueuedGroupMessagesWorkflow internal constructor(
    private val messageRepository: GroupMessageRepository,
    private val findGroupMessagesToFlush: FindGroupMessagesToFlushUseCase,
    private val findUndeliveredGroupRecipients: FindUndeliveredGroupRecipientsUseCase,
    private val encryptGroupMessage: EncryptGroupMessageUseCase,
    private val saveQueuedGroupRecipients: SaveQueuedGroupRecipientsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        MessageCommand.FlushQueuedGroupMessages,
        FlushQueuedGroupMessagesWorkflow.Step,
        FlushQueuedGroupMessagesWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("FlushQueuedGroupMessagesWorkflow")

    override val commandType: KClass<MessageCommand.FlushQueuedGroupMessages> =
        MessageCommand.FlushQueuedGroupMessages::class

    override fun lockKey(command: MessageCommand.FlushQueuedGroupMessages): String = groupSendLockKey(command.groupId)

    override fun firstStep(command: MessageCommand.FlushQueuedGroupMessages): Step = Step.FindRecipients(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindRecipients ->
                Outcome.RecipientsFound(messageRepository.findParticipantIds(step.command.groupId).getOrThrow())
            is Step.FindQueuedMessages ->
                Outcome.QueuedMessagesFound(
                    FlushQueue(findGroupMessagesToFlush(step.command.groupId).getOrThrow(), step.recipients)
                )
            is Step.FindMissingRecipients ->
                attempt(step.queue) {
                    Outcome.MissingRecipientsFound(
                        queue = step.queue,
                        missing = findUndeliveredGroupRecipients(step.queue.current.id, step.queue.recipients)
                            .getOrThrow()
                    )
                }
            is Step.EncryptMessage ->
                attempt(step.queue) {
                    Outcome.MessageEncrypted(
                        queue = step.queue,
                        packets = encryptGroupMessage(step.queue.current, step.missing.contactIds).getOrThrow(),
                        isFirstDelivery = step.missing.isFirstDelivery
                    )
                }
            is Step.SaveRecipientStates ->
                attempt(step.queue) {
                    saveQueuedGroupRecipients.forQueuedMessage(step.queue.current, step.packets).getOrThrow()
                    Outcome.RecipientStatesSaved(step.queue, step.packets)
                }
            is Step.EnqueuePackets ->
                attempt(step.queue) {
                    enqueueGroupPackets(step.packets, recordFailedRecipients = true).getOrThrow()
                    Outcome.MessageFlushed(step.queue.next())
                }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.RecipientsFound -> startFlush(outcome.recipients, step.command)
            is Outcome.QueuedMessagesFound -> nextMessage(outcome.queue, step.command)
            is Outcome.MissingRecipientsFound -> encryptForMissing(outcome.queue, outcome.missing, step.command)
            is Outcome.MessageEncrypted ->
                saveStatesOrEnqueue(outcome.queue, outcome.packets, outcome.isFirstDelivery, step.command)
            is Outcome.RecipientStatesSaved ->
                Transition.Next(Step.EnqueuePackets(outcome.queue, outcome.packets, step.command))
            is Outcome.MessageFlushed -> nextMessage(outcome.queue, step.command)
            is Outcome.MessageStaysQueued -> nextMessage(outcome.queue, step.command)
        }

    private fun nextMessage(queue: FlushQueue, command: MessageCommand.FlushQueuedGroupMessages): Transition<Step> =
        if (queue.messages.isEmpty()) {
            Transition.Complete()
        } else {
            Transition.Next(
                Step.FindMissingRecipients(queue, command)
            )
        }

    /** Without an active recipient nothing can be flushed. */
    private fun startFlush(
        recipients: List<String>,
        command: MessageCommand.FlushQueuedGroupMessages
    ): Transition<Step> =
        if (recipients.isEmpty()) Transition.Complete() else Transition.Next(Step.FindQueuedMessages(recipients, command))

    /** A message that every recipient already has in the outbox needs nothing. */
    private fun encryptForMissing(
        queue: FlushQueue,
        missing: UndeliveredGroupRecipients,
        command: MessageCommand.FlushQueuedGroupMessages
    ): Transition<Step> =
        if (missing.contactIds.isEmpty()) {
            nextMessage(queue.next(), command)
        } else {
            Transition.Next(Step.EncryptMessage(queue, missing, command))
        }

    private fun saveStatesOrEnqueue(
        queue: FlushQueue,
        packets: Map<String, GroupChatMessagePacket>,
        isFirstDelivery: Boolean,
        command: MessageCommand.FlushQueuedGroupMessages
    ): Transition<Step> =
        if (isFirstDelivery) {
            Transition.Next(Step.SaveRecipientStates(queue, packets, command))
        } else {
            Transition.Next(Step.EnqueuePackets(queue, packets, command))
        }

    private suspend fun attempt(queue: FlushQueue, action: suspend () -> Outcome): Outcome =
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            logger.warn(error) { "Group message remains queued for retry: messageId=${queue.current.id}" }
            Outcome.MessageStaysQueued(queue.next())
        }

    /** The messages still to flush; [current] is the one being flushed. */
    data class FlushQueue(
        val messages: List<StoredMessage>,
        val recipients: List<String>
    ) {
        val current: StoredMessage
            get() = messages.first()

        fun next(): FlushQueue = copy(messages = messages.drop(1))
    }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.FlushQueuedGroupMessages

        data class FindRecipients(
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step

        data class FindQueuedMessages(
            val recipients: List<String>,
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step

        data class FindMissingRecipients(
            val queue: FlushQueue,
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step

        data class EncryptMessage(
            val queue: FlushQueue,
            val missing: UndeliveredGroupRecipients,
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step

        data class SaveRecipientStates(
            val queue: FlushQueue,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step

        data class EnqueuePackets(
            val queue: FlushQueue,
            val packets: Map<String, GroupChatMessagePacket>,
            override val command: MessageCommand.FlushQueuedGroupMessages
        ) : Step
    }

    sealed interface Outcome {
        data class RecipientsFound(
            val recipients: List<String>
        ) : Outcome

        data class QueuedMessagesFound(
            val queue: FlushQueue
        ) : Outcome

        data class MissingRecipientsFound(
            val queue: FlushQueue,
            val missing: UndeliveredGroupRecipients
        ) : Outcome

        data class MessageEncrypted(
            val queue: FlushQueue,
            val packets: Map<String, GroupChatMessagePacket>,
            val isFirstDelivery: Boolean
        ) : Outcome

        data class RecipientStatesSaved(
            val queue: FlushQueue,
            val packets: Map<String, GroupChatMessagePacket>
        ) : Outcome

        data class MessageFlushed(
            val queue: FlushQueue
        ) : Outcome

        data class MessageStaysQueued(
            val queue: FlushQueue
        ) : Outcome
    }
}
