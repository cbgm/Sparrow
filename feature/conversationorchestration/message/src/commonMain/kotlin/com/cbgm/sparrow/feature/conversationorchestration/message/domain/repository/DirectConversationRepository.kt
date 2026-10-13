package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget

interface DirectConversationRepository {
    suspend fun getOrCreate(contactId: String): Result<String>

    /** Lookup only; never creates a conversation. */
    suspend fun findConversationId(contactId: String): Result<String?>

    suspend fun findContactId(conversationId: String): Result<String?>

    /** The existing direct conversation and its contact; fails for missing or group conversations. */
    suspend fun requireTarget(conversationId: String): Result<DirectConversationTarget>

    suspend fun delete(conversationId: String): Result<Unit>
}
