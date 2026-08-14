package com.cbgm.sparrow.feature.chats.domain.repository.direct

import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import kotlinx.coroutines.flow.Flow

interface DirectConversationRepository {
    fun observe(conversationId: String): Flow<DirectConversation?>

    suspend fun getOrCreate(contactId: String): String

    suspend fun delete(conversationId: String): Result<Unit>
}
