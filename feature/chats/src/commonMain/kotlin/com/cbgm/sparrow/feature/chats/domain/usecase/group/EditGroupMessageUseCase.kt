package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class EditGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String, messageId: String, text: String): Result<Unit> =
        repository.editMessage(groupId, messageId, text)
}
