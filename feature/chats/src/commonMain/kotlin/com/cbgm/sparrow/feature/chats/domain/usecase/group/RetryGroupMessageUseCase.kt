package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class RetryGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> = repository.retry(messageId)
}
