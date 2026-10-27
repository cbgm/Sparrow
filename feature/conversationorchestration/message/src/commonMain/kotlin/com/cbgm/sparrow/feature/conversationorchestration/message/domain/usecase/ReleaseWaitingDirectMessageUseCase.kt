package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/**
 * Turns a message that waited for authorization into a queued one with a new packet. The message is
 * marked queued before its packet reaches the outbox; [RestoreWaitingDirectMessageUseCase] undoes that
 * when the outbox rejects the packet.
 *
 * The message is read again because the list of waiting messages may be stale: a message that was deleted or
 * is no longer waiting is skipped (null), and the status change is conditional so a deleted message is never
 * written back.
 */
class ReleaseWaitingDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository,
    private val createDirectMessagePacket: CreateDirectMessagePacketUseCase
) {
    suspend operator fun invoke(messageId: String): Result<ChatMessagePacket?> =
        safeSuspendCall(logFailure = false) {
            val message = messageRepository.find(messageId).getOrThrow() ?: return@safeSuspendCall null
            if (!message.wasNeverSent) return@safeSuspendCall null
            val nextStatus =
                DirectMessageDeliveryStateMachine.transition(
                    current = message.deliveryStatus,
                    event = MessageDeliveryEvent.AUTHORIZATION_GRANTED
                )
            check(nextStatus == MessageDeliveryStatus.QUEUED) { "Direct message is not waiting for authorization" }

            val packet = createDirectMessagePacket.forRelease(message).getOrThrow()
            val released =
                messageRepository.updatePacketIfStatus(
                    messageId = message.id,
                    expectedStatus = message.deliveryStatus,
                    packetId = packet.packetId,
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    deliveryStatus = nextStatus
                ).getOrThrow()
            packet.takeIf { released }
        }
}
