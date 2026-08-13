package com.cbgm.securechat.feature.chats.domain.repository.group

import kotlinx.coroutines.flow.Flow

interface GroupTypingRepository {
    fun observeMember(contactId: String): Flow<Boolean>

    suspend fun sendToMembers(
        contactIds: Set<String>,
        isTyping: Boolean
    ): Result<Unit>
}
