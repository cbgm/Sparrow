package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** Loads an own group user message that may be deleted for everyone; payment records stay. */
class FindDeletableGroupMessageUseCase(
    private val findGroupMessage: FindGroupMessageUseCase,
    private val requireMessageWithoutPayment: RequireMessageWithoutPaymentUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findGroupMessage(groupId, messageId).getOrThrow()
            check(message.isGroupUserMessage) { "Only user messages can be deleted" }
            requireMessageWithoutPayment(message.id).getOrThrow()
            check(message.isMine) { "Only your own messages can be deleted for everyone" }
            message
        }
}
