package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.group

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EncryptGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
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
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val protocolOutbox: ProtocolOutbox,
    private val encryptGroupMessage: EncryptGroupMessageUseCase,
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
            is Step.FindQueuedMessages -> findQueuedMessages(step)
            is Step.FindMissingRecipients -> attempt(step.queue) { findMissingRecipients(step.queue) }
            is Step.EncryptMessage -> attempt(step.queue) { encryptMessage(step) }
            is Step.SaveRecipientStates -> attempt(step.queue) { saveRecipientStates(step) }
            is Step.EnqueuePackets ->
                attempt(step.queue) {
                    enqueueGroupPackets(step.packets, recordFailedRecipients = true).getOrThrow()
                    Outcome.MessageFlushed(step.queue.next())
                }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.RecipientsFound ->
                if (outcome.recipients.isEmpty()) {
                    Transition.Complete()
                } else {
                    Transition.Next(Step.FindQueuedMessages(outcome.recipients, step.command))
                }
            is Outcome.QueuedMessagesFound -> nextMessage(outcome.queue, step.command)
            is Outcome.MissingRecipientsFound ->
                if (outcome.missingRecipients.isEmpty()) {
                    nextMessage(outcome.queue.next(), step.command)
                } else {
                    Transition.Next(
                        Step.EncryptMessage(
                            outcome.queue,
                            outcome.missingRecipients,
                            outcome.isFirstDelivery,
                            step.command
                        )
                    )
                }
            is Outcome.MessageEncrypted ->
                if (outcome.isFirstDelivery) {
                    Transition.Next(Step.SaveRecipientStates(outcome.queue, outcome.packets, step.command))
                } else {
                    Transition.Next(Step.EnqueuePackets(outcome.queue, outcome.packets, step.command))
                }
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

    private suspend fun findQueuedMessages(step: Step.FindQueuedMessages): Outcome {
        val waiting = messageRepository.findQueued(step.command.groupId).getOrThrow()
        val interrupted = messageRepository.findAwaitingOutbox(step.command.groupId).getOrThrow()
        val messages =
            (waiting + interrupted)
                .distinctBy { it.id }
                .sortedWith(compareBy<StoredMessage> { it.createdAtEpochMilliseconds }.thenBy { it.id })
        return Outcome.QueuedMessagesFound(FlushQueue(messages, step.recipients))
    }

    /** Reconciles ONLY the original recipients; never backfills someone who joined in a later epoch. */
    private suspend fun findMissingRecipients(queue: FlushQueue): Outcome {
        val previousStates = messageRepository.findRecipientStates(queue.current.id).getOrThrow()
        if (previousStates.isEmpty()) {
            return Outcome.MissingRecipientsFound(queue, queue.recipients, isFirstDelivery = true)
        }
        val missing =
            previousStates.filter { state ->
                val packetId = state.packetId
                state.contactId in queue.recipients &&
                    state.deliveryStatus == MessageDeliveryStatus.QUEUED &&
                    packetId != null &&
                    protocolOutbox.findByPacketId(packetId).getOrThrow() == null
            }.map { it.contactId }
        return Outcome.MissingRecipientsFound(queue, missing, isFirstDelivery = false)
    }

    private suspend fun encryptMessage(step: Step.EncryptMessage): Outcome {
        val message = step.queue.current
        val packets =
            encryptGroupMessage(
                message = message,
                text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                parts = attachmentRepository.prepareOutgoing(message.id).getOrThrow(),
                recipients = step.missingRecipients
            ).getOrThrow()
        return Outcome.MessageEncrypted(step.queue, packets, step.isFirstDelivery)
    }

    /** The recipient mapping is persisted before enqueueing; restart reconciliation covers a crash in between. */
    private suspend fun saveRecipientStates(step: Step.SaveRecipientStates): Outcome {
        val message = step.queue.current
        val now = SystemClock.nowEpochMilliseconds()
        messageRepository.saveOutgoing(
            message = message,
            text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
            recipientStates = step.packets.map { (contactId, packet) ->
                GroupRecipientState(
                    messageId = message.id,
                    contactId = contactId,
                    packetId = packet.packetId,
                    deliveryStatus = MessageDeliveryStatus.QUEUED,
                    lastError = null,
                    updatedAtEpochMilliseconds = now
                )
            }
        ).getOrThrow()
        return Outcome.RecipientStatesSaved(step.queue, step.packets)
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
            val missingRecipients: List<String>,
            val isFirstDelivery: Boolean,
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
            val missingRecipients: List<String>,
            val isFirstDelivery: Boolean
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
