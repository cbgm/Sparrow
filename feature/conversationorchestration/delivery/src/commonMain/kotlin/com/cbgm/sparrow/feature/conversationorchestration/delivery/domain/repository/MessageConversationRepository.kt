package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository

interface MessageConversationRepository {
    /** The group that owns [messageId], or null for direct or unknown messages. */
    suspend fun findGroupIdForMessage(messageId: String): Result<String?>
}
