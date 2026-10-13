package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectRecipient
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredOutgoingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.LoadDirectRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordPacketSendFailureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingDirectMessageUseCase
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.reflect.KClass

/**
 * FindConversation -> CheckAuthorization -> LoadRecipient -> StoreMessage -> CreatePacket
 * -> LinkPacket -> EnqueuePacket.
 *
 * A packet that cannot be created or linked discards the stored message again; a packet the
 * outbox rejects keeps the message and marks it failed so the user can retry.
 */
class DeliverDirectMessageWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val loadDirectRecipient: LoadDirectRecipientUseCase,
    private val storeOutgoingDirectMessage: StoreOutgoingDirectMessageUseCase,
    private val discardDirectMessages: DiscardDirectMessagesUseCase,
    private val packetFactory: DirectMessagePacketFactory,
    private val protocolOutbox: ProtocolOutbox,
    private val recordPacketSendFailure: RecordPacketSendFailureUseCase
) : WorkflowStateMachine<
        MessageCommand.DeliverDirectMessage,
        DeliverDirectMessageWorkflow.Step,
        DeliverDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.DeliverDirectMessage> = MessageCommand.DeliverDirectMessage::class

    override fun firstStep(command: MessageCommand.DeliverDirectMessage): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindConversation ->
                Outcome.ConversationFound(
                    conversationRepository.requireTarget(step.command.conversationId).getOrThrow()
                )
            is Step.CheckAuthorization ->
                findDirectAuthorizationBlock(step.target.contactId).getOrThrow()
                    ?.let(Outcome::AuthorizationMissing)
                    ?: Outcome.Authorized(step.target)
            is Step.LoadRecipient -> Outcome.RecipientLoaded(loadDirectRecipient(step.target).getOrThrow())
            is Step.StoreMessage ->
                Outcome.MessageStored(
                    stored = storeOutgoingDirectMessage(
                        recipient = step.recipient,
                        content = step.command.content,
                        deliveryStatus = MessageDeliveryStatus.QUEUED,
                        replyToMessageId = step.command.replyToMessageId
                    ).getOrThrow(),
                    contactId = step.recipient.target.contactId
                )
            is Step.CreatePacket -> createPacket(step)
            is Step.LinkPacket -> linkPacket(step)
            is Step.DiscardMessage -> {
                discardDirectMessages(listOf(step.stored.message)).getOrThrow()
                Outcome.MessageDiscarded(step.cause)
            }
            is Step.EnqueuePacket ->
                protocolOutbox.enqueue(step.contactId, step.packet).fold(
                    onSuccess = { Outcome.PacketEnqueued },
                    onFailure = { error -> Outcome.EnqueueFailed(step.packet, error) }
                )
            is Step.MarkSendFailed -> {
                recordPacketSendFailure(step.packet.packetId, step.cause.message).getOrThrow()
                Outcome.SendFailureRecorded(step.cause)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.CheckAuthorization(outcome.target, step.command))
            is Outcome.Authorized -> Transition.Next(Step.LoadRecipient(outcome.target, step.command))
            is Outcome.AuthorizationMissing ->
                Transition.Reject(ConversationFailure(ConversationFailureReason.AUTHORIZATION_REQUIRED, outcome.reason))
            is Outcome.RecipientLoaded -> Transition.Next(Step.StoreMessage(outcome.recipient, step.command))
            is Outcome.MessageStored ->
                Transition.Next(Step.CreatePacket(outcome.stored, outcome.contactId, step.command))
            is Outcome.PacketCreated ->
                Transition.Next(Step.LinkPacket(outcome.packet, outcome.stored, outcome.contactId, step.command))
            is Outcome.PacketPreparationFailed ->
                Transition.Next(Step.DiscardMessage(outcome.stored, outcome.cause, step.command))
            is Outcome.MessageDiscarded -> Transition.Reject.because(outcome.cause)
            is Outcome.PacketLinked ->
                Transition.Next(Step.EnqueuePacket(outcome.packet, outcome.contactId, step.command))
            Outcome.PacketEnqueued -> Transition.Complete(ConversationResult.DirectMessageSent)
            is Outcome.EnqueueFailed ->
                Transition.Next(Step.MarkSendFailed(outcome.packet, outcome.cause, step.command))
            is Outcome.SendFailureRecorded -> Transition.Reject.because(outcome.cause)
        }

    private suspend fun createPacket(step: Step.CreatePacket): Outcome =
        packetFactory.create(
            messageId = step.stored.message.id,
            text = step.stored.text,
            parts = step.stored.parts,
            replyToMessageId = step.stored.message.replyToMessageId
        ).fold(
            onSuccess = { packet -> Outcome.PacketCreated(packet, step.stored, step.contactId) },
            onFailure = { error -> Outcome.PacketPreparationFailed(step.stored, error) }
        )

    private suspend fun linkPacket(step: Step.LinkPacket): Outcome =
        runCatching {
            val stored = messageRepository.find(step.stored.message.id).getOrThrow()
                ?: error("Stored direct message was not found")
            messageRepository.update(stored.copy(packetId = step.packet.packetId)).getOrThrow()
        }.fold(
            onSuccess = { Outcome.PacketLinked(step.packet, step.contactId) },
            onFailure = { error -> Outcome.PacketPreparationFailed(step.stored, error) }
        )

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.DeliverDirectMessage

        data class FindConversation(
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class LoadRecipient(
            val target: DirectConversationTarget,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class StoreMessage(
            val recipient: DirectRecipient,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class CreatePacket(
            val stored: StoredOutgoingMessage,
            val contactId: String,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class LinkPacket(
            val packet: ChatMessagePacket,
            val stored: StoredOutgoingMessage,
            val contactId: String,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class DiscardMessage(
            val stored: StoredOutgoingMessage,
            val cause: Throwable,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class EnqueuePacket(
            val packet: ChatMessagePacket,
            val contactId: String,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step

        data class MarkSendFailed(
            val packet: ChatMessagePacket,
            val cause: Throwable,
            override val command: MessageCommand.DeliverDirectMessage
        ) : Step
    }

    /** What a step reported, including everything the following decision needs. */
    sealed interface Outcome {
        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class Authorized(
            val target: DirectConversationTarget
        ) : Outcome

        data class AuthorizationMissing(
            val reason: String
        ) : Outcome

        data class RecipientLoaded(
            val recipient: DirectRecipient
        ) : Outcome

        data class MessageStored(
            val stored: StoredOutgoingMessage,
            val contactId: String
        ) : Outcome

        data class PacketCreated(
            val packet: ChatMessagePacket,
            val stored: StoredOutgoingMessage,
            val contactId: String
        ) : Outcome

        data class PacketPreparationFailed(
            val stored: StoredOutgoingMessage,
            val cause: Throwable
        ) : Outcome

        data class PacketLinked(
            val packet: ChatMessagePacket,
            val contactId: String
        ) : Outcome

        data class MessageDiscarded(
            val cause: Throwable
        ) : Outcome

        data object PacketEnqueued : Outcome

        data class EnqueueFailed(
            val packet: ChatMessagePacket,
            val cause: Throwable
        ) : Outcome

        data class SendFailureRecorded(
            val cause: Throwable
        ) : Outcome
    }
}
