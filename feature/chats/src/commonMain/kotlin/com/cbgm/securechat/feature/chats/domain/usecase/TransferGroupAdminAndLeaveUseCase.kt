package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class TransferGroupAdminAndLeaveUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> = repository.transferGroupAdminAndLeave(groupId, contactId)
}
