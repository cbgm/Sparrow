package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class ToggleGroupMessageReactionUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String, messageId: String, emoji: String): Result<Unit> =
        repository.toggleReaction(groupId, messageId, emoji)
}
