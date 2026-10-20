package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

interface ConversationUnseenCountRepository {
    suspend fun increment(conversationId: String): Result<Unit>

    suspend fun clear(conversationId: String): Result<Unit>
}
