package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class CreateGroupConversationUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        title: String,
        contactIds: Set<String>
    ): Result<String> = runCatching { repository.createGroupConversation(title, contactIds) }
}
