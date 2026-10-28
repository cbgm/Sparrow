package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** Loads an own direct message that may be deleted for everyone. */
class FindDeletableDirectMessageUseCase(
    private val findDirectMessage: FindDirectMessageUseCase
) {
    suspend operator fun invoke(conversationId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findDirectMessage(conversationId, messageId).getOrThrow()
            check(message.isMine) { "Only your own messages can be deleted for everyone" }
            message
        }
}
