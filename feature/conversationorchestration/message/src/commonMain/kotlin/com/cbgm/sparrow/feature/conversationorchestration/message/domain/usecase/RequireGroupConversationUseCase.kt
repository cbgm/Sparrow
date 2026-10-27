package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Loads a conversation that must exist and be a group. */
class RequireGroupConversationUseCase(
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<ConversationInfo> =
        safeSuspendCall(logFailure = false) {
            val conversation = messageRepository.findConversation(groupId).getOrThrow()
                ?: error("Group conversation was not found")
            check(conversation.isGroup) { "Conversation is not a group" }
            conversation
        }
}
