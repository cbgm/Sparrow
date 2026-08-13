package com.cbgm.securechat.feature.chats.domain.repository.overview

import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverview
import kotlinx.coroutines.flow.Flow

interface ConversationOverviewRepository {
    fun observeAll(): Flow<List<ConversationOverview>>
}
