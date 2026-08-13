package com.cbgm.securechat.feature.chats.domain.usecase.overview

import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.securechat.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationOverviewsUseCase(
    private val repository: ConversationOverviewRepository
) {
    operator fun invoke(): Flow<List<ConversationOverview>> = repository.observeAll()
}
