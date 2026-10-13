package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.group

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.SendDeliveryReceiptUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessagePlaintext
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.PrefetchLinkPreviewsUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.message.OperationMessage
import kotlin.reflect.KClass

/**
 * FindConversation -> DecryptMessage -> ApplyOperation                                   (an operation)
 * FindConversation -> DecryptMessage -> FindExisting -> [ApplyProfilePicture -> StoreMessage]
 *   -> PrefetchLinkPreviews -> StoreAttachments -> SendDeliveryReceipt -> CacheAttachments   (a message)
 *
 * A redelivered message keeps its row; only attachments and the receipt are repeated.
 */
class ReceiveGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val messageRepository: GroupMessageRepository,
    private val groupSecurityRepository: GroupSecurityRepository,
    private val plaintextCodec: GroupMessagePlaintextCodec,
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase,
    private val prefetchLinkPreviews: PrefetchLinkPreviewsUseCase,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val sendDeliveryReceipt: SendDeliveryReceiptUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupMessage,
        ReceiveGroupMessageWorkflow.Step,
        ReceiveGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupMessage> = IncomingCommand.ReceiveGroupMessage::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupMessage): Step = Step.FindConversation(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.FindConversation -> {
                val conversation =
                    messageRepository.findConversation(packet.groupId).getOrThrow() ?: error(
                        "Group conversation was not found"
                    )
                check(conversation.isGroup) { "Conversation is not a group" }
                Outcome.ConversationFound(conversation)
            }
            is Step.DecryptMessage -> {
                val plaintext = groupSecurityRepository.decryptMessage(packet, context.contactId).getOrThrow()
                when (val decoded = plaintextCodec.decode(plaintext)) {
                    is GroupMessagePlaintext.Operation -> Outcome.OperationDecrypted(decoded.message)
                    is GroupMessagePlaintext.Message ->
                        Outcome.MessageDecrypted(ReceivedGroupMessage(step.conversation, decoded))
                }
            }
            is Step.ApplyOperation -> {
                engine.run(
                    IncomingCommand.ApplyRemoteMessageOperation(
                        conversationId = packet.groupId,
                        senderContactId = context.contactId,
                        operation = step.message.operation,
                        conversation = OperationConversation.GROUP,
                        sentAtEpochMilliseconds = packet.sentAtEpochMilliseconds
                    )
                ).getOrThrow()
                Outcome.Done
            }
            is Step.FindExisting -> {
                val existing = messageRepository.find(packet.messageId).getOrThrow()
                if (existing == null) {
                    Outcome.NewMessage(step.message)
                } else {
                    check(
                        existing.conversationId == packet.groupId &&
                            existing.packetId == packet.packetId &&
                            existing.senderContactId == context.contactId
                    ) { "Group message ID conflicts with an existing message" }
                    Outcome.MessageStored(step.message.withCreatedAt(existing.createdAtEpochMilliseconds))
                }
            }
            is Step.ApplyProfilePicture -> {
                applyPeerProfilePicture(context.contactId, packet.profilePicture)
                Outcome.ProfilePictureApplied(step.message)
            }
            is Step.StoreMessage -> {
                messageRepository.saveIncoming(
                    step.command.toStoredMessage(step.message),
                    step.message.content.text,
                    context.receivedAtEpochMilliseconds
                )
                    .getOrThrow()
                Outcome.MessageStored(step.message.withCreatedAt(packet.sentAtEpochMilliseconds))
            }
            is Step.PrefetchLinkPreviews -> {
                prefetchLinkPreviews(step.message.content.text)
                Outcome.LinkPreviewsRequested(step.message)
            }
            is Step.StoreAttachments -> {
                attachmentRepository.persistIncoming(
                    messageId = packet.messageId,
                    parts = step.message.content.attachments,
                    context =
                        AttachmentMessageContext(
                            conversationId = step.message.conversation.id,
                            createdAtEpochMilliseconds = step.message.createdAtEpochMilliseconds,
                            displayName =
                                step.message.conversation.title?.takeIf(
                                    String::isNotBlank
                                ) ?: step.message.conversation.id,
                            isGroup = true,
                            isMine = false,
                            senderContactId = context.contactId
                        )
                )
                Outcome.AttachmentsStored
            }
            is Step.SendDeliveryReceipt -> {
                sendDeliveryReceipt.toGroupMember(context.contactId, packet.messageId).getOrThrow()
                Outcome.DeliveryReceiptSent
            }
            is Step.CacheAttachments -> {
                attachmentRepository.cacheIncoming(packet.messageId)
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationFound -> Transition.Next(Step.DecryptMessage(outcome.conversation, step.command))
            is Outcome.OperationDecrypted -> Transition.Next(Step.ApplyOperation(outcome.message, step.command))
            is Outcome.MessageDecrypted -> Transition.Next(Step.FindExisting(outcome.message, step.command))
            is Outcome.NewMessage -> Transition.Next(Step.ApplyProfilePicture(outcome.message, step.command))
            is Outcome.ProfilePictureApplied -> Transition.Next(Step.StoreMessage(outcome.message, step.command))
            is Outcome.MessageStored -> Transition.Next(Step.PrefetchLinkPreviews(outcome.message, step.command))
            is Outcome.LinkPreviewsRequested -> Transition.Next(Step.StoreAttachments(outcome.message, step.command))
            Outcome.AttachmentsStored -> Transition.Next(Step.SendDeliveryReceipt(step.command))
            Outcome.DeliveryReceiptSent -> Transition.Next(Step.CacheAttachments(step.command))
            Outcome.Done -> Transition.Complete()
        }

    private fun IncomingCommand.ReceiveGroupMessage.toStoredMessage(message: ReceivedGroupMessage): StoredMessage =
        StoredMessage(
            id = packet.messageId,
            conversationId = packet.groupId,
            packetId = packet.packetId,
            replyToMessageId = message.content.replyToMessageId,
            transportPayload = context.encodedTransportPayload,
            transportMode = MessageTransportMode.GROUP_END_TO_END_ENCRYPTED,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
            senderContactId = context.contactId,
            readReceiptSent = false,
            isMine = false,
            createdAtEpochMilliseconds = packet.sentAtEpochMilliseconds
        )

    /** The decrypted message, its group, and when it was stored (0 until known). */
    data class ReceivedGroupMessage(
        val conversation: ConversationInfo,
        val content: GroupMessagePlaintext.Message,
        val createdAtEpochMilliseconds: Long = 0L
    ) {
        fun withCreatedAt(createdAtEpochMilliseconds: Long): ReceivedGroupMessage =
            copy(createdAtEpochMilliseconds = createdAtEpochMilliseconds)
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupMessage

        data class FindConversation(
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class DecryptMessage(
            val conversation: ConversationInfo,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class ApplyOperation(
            val message: OperationMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class FindExisting(
            val message: ReceivedGroupMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class ApplyProfilePicture(
            val message: ReceivedGroupMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class StoreMessage(
            val message: ReceivedGroupMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class PrefetchLinkPreviews(
            val message: ReceivedGroupMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class StoreAttachments(
            val message: ReceivedGroupMessage,
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class SendDeliveryReceipt(
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step

        data class CacheAttachments(
            override val command: IncomingCommand.ReceiveGroupMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationFound(
            val conversation: ConversationInfo
        ) : Outcome

        data class OperationDecrypted(
            val message: OperationMessage
        ) : Outcome

        data class MessageDecrypted(
            val message: ReceivedGroupMessage
        ) : Outcome

        data class NewMessage(
            val message: ReceivedGroupMessage
        ) : Outcome

        data class ProfilePictureApplied(
            val message: ReceivedGroupMessage
        ) : Outcome

        /** The message row exists; [message] knows when it was created. */
        data class MessageStored(
            val message: ReceivedGroupMessage
        ) : Outcome

        data class LinkPreviewsRequested(
            val message: ReceivedGroupMessage
        ) : Outcome

        data object AttachmentsStored : Outcome

        data object DeliveryReceiptSent : Outcome

        data object Done : Outcome
    }
}
