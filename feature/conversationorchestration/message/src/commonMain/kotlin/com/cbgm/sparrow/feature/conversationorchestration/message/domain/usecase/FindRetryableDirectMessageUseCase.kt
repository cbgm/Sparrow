package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/** Which failed direct message may be retried, and whether its packet can simply be sent again. */
class FindRetryableDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository,
    private val protocolOutbox: ProtocolOutbox
) {
    /** Only own messages whose delivery failed can be retried. */
    suspend operator fun invoke(messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = messageRepository.find(messageId).getOrThrow() ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            check(
                DirectMessageDeliveryStateMachine.canTransition(
                    current = message.deliveryStatus,
                    event = MessageDeliveryEvent.RETRY_REQUESTED
                )
            ) { "Only failed direct messages can be retried" }
            message
        }

    /**
     * The ID of the message's packet in the outbox, or null when the outbox has none and a new packet is
     * needed. An explicit retry must not resurrect an obsolete packet after a contact changes cryptographic
     * identity: its stored transport payload was prepared for a different recipient key.
     */
    suspend fun findOutboxPacketId(message: StoredMessage): Result<String?> =
        safeSuspendCall(logFailure = false) {
            val packet = message.packetId?.let { packetId -> protocolOutbox.findByPacketId(packetId).getOrThrow() }
            check(packet?.status != OutboxStatus.QUARANTINED) {
                "This message belongs to the contact's previous identity. After reconnecting, send its content as a new message."
            }
            packet?.packetId
        }
}
