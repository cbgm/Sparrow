package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardExpiredWaitingDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.ReleaseWaitingDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RestoreWaitingDirectMessageUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindWaitingMessages -> CheckAuthorization -> DiscardExpiredMessages
 * -> (PrepareRelease [-> EnqueuePacket [-> MarkSendFailed -> RestoreWaitingMessage]])* .
 *
 * Release is opportunistic: onboarding, invitation acceptance and restart reconciliation may
 * all try before keys exist. Missing authorization defers the work instead of failing. A message
 * whose packet could not reach the outbox goes back to waiting so a later release retries it.
 *
 * Storing a new waiting message and editing or deleting one share the lock, so none of them works on a contact's
 * waiting messages while they are released. PrepareRelease still reads each message again and skips one that is
 * gone or no longer waiting.
 */
class ReleaseWaitingDirectMessagesWorkflow internal constructor(
    private val messageRepository: DirectMessageRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val discardExpiredWaitingDirectMessages: DiscardExpiredWaitingDirectMessagesUseCase,
    private val releaseWaitingDirectMessage: ReleaseWaitingDirectMessageUseCase,
    private val restoreWaitingDirectMessage: RestoreWaitingDirectMessageUseCase,
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
        directReleaseLockKey(command.contactId)

    override fun firstStep(command: MessageCommand.ReleaseWaitingDirectMessages): Step =
        Step.FindWaitingMessages(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindWaitingMessages ->
                Outcome.WaitingMessagesFound(
                    messageRepository.findWaitingForAuthorization(step.command.contactId).getOrThrow()
                )
            is Step.CheckAuthorization -> checkAuthorization(step)
            is Step.DiscardExpiredMessages ->
                Outcome.ExpiredMessagesDiscarded(discardExpiredWaitingDirectMessages(step.messages).getOrThrow())
            is Step.PrepareRelease ->
                releasePrepared(releaseWaitingDirectMessage(step.remaining.first().id).getOrThrow(), step.remaining)
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
            is Outcome.WaitingMessagesFound -> checkFound(outcome.messages, step.command)
            is Outcome.Authorized -> Transition.Next(Step.DiscardExpiredMessages(outcome.messages, step.command))
            Outcome.ReleaseDeferred -> Transition.Complete()
            is Outcome.ExpiredMessagesDiscarded -> nextRelease(outcome.remaining, step.command)
            is Outcome.ReleasePrepared ->
                Transition.Next(Step.EnqueuePacket(outcome.packet, outcome.remaining, step.command))
            is Outcome.ReleaseSkipped -> nextRelease(outcome.remaining, step.command)
            is Outcome.PacketEnqueued -> nextRelease(outcome.remaining, step.command)
            is Outcome.EnqueueFailed ->
                Transition.Next(Step.MarkSendFailed(outcome.packet, outcome.cause, outcome.remaining, step.command))
            is Outcome.SendFailureRecorded ->
                Transition.Next(
                    Step.RestoreWaitingMessage(outcome.packet, outcome.cause, outcome.remaining, step.command)
                )
            is Outcome.WaitingMessageRestored -> nextRelease(outcome.remaining, step.command)
        }

    private fun checkFound(
        messages: List<StoredMessage>,
        command: MessageCommand.ReleaseWaitingDirectMessages
    ): Transition<Step> =
        if (messages.isEmpty()) Transition.Complete() else Transition.Next(Step.CheckAuthorization(messages, command))

    private fun nextRelease(
        remaining: List<StoredMessage>,
        command: MessageCommand.ReleaseWaitingDirectMessages
    ): Transition<Step> =
        if (remaining.isEmpty()) Transition.Complete() else Transition.Next(Step.PrepareRelease(remaining, command))

    /** A message deleted, edited away from waiting or released meanwhile has no packet and is skipped. */
    private fun releasePrepared(packet: ChatMessagePacket?, remaining: List<StoredMessage>): Outcome =
        if (packet == null) Outcome.ReleaseSkipped(remaining.drop(1)) else Outcome.ReleasePrepared(packet, remaining)

    private suspend fun checkAuthorization(step: Step.CheckAuthorization): Outcome {
        if (findDirectAuthorizationBlock(step.command.contactId).getOrThrow() == null) {
            return Outcome.Authorized(step.messages)
        }
        logger.debug { "Deferred ${step.messages.size} direct messages until mutual identity/keys are ready" }
        return Outcome.ReleaseDeferred
    }

    /**
     * The message was marked QUEUED before the outbox insert; a packet the outbox did not persist puts it
     * back to waiting so a later release retries it.
     */
    private suspend fun restoreWaitingMessage(step: Step.RestoreWaitingMessage): Outcome {
        val message = step.remaining.first()
        logger.warn(step.cause) {
            "Queued direct message could not be released after authorization: messageId=${message.id}"
        }
        restoreWaitingDirectMessage(message.id, step.packet.packetId).getOrThrow()
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

        data class ReleaseSkipped(
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
}
