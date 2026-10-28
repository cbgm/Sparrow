package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredOutgoingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/** Builds a new packet for a persisted direct message. */
class CreateDirectMessagePacketUseCase(
    private val messageRepository: DirectMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val packetFactory: DirectMessagePacketFactory
) {
    /** A message stored just now: its content is still in memory. The packet is linked to the message. */
    suspend operator fun invoke(stored: StoredOutgoingMessage): Result<ChatMessagePacket> =
        safeSuspendCall(logFailure = false) {
            val packet =
                packetFactory.create(
                    messageId = stored.message.id,
                    text = stored.text,
                    parts = stored.parts,
                    replyToMessageId = stored.message.replyToMessageId
                ).getOrThrow()
            link(stored.message.id, packet, missingMessage = "Stored direct message was not found")
            packet
        }

    /** A failed message is retried because its previous packet is gone. The packet is linked to the message. */
    suspend fun forRetry(message: StoredMessage): Result<ChatMessagePacket> =
        safeSuspendCall(logFailure = false) {
            val packet = create(message)
            link(message.id, packet, missingMessage = "Message was not found")
            packet
        }

    /**
     * A waiting message is released. The caller stores the packet ID together with the message's new delivery
     * state.
     */
    suspend fun forRelease(message: StoredMessage): Result<ChatMessagePacket> =
        safeSuspendCall(logFailure = false) { create(message) }

    /**
     * A stored message's attachments may not be uploaded yet (an interrupted upload, or a message that waited for
     * authorization), so they are prepared first; the packet must never point at a blob that is not on a node. The
     * text is read before the parts, so preparing attachments happens only for readable messages.
     */
    private suspend fun create(message: StoredMessage): ChatMessagePacket =
        packetFactory.create(
            messageId = message.id,
            text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
            parts = attachmentRepository.prepareOutgoing(message.id).getOrThrow(),
            replyToMessageId = message.replyToMessageId
        ).getOrThrow()

    /** The message is loaded again because other workflows may have changed it since it was read. */
    private suspend fun link(messageId: String, packet: ChatMessagePacket, missingMessage: String) {
        val current = messageRepository.find(messageId).getOrThrow() ?: error(missingMessage)
        messageRepository.update(current.copy(packetId = packet.packetId)).getOrThrow()
    }
}
