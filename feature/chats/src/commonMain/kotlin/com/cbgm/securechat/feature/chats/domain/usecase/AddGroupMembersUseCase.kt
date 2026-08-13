package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class AddGroupMembersUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        contactIds: Set<String>
    ): Result<Unit> = repository.addGroupMembers(conversationId, contactIds)
}
