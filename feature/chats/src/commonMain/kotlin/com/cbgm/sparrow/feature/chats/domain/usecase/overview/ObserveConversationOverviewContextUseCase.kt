package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewContext
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveConversationOverviewContextUseCase(
    private val conversationRepository: ConversationOverviewRepository
) {
    operator fun invoke(): Flow<ConversationOverviewContext> =
        conversationRepository.observeAll().map { conversations ->
            ConversationOverviewContext(conversations = conversations)
        }
}
