package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository

class MarkGroupConversationReadUseCase(
    private val repository: GroupMessageRepository,
    private val conversationOverviewRepository: ConversationOverviewRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        safeSuspendCall {
            conversationOverviewRepository
                .clearUnseenLocalMessageCount(groupId)
                .getOrThrow()
            repository.markConversationRead(groupId).getOrThrow()
        }
}
