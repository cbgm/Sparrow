package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/**
 * Turns a message that waited for authorization into a queued one with a new packet. The message is
 * marked queued before its packet reaches the outbox; [RestoreWaitingDirectMessageUseCase] undoes that
 * when the outbox rejects the packet.
 */
class ReleaseWaitingDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository,
    private val createDirectMessagePacket: CreateDirectMessagePacketUseCase
) {
    suspend operator fun invoke(message: StoredMessage): Result<ChatMessagePacket> =
        safeSuspendCall(logFailure = false) {
            val nextStatus =
                DirectMessageDeliveryStateMachine.transition(
                    current = message.deliveryStatus,
                    event = MessageDeliveryEvent.AUTHORIZATION_GRANTED
                )
            check(nextStatus == MessageDeliveryStatus.QUEUED) { "Direct message is not waiting for authorization" }

            val packet = createDirectMessagePacket.forRelease(message).getOrThrow()
            messageRepository.update(
                message.copy(
                    packetId = packet.packetId,
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    deliveryStatus = nextStatus
                )
            ).getOrThrow()
            packet
        }
}
