package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class DeleteGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<Unit> =
        repository.deleteMessage(groupId, messageId)
}
