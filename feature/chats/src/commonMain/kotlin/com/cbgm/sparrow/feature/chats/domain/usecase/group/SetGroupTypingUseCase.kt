package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTypingRepository

class SetGroupTypingUseCase(
    private val repository: GroupTypingRepository
) {
    suspend operator fun invoke(
        groupId: String,
        isTyping: Boolean
    ): Result<Unit> =
        repository.setTyping(
            groupId = groupId,
            isTyping = isTyping
        )
}
