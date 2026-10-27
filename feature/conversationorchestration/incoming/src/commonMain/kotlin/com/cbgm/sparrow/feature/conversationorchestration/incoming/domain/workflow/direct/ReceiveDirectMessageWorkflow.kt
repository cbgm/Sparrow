package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.direct

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.domain.model.Text
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ClaimAutoReplyForContactUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ReleaseAutoReplyRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectInvalid
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectUnless
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.packet.DirectMessagePacketReader
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.IncomingDirectTransportPolicy
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.SendDeliveryReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StoreIncomingDirectAttachmentsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.UpdateIncomingSenderProfileUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.reflect.KClass

/**
 * ReadMessage -> FindExisting -> [UpdateSenderProfile -> StoreMessage] -> StoreAttachments -> SendDeliveryReceipt
 * -> ClaimAutoReply -> [SendAutoReply -> [ReleaseAutoReplyClaim]] -> CacheAttachments -> ApplyParkedOperations
 *
 * The router created the conversation and checked the authorization before this workflow starts. A redelivered
 * message keeps its row, so edits, deletions and the read state applied since are not undone; only attachments
 * and the receipt are repeated. A message ID that belongs to another message is rejected.
 */
class ReceiveDirectMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val packetReader: DirectMessagePacketReader,
    private val updateIncomingSenderProfile: UpdateIncomingSenderProfileUseCase,
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val storeIncomingDirectAttachments: StoreIncomingDirectAttachmentsUseCase,
    private val sendDeliveryReceipt: SendDeliveryReceiptUseCase,
    private val claimAutoReplyForContact: ClaimAutoReplyForContactUseCase,
    private val releaseAutoReplyRecipient: ReleaseAutoReplyRecipientUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveDirectMessage,
        ReceiveDirectMessageWorkflow.Step,
        ReceiveDirectMessageWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReceiveDirectMessageWorkflow")

    override val commandType: KClass<IncomingCommand.ReceiveDirectMessage> = IncomingCommand.ReceiveDirectMessage::class

    override fun firstStep(command: IncomingCommand.ReceiveDirectMessage): Step = Step.ReadMessage(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.ReadMessage -> Outcome.MessageRead(rejectInvalid { readMessage(step.command) })
            is Step.FindExisting -> findExisting(step)
            is Step.UpdateSenderProfile -> {
                updateIncomingSenderProfile(context.contactId, packet, context.receivedAtEpochMilliseconds).getOrThrow()
                Outcome.SenderProfileUpdated(step.content)
            }
            is Step.StoreMessage -> {
                messageRepository.saveIncoming(
                    packet.toStoredMessage(step.command),
                    step.content.text,
                    context.receivedAtEpochMilliseconds
                )
                    .getOrThrow()
                Outcome.MessageStored(step.content)
            }
            is Step.StoreAttachments -> {
                storeIncomingDirectAttachments(context, packet, step.content.attachments).getOrThrow()
                Outcome.AttachmentsStored
            }
            is Step.SendDeliveryReceipt -> {
                sendDeliveryReceipt.toContact(context.contactId, packet.messageId).getOrThrow()
                Outcome.DeliveryReceiptSent
            }
            is Step.ClaimAutoReply ->
                claimAutoReplyForContact(context.contactId)
                    .onFailure { error ->
                        logger.error(error) { "Could not claim automatic reply for ${context.contactId}" }
                    }
                    .getOrNull()
                    ?.let(Outcome::AutoReplyClaimed)
                    ?: Outcome.NoAutoReply
            is Step.SendAutoReply -> sendAutoReply(step)
            is Step.ReleaseAutoReplyClaim -> {
                releaseAutoReplyRecipient(context.contactId, step.activationSessionId)
                    .onFailure { error ->
                        logger.error(error) {
                            "Could not release failed auto-reply claim for contactId=${context.contactId}"
                        }
                    }
                Outcome.AutoReplyHandled
            }
            is Step.CacheAttachments -> {
                attachmentRepository.cacheIncoming(packet.messageId).getOrThrow()
                Outcome.AttachmentsCached
            }
            is Step.ApplyParkedOperations -> {
                engine.run(IncomingCommand.ApplyParkedMessageOperations(packet.messageId)).getOrThrow()
                Outcome.ParkedOperationsApplied
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.MessageRead -> Transition.Next(Step.FindExisting(outcome.content, step.command))
            is Outcome.NewMessage -> Transition.Next(Step.UpdateSenderProfile(outcome.content, step.command))
            is Outcome.SenderProfileUpdated -> Transition.Next(Step.StoreMessage(outcome.content, step.command))
            is Outcome.MessageStored -> Transition.Next(Step.StoreAttachments(outcome.content, step.command))
            Outcome.AttachmentsStored -> Transition.Next(Step.SendDeliveryReceipt(step.command))
            Outcome.DeliveryReceiptSent -> Transition.Next(Step.ClaimAutoReply(step.command))
            is Outcome.AutoReplyClaimed -> Transition.Next(Step.SendAutoReply(outcome.reply, step.command))
            Outcome.NoAutoReply,
            Outcome.AutoReplyHandled -> Transition.Next(Step.CacheAttachments(step.command))
            is Outcome.AutoReplyFailed ->
                Transition.Next(Step.ReleaseAutoReplyClaim(outcome.activationSessionId, step.command))
            Outcome.AttachmentsCached -> Transition.Next(Step.ApplyParkedOperations(step.command))
            Outcome.ParkedOperationsApplied -> Transition.Complete()
        }

    private fun readMessage(command: IncomingCommand.ReceiveDirectMessage): IncomingMessageContent {
        require(command.packet.parts.isNotEmpty()) { "Incoming chat message must contain message parts" }
        val content = packetReader.read(command.packet)
        IncomingDirectTransportPolicy.requireAttachmentsAllowed(
            command.context.transportMode,
            hasAttachments = content.attachments.isNotEmpty()
        )
        return content
    }

    private suspend fun findExisting(step: Step.FindExisting): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        val existing =
            messageRepository.find(packet.messageId).getOrThrow() ?: return Outcome.NewMessage(step.content)
        rejectUnless(
            !existing.isMine && existing.isRedeliveryOf(context.conversationId, packet.packetId, context.contactId)
        ) { "Direct message ID conflicts with an existing message" }
        return Outcome.MessageStored(step.content)
    }

    private suspend fun sendAutoReply(step: Step.SendAutoReply): Outcome {
        val contactId = step.command.context.contactId
        val result =
            engine.run(
                DirectConversationCommand.SendMessage(
                    contactId = contactId,
                    parts = listOf(Text(id = IdGenerator.generate(prefix = "text"), text = step.reply.text)),
                    conversationId = step.command.context.conversationId
                )
            )
        val error = result.exceptionOrNull() ?: return Outcome.AutoReplyHandled
        logger.error(error) { "Auto reply could not be sent to contactId=$contactId" }
        return step.reply.activationSessionId?.let(Outcome::AutoReplyFailed) ?: Outcome.AutoReplyHandled
    }

    private fun ChatMessagePacket.toStoredMessage(command: IncomingCommand.ReceiveDirectMessage): StoredMessage =
        StoredMessage(
            id = messageId,
            conversationId = command.context.conversationId,
            packetId = packetId,
            replyToMessageId = replyToMessageId,
            transportPayload = command.context.encodedTransportPayload,
            transportMode = command.context.transportMode,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
            senderContactId = command.context.contactId,
            readReceiptSent = false,
            isMine = false,
            createdAtEpochMilliseconds = command.context.receivedAtEpochMilliseconds
        )

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveDirectMessage

        data class ReadMessage(
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class FindExisting(
            val content: IncomingMessageContent,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class UpdateSenderProfile(
            val content: IncomingMessageContent,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class StoreMessage(
            val content: IncomingMessageContent,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class StoreAttachments(
            val content: IncomingMessageContent,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class SendDeliveryReceipt(
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class ClaimAutoReply(
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class SendAutoReply(
            val reply: AutoReply,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class ReleaseAutoReplyClaim(
            val activationSessionId: String,
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        data class CacheAttachments(
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step

        /** Edits, deletes and reactions that arrived before this message. */
        data class ApplyParkedOperations(
            override val command: IncomingCommand.ReceiveDirectMessage
        ) : Step
    }

    sealed interface Outcome {
        data class MessageRead(
            val content: IncomingMessageContent
        ) : Outcome

        data class NewMessage(
            val content: IncomingMessageContent
        ) : Outcome

        data class SenderProfileUpdated(
            val content: IncomingMessageContent
        ) : Outcome

        data class MessageStored(
            val content: IncomingMessageContent
        ) : Outcome

        data object AttachmentsStored : Outcome

        data object DeliveryReceiptSent : Outcome

        data class AutoReplyClaimed(
            val reply: AutoReply
        ) : Outcome

        data object NoAutoReply : Outcome

        /** The reply was sent, or its failure cannot be undone. */
        data object AutoReplyHandled : Outcome

        data class AutoReplyFailed(
            val activationSessionId: String
        ) : Outcome

        data object AttachmentsCached : Outcome

        data object ParkedOperationsApplied : Outcome
    }
}
