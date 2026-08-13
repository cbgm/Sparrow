package com.cbgm.securechat.feature.chats.domain.usecase.direct

import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectTypingRepository

class SetDirectTypingUseCase(
    private val repository: DirectTypingRepository
) {
    suspend operator fun invoke(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        repository.send(contactId, isTyping)
}
