package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** Loads an own direct text message that the contact has not read yet; only such messages can be edited. */
class FindEditableDirectMessageUseCase(
    private val findDirectMessage: FindDirectMessageUseCase,
    private val checkMessageEditable: CheckMessageEditableUseCase
) {
    suspend operator fun invoke(conversationId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findDirectMessage(conversationId, messageId).getOrThrow()
            check(message.isMine) { "Only your own messages can be edited" }
            check(message.deliveryStatus != MessageDeliveryStatus.READ) { "Read messages cannot be edited" }
            checkMessageEditable.forDirectMessage(messageId).getOrThrow()?.let { reason -> error(reason) }
            message
        }
}
