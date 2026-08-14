package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class RefreshGroupDeliveryStateUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.refreshDeliveryState(groupId)
}
