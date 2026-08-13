package com.cbgm.securechat.feature.chats.domain.repository.direct

import kotlinx.coroutines.flow.Flow

interface DirectTypingRepository {
    fun observe(contactId: String): Flow<Boolean>

    suspend fun send(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit>
}
