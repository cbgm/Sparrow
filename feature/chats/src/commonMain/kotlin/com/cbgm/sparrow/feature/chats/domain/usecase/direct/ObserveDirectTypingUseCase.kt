package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectTypingRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectTypingUseCase(
    private val repository: DirectTypingRepository
) {
    operator fun invoke(contactId: String): Flow<Boolean> = repository.observe(contactId)
}
