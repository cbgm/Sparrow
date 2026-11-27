package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UndeliveredGroupRecipients
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/**
 * The recipients of a queued group message whose packet is not in the outbox. A message without recipient
 * states goes to all current [recipients]; otherwise ONLY its original recipients are reconciled, so nobody
 * who joined in a later epoch is backfilled.
 */
class FindUndeliveredGroupRecipientsUseCase(
    private val messageRepository: GroupMessageRepository,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(messageId: String, recipients: List<String>): Result<UndeliveredGroupRecipients> =
        safeSuspendCall(logFailure = false) {
            val previousStates = messageRepository.findRecipientStates(messageId).getOrThrow()
            if (previousStates.isEmpty()) {
                return@safeSuspendCall UndeliveredGroupRecipients(recipients, isFirstDelivery = true)
            }
            val missing =
                previousStates.filter { state ->
                    val packetId = state.packetId
                    state.contactId in recipients &&
                        state.deliveryStatus == MessageDeliveryStatus.QUEUED &&
                        packetId != null &&
                        protocolOutbox.findByPacketId(packetId).getOrThrow() == null
                }.map { it.contactId }
            UndeliveredGroupRecipients(missing, isFirstDelivery = false)
        }
}
