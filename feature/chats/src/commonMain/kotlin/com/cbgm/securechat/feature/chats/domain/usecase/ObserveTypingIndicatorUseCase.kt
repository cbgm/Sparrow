package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorRepository
import kotlinx.coroutines.flow.Flow

class ObserveTypingIndicatorUseCase(
    private val repository: TypingIndicatorRepository
) {
    operator fun invoke(contactId: String): Flow<Boolean> = repository.observeTyping(contactId = contactId)
}
