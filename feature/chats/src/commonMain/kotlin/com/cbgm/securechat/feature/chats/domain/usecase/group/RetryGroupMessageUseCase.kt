package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMessageRepository

class RetryGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> = repository.retry(messageId)
}
