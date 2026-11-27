package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket

/**
 * Records which packet each recipient of a group message gets, as queued. The mapping is persisted before
 * the packets are enqueued; restart reconciliation covers a crash in between.
 */
class SaveQueuedGroupRecipientsUseCase(
    private val messageRepository: GroupMessageRepository
) {
    /** A message stored just now. */
    suspend operator fun invoke(
        messageId: String,
        packetsByContactId: Map<String, GroupChatMessagePacket>
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            messageRepository.saveRecipientStates(
                queuedStates(messageId, packetsByContactId, SystemClock.nowEpochMilliseconds())
            ).getOrThrow()
        }

    /** A queued message delivered for the first time; it is saved again together with its states. */
    suspend fun forQueuedMessage(
        message: StoredMessage,
        packetsByContactId: Map<String, GroupChatMessagePacket>
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val now = SystemClock.nowEpochMilliseconds()
            messageRepository.saveOutgoing(
                message = message,
                text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                recipientStates = queuedStates(message.id, packetsByContactId, now)
            ).getOrThrow()
        }

    private fun queuedStates(
        messageId: String,
        packetsByContactId: Map<String, GroupChatMessagePacket>,
        nowEpochMilliseconds: Long
    ): List<GroupRecipientState> =
        packetsByContactId.map { (contactId, packet) ->
            GroupRecipientState(
                messageId = messageId,
                contactId = contactId,
                packetId = packet.packetId,
                deliveryStatus = MessageDeliveryStatus.QUEUED,
                lastError = null,
                updatedAtEpochMilliseconds = nowEpochMilliseconds
            )
        }
}
