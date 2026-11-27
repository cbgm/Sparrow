package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/**
 * A released message whose packet the outbox did not persist goes back to waiting: a FAILED message pointing
 * at a nonexistent packet could never be retried. If the outbox did persist the packet, its own retry owns
 * the message status and nothing changes. A failed outbox lookup also leaves the message as it is.
 */
class RestoreWaitingDirectMessageUseCase(
    private val protocolOutbox: ProtocolOutbox,
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(messageId: String, packetId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val persisted = protocolOutbox.findByPacketId(packetId).getOrElse { return@safeSuspendCall }
            if (persisted != null) return@safeSuspendCall
            val current = messageRepository.find(messageId).getOrThrow()
            if (current?.packetId == packetId && current.deliveryStatus in RESTORABLE_STATUSES) {
                messageRepository.update(
                    current.copy(
                        packetId = null,
                        transportPayload = null,
                        deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION
                    )
                ).getOrThrow()
            }
        }

    private companion object {
        val RESTORABLE_STATUSES = setOf(MessageDeliveryStatus.QUEUED, MessageDeliveryStatus.FAILED)
    }
}
