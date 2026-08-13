package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository

class DeleteGroupConversationUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.delete(groupId)
}
