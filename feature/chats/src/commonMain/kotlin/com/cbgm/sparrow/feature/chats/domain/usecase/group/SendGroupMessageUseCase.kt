package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class SendGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(
        groupId: String,
        text: String
    ): Result<Unit> =
        repository.send(groupId, text)
}
