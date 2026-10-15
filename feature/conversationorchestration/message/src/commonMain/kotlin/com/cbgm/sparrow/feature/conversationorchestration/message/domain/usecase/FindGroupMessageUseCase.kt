package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Loads a message that an operation targets and verifies it belongs to the group. */
class FindGroupMessageUseCase(
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            val message = messageRepository.find(messageId).getOrThrow() ?: error("Message was not found")
            check(message.conversationId == groupId) { "Message does not belong to this group" }
            message
        }
}
