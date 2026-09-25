package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository

class UnpinGroupMessageUseCase(
    private val repository: GroupPinRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.unpin(groupId)
}
