package com.cbgm.sparrow.feature.chats.domain.repository.overview

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import kotlinx.coroutines.flow.Flow

interface ConversationOverviewRepository {
    fun observeAll(): Flow<List<ConversationOverview>>

    suspend fun incrementUnseenLocalMessageCount(conversationId: String): Result<Unit>

    suspend fun clearUnseenLocalMessageCount(conversationId: String): Result<Unit>
}
