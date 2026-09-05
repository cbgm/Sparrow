package com.cbgm.sparrow.feature.chats.domain.repository.direct

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import kotlinx.coroutines.flow.Flow

interface DirectConversationRepository {
    fun observe(
        conversationId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<DirectConversation?>

    suspend fun getOrCreate(contactId: String): Result<String>

    suspend fun findContactId(conversationId: String): Result<String?>

    suspend fun findConversationId(contactId: String): Result<String?>

    suspend fun delete(conversationId: String): Result<Unit>
}
