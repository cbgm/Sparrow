package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Which recipients of an own group message get its packet again; only failed deliveries are retried. */
class FindRetryableGroupRecipientsUseCase(
    private val messageRepository: GroupMessageRepository,
    private val requireGroupConversation: RequireGroupConversationUseCase
) {
    suspend operator fun invoke(messageId: String): Result<List<GroupRecipientState>> =
        safeSuspendCall(logFailure = false) {
            val message = messageRepository.find(messageId).getOrThrow() ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            requireGroupConversation(message.conversationId).getOrThrow()
            val failed = messageRepository.findRecipientStates(message.id).getOrThrow()
                .filter { state -> state.deliveryStatus == MessageDeliveryStatus.FAILED }
            check(failed.isNotEmpty()) { "Only failed group messages can be retried" }
            failed
        }

    /** The packet to resend to [recipient]; its current state is read again because a receipt may have arrived. */
    suspend fun requireRetryablePacketId(recipient: GroupRecipientState): Result<String> =
        safeSuspendCall(logFailure = false) {
            val packetId = requireNotNull(recipient.packetId) { "Recipient state has no packet ID" }
            val current = messageRepository.findRecipientByPacketId(packetId).getOrThrow()
                ?: error("Recipient delivery state was not found")
            check(
                GroupMessageDeliveryStateMachine.canTransition(
                    current = current.deliveryStatus,
                    event = MessageDeliveryEvent.RETRY_REQUESTED
                )
            ) { "Recipient delivery is not retryable" }
            packetId
        }
}
