package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class ToggleDirectMessageReactionUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(conversationId: String, messageId: String, emoji: String): Result<Unit> =
        repository.toggleReaction(conversationId, messageId, emoji)
}
