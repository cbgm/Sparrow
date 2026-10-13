package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.ConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository

internal class ConversationUnseenCountRepositoryImpl(
    private val dataSource: ConversationDataSource
) : ConversationUnseenCountRepository {
    override suspend fun increment(conversationId: String): Result<Unit> =
        safeSuspendCall {
            check(dataSource.incrementUnseenLocalMessageCount(conversationId) == 1) { "Conversation not found" }
        }

    override suspend fun clear(conversationId: String): Result<Unit> =
        safeSuspendCall {
            check(dataSource.clearUnseenLocalMessageCount(conversationId) == 1) { "Conversation not found" }
        }
}
