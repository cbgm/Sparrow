package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMessageRepository

class MarkGroupConversationReadUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.markConversationRead(groupId)
}
