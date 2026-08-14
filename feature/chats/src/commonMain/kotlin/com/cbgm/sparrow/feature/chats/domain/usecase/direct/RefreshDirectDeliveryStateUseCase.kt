package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class RefreshDirectDeliveryStateUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(
        conversationId: String
    ): Result<Unit> =
        repository.refreshDeliveryState(conversationId)
}
