package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/**
 * A message whose packet could not be made yet (its attachment did not go out) stays in the chat as failed, with
 * its files: the user can retry it, and it is sent again by itself ([SendUnsentMessagesUseCase]).
 */
class KeepUnsentDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(message: StoredMessage): Result<Unit> =
        safeSuspendCall {
            val current = messageRepository.find(message.id).getOrThrow() ?: return@safeSuspendCall
            val failed = DirectMessageDeliveryStateMachine.transition(current.deliveryStatus, MessageDeliveryEvent.SEND_FAILED)
            messageRepository.update(current.copy(deliveryStatus = failed)).getOrThrow()
        }
}
