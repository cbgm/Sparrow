package com.cbgm.sparrow.feature.conversationorchestration.delivery.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.feature.conversationorchestration.delivery.data.datasource.MessageConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.MessageConversationRepository

internal class MessageConversationRepositoryImpl(
    private val dataSource: MessageConversationDataSource
) : MessageConversationRepository {
    override suspend fun findGroupIdForMessage(messageId: String): Result<String?> =
        safeSuspendCall {
            val message = dataSource.findMessage(messageId) ?: return@safeSuspendCall null
            val conversation = dataSource.findConversation(message.conversationId) ?: return@safeSuspendCall null
            conversation.id.takeIf { conversation.type == ConversationType.GROUP.name }
        }
}
