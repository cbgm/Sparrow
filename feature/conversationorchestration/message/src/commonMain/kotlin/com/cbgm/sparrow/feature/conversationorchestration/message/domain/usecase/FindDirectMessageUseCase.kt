package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/** Loads a message that an operation targets and verifies it belongs to the direct conversation. */
class FindDirectMessageUseCase(
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(conversationId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = messageRepository.find(messageId).getOrThrow() ?: error("Message was not found")
            check(message.conversationId == conversationId) { "Message does not belong to this conversation" }
            message
        }
}
