package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorRepository

class SetTypingIndicatorUseCase(
    private val repository: TypingIndicatorRepository
) {
    suspend operator fun invoke(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        repository.sendTypingState(
            contactId = contactId,
            isTyping = isTyping
        )
}
