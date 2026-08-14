package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class RetryDirectMessageUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> = repository.retry(messageId)
}
