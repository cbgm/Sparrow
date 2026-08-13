package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository

class SetGroupTypingUseCase(
    private val repository: GroupTypingRepository
) {
    suspend operator fun invoke(
        contactIds: Set<String>,
        isTyping: Boolean
    ): Result<Unit> =
        repository.sendToMembers(contactIds, isTyping)
}
