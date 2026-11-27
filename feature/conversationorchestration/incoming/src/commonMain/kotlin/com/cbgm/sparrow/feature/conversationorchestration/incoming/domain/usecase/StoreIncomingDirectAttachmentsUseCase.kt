package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/**
 * Stores the attachments of a received direct message. They are dated like the stored message, so a redelivery
 * keeps the original date; a message that is not stored falls back to the receive time.
 */
class StoreIncomingDirectAttachmentsUseCase(
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val resolveSenderDisplayName: ResolveSenderDisplayNameUseCase
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ChatMessagePacket,
        attachments: List<MessagePart>
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            attachmentRepository.persistIncoming(
                messageId = packet.messageId,
                parts = attachments,
                context =
                    AttachmentMessageContext(
                        conversationId = context.conversationId,
                        createdAtEpochMilliseconds =
                            messageRepository.find(packet.messageId).getOrThrow()?.createdAtEpochMilliseconds
                                ?: context.receivedAtEpochMilliseconds,
                        displayName = resolveSenderDisplayName(context.contactId, packet.senderPhoneNumber).getOrThrow(),
                        isGroup = false,
                        isMine = false,
                        senderContactId = context.contactId
                    )
            ).getOrThrow()
        }
}
