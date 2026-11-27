package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.CreateDirectMessagePacketUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindRetryableDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordRetryRequestedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindRetryableMessage -> FindConversation -> CheckAuthorization -> FindPacket
 * -> (ResendPacket | CreatePacket -> EnqueuePacket) -> RecordRetry.
 *
 * A message whose packet never reached the outbox, or is no longer in it, gets a new packet; resending a packet
 * the outbox does not know would do nothing and leave the message queued forever.
 */
class RetryDirectMessageWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val findRetryableDirectMessage: FindRetryableDirectMessageUseCase,
    private val createDirectMessagePacket: CreateDirectMessagePacketUseCase,
    private val protocolOutbox: ProtocolOutbox,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val recordRetryRequested: RecordRetryRequestedUseCase
) : ConversationWorkflow<
        DirectConversationCommand.RetryMessage,
        RetryDirectMessageWorkflow.Step,
        RetryDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.RetryMessage> =
        DirectConversationCommand.RetryMessage::class

    override fun firstStep(command: DirectConversationCommand.RetryMessage): Step = Step.FindRetryableMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindRetryableMessage -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                Outcome.RetryableMessageFound(findRetryableDirectMessage(step.command.messageId).getOrThrow())
            }
            is Step.FindConversation ->
                Outcome.ConversationFound(
                    target = conversationRepository.requireTarget(step.message.conversationId).getOrThrow(),
                    message = step.message
                )
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.target.contactId).getOrThrow()
                Outcome.Authorized(step.target, step.message)
            }
            is Step.FindPacket -> findPacket(step.target, step.message)
            is Step.ResendPacket -> {
                protocolOutbox.resend(step.packetId).getOrThrow()
                Outcome.PacketResent
            }
            is Step.CreatePacket ->
                Outcome.PacketCreated(step.target, createDirectMessagePacket.forRetry(step.message).getOrThrow())
            is Step.EnqueuePacket -> {
                protocolOutbox.enqueue(step.target.contactId, step.packet).getOrThrow()
                Outcome.PacketResent
            }
            is Step.RecordRetry -> {
                recordRetryRequested.forDirectMessage(step.command.messageId).getOrThrow()
                Outcome.RetryRecorded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.RetryableMessageFound -> Transition.Next(Step.FindConversation(outcome.message, step.command))
            is Outcome.ConversationFound ->
                Transition.Next(Step.CheckAuthorization(outcome.target, outcome.message, step.command))
            is Outcome.Authorized -> Transition.Next(Step.FindPacket(outcome.target, outcome.message, step.command))
            is Outcome.PacketInOutbox -> Transition.Next(Step.ResendPacket(outcome.packetId, step.command))
            is Outcome.PacketMissing ->
                Transition.Next(Step.CreatePacket(outcome.target, outcome.message, step.command))
            is Outcome.PacketCreated ->
                Transition.Next(Step.EnqueuePacket(outcome.target, outcome.packet, step.command))
            Outcome.PacketResent -> Transition.Next(Step.RecordRetry(step.command))
            Outcome.RetryRecorded -> Transition.Complete()
        }

    private suspend fun findPacket(target: DirectConversationTarget, message: StoredMessage): Outcome =
        findRetryableDirectMessage.findOutboxPacketId(message).getOrThrow()
            ?.let(Outcome::PacketInOutbox)
            ?: Outcome.PacketMissing(target, message)

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.RetryMessage

        data class FindRetryableMessage(
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class FindConversation(
            val message: StoredMessage,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            val message: StoredMessage,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class FindPacket(
            val target: DirectConversationTarget,
            val message: StoredMessage,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class ResendPacket(
            val packetId: String,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class CreatePacket(
            val target: DirectConversationTarget,
            val message: StoredMessage,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class EnqueuePacket(
            val target: DirectConversationTarget,
            val packet: ChatMessagePacket,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class RecordRetry(
            override val command: DirectConversationCommand.RetryMessage
        ) : Step
    }

    sealed interface Outcome {
        data class RetryableMessageFound(
            val message: StoredMessage
        ) : Outcome

        data class ConversationFound(
            val target: DirectConversationTarget,
            val message: StoredMessage
        ) : Outcome

        data class Authorized(
            val target: DirectConversationTarget,
            val message: StoredMessage
        ) : Outcome

        data class PacketInOutbox(
            val packetId: String
        ) : Outcome

        /** The outbox has no packet for the message. */
        data class PacketMissing(
            val target: DirectConversationTarget,
            val message: StoredMessage
        ) : Outcome

        data class PacketCreated(
            val target: DirectConversationTarget,
            val packet: ChatMessagePacket
        ) : Outcome

        data object PacketResent : Outcome

        data object RetryRecorded : Outcome
    }
}
