package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectPendingAuthorizationMessagePolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindWaitingMessages -> CheckAuthorization -> DiscardExpiredMessages
 * -> (PrepareRelease -> EnqueuePacket [-> MarkSendFailed -> RestoreWaitingMessage])* .
 *
 * Release is opportunistic: onboarding, invitation acceptance and restart reconciliation may
 * all try before keys exist. Missing authorization defers the work instead of failing. A message
 * whose packet could not reach the outbox goes back to waiting so a later release retries it.
 */
class ReleaseWaitingDirectMessagesWorkflow internal constructor(
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val discardDirectMessages: DiscardDirectMessagesUseCase,
    private val packetFactory: DirectMessagePacketFactory,
    private val protocolOutbox: ProtocolOutbox,
    private val recordPacketSendFailure: RecordPacketSendFailureUseCase
) : WorkflowStateMachine<
        MessageCommand.ReleaseWaitingDirectMessages,
        ReleaseWaitingDirectMessagesWorkflow.Step,
        ReleaseWaitingDirectMessagesWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReleaseWaitingDirectMessagesWorkflow")

    override val commandType: KClass<MessageCommand.ReleaseWaitingDirectMessages> =
        MessageCommand.ReleaseWaitingDirectMessages::class

    override fun lockKey(command: MessageCommand.ReleaseWaitingDirectMessages): String =
        "direct-release:${command.contactId}"

    override fun firstStep(command: MessageCommand.ReleaseWaitingDirectMessages): Step =
        Step.FindWaitingMessages(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindWaitingMessages ->
                Outcome.WaitingMessagesFound(
                    messageRepository.findWaitingForAuthorization(step.command.contactId).getOrThrow()
                )
            is Step.CheckAuthorization ->
                if (findDirectAuthorizationBlock(step.command.contactId).getOrThrow() == null) {
                    Outcome.Authorized(step.messages)
                } else {
                    logger.debug {
                        "Deferred ${step.messages.size} direct messages until mutual identity/keys are ready"
                    }
                    Outcome.ReleaseDeferred
                }
            is Step.DiscardExpiredMessages -> discardExpiredMessages(step.messages)
            is Step.PrepareRelease -> prepareRelease(step.remaining.first(), step.remaining)
            is Step.EnqueuePacket ->
                protocolOutbox.enqueue(step.command.contactId, step.packet).fold(
                    onSuccess = { Outcome.PacketEnqueued(step.remaining.drop(1)) },
                    onFailure = { error -> Outcome.EnqueueFailed(step.packet, error, step.remaining) }
                )
            is Step.MarkSendFailed -> {
                // The message is restored next, so a failed status update must not stop the release.
                recordPacketSendFailure(step.packet.packetId, step.cause.message)
                    .onFailure { error -> logger.warn(error) { "Could not mark released packet as failed" } }
                Outcome.SendFailureRecorded(step.packet, step.cause, step.remaining)
            }
            is Step.RestoreWaitingMessage -> restoreWaitingMessage(step)
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.WaitingMessagesFound ->
                if (outcome.messages.isEmpty()) {
                    Transition.Complete()
                } else {
                    Transition.Next(Step.CheckAuthorization(outcome.messages, step.command))
                }
            is Outcome.Authorized -> Transition.Next(Step.DiscardExpiredMessages(outcome.messages, step.command))
            Outcome.ReleaseDeferred -> Transition.Complete()
            is Outcome.ExpiredMessagesDiscarded -> nextRelease(outcome.remaining, step.command)
            is Outcome.ReleasePrepared ->
                Transition.Next(Step.EnqueuePacket(outcome.packet, outcome.remaining, step.command))
            is Outcome.PacketEnqueued -> nextRelease(outcome.remaining, step.command)
            is Outcome.EnqueueFailed ->
                Transition.Next(Step.MarkSendFailed(outcome.packet, outcome.cause, outcome.remaining, step.command))
            is Outcome.SendFailureRecorded ->
                Transition.Next(
                    Step.RestoreWaitingMessage(outcome.packet, outcome.cause, outcome.remaining, step.command)
                )
            is Outcome.WaitingMessageRestored -> nextRelease(outcome.remaining, step.command)
        }

    private fun nextRelease(
        remaining: List<StoredMessage>,
        command: MessageCommand.ReleaseWaitingDirectMessages
    ): Transition<Step> =
        if (remaining.isEmpty()) Transition.Complete() else Transition.Next(Step.PrepareRelease(remaining, command))

    private suspend fun discardExpiredMessages(messages: List<StoredMessage>): Outcome {
        val now = SystemClock.nowEpochMilliseconds()
        val expired = messages.filter { message ->
            DirectPendingAuthorizationMessagePolicy.isExpired(
                createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                nowEpochMilliseconds = now
            )
        }
        discardDirectMessages(expired).getOrThrow()
        val expiredIds = expired.mapTo(mutableSetOf(), StoredMessage::id)
        return Outcome.ExpiredMessagesDiscarded(messages.filterNot { message -> message.id in expiredIds })
    }

    private suspend fun prepareRelease(message: StoredMessage, remaining: List<StoredMessage>): Outcome {
        val nextStatus =
            DirectMessageDeliveryStateMachine.transition(
                current = message.deliveryStatus,
                event = MessageDeliveryEvent.AUTHORIZATION_GRANTED
            )
        check(nextStatus == MessageDeliveryStatus.QUEUED) { "Direct message is not waiting for authorization" }

        val packet =
            packetFactory.create(
                messageId = message.id,
                text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                parts = attachmentRepository.messageParts(message.id).getOrThrow(),
                replyToMessageId = message.replyToMessageId
            ).getOrThrow()
        messageRepository.update(
            message.copy(
                packetId = packet.packetId,
                transportMode = TransportEncryptionMode.SEALED_BOX.name,
                deliveryStatus = nextStatus
            )
        ).getOrThrow()
        return Outcome.ReleasePrepared(packet, remaining)
    }

    /**
     * The message was marked QUEUED before the outbox insert. If the outbox did not persist the
     * packet, a FAILED message pointing at a nonexistent packet could never be retried, so the
     * waiting message is restored for a later release. If the outbox did persist it, its own
     * retry owns the message status and no second packet is created.
     */
    private suspend fun restoreWaitingMessage(step: Step.RestoreWaitingMessage): Outcome {
        val message = step.remaining.first()
        logger.warn(step.cause) {
            "Queued direct message could not be released after authorization: messageId=${message.id}"
        }
        protocolOutbox.findByPacketId(step.packet.packetId).onSuccess { persisted ->
            if (persisted != null) return@onSuccess
            val current = messageRepository.find(message.id).getOrThrow()
            if (current?.packetId == step.packet.packetId && current.deliveryStatus in RESTORABLE_STATUSES) {
                messageRepository.update(
                    current.copy(
                        packetId = null,
                        transportPayload = null,
                        deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION
                    )
                ).getOrThrow()
            }
        }
        return Outcome.WaitingMessageRestored(step.remaining.drop(1))
    }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.ReleaseWaitingDirectMessages

        data class FindWaitingMessages(
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        data class CheckAuthorization(
            val messages: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        data class DiscardExpiredMessages(
            val messages: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        /** Prepares the first of the [remaining] messages. */
        data class PrepareRelease(
            val remaining: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        data class EnqueuePacket(
            val packet: ChatMessagePacket,
            val remaining: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        data class MarkSendFailed(
            val packet: ChatMessagePacket,
            val cause: Throwable,
            val remaining: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step

        data class RestoreWaitingMessage(
            val packet: ChatMessagePacket,
            val cause: Throwable,
            val remaining: List<StoredMessage>,
            override val command: MessageCommand.ReleaseWaitingDirectMessages
        ) : Step
    }

    sealed interface Outcome {
        data class WaitingMessagesFound(
            val messages: List<StoredMessage>
        ) : Outcome

        data class Authorized(
            val messages: List<StoredMessage>
        ) : Outcome

        data object ReleaseDeferred : Outcome

        data class ExpiredMessagesDiscarded(
            val remaining: List<StoredMessage>
        ) : Outcome

        data class ReleasePrepared(
            val packet: ChatMessagePacket,
            val remaining: List<StoredMessage>
        ) : Outcome

        data class PacketEnqueued(
            val remaining: List<StoredMessage>
        ) : Outcome

        data class EnqueueFailed(
            val packet: ChatMessagePacket,
            val cause: Throwable,
            val remaining: List<StoredMessage>
        ) : Outcome

        data class SendFailureRecorded(
            val packet: ChatMessagePacket,
            val cause: Throwable,
            val remaining: List<StoredMessage>
        ) : Outcome

        data class WaitingMessageRestored(
            val remaining: List<StoredMessage>
        ) : Outcome
    }

    private companion object {
        val RESTORABLE_STATUSES = setOf(MessageDeliveryStatus.QUEUED, MessageDeliveryStatus.FAILED)
    }
}
