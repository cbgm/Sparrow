package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/**
 * A send that stopped between storing the message and queuing its packet (the app was closed or crashed) leaves
 * the message queued with nothing to deliver it. Such messages, created before [invoke]'s cutoff (the app start),
 * become failed, so the user can retry them; the retry creates a new packet.
 */
class RecoverInterruptedDirectSendsUseCase(
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(createdBeforeEpochMilliseconds: Long): Result<Int> =
        safeSuspendCall {
            val interrupted = messageRepository.findAwaitingOutbox(createdBeforeEpochMilliseconds).getOrThrow()
            interrupted.forEach { message ->
                messageRepository.update(
                    message.copy(
                        deliveryStatus =
                            DirectMessageDeliveryStateMachine.transition(
                                message.deliveryStatus,
                                MessageDeliveryEvent.SEND_FAILED
                            )
                    )
                ).getOrThrow()
            }
            interrupted.size
        }
}
