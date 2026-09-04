package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository

class MarkDirectConversationReadUseCase(
    private val repository: DirectMessageRepository,
    private val conversationOverviewRepository: ConversationOverviewRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        safeSuspendCall {
            conversationOverviewRepository
                .clearUnseenLocalMessageCount(conversationId)
                .getOrThrow()
            repository.markConversationRead(conversationId).getOrThrow()
        }
}
