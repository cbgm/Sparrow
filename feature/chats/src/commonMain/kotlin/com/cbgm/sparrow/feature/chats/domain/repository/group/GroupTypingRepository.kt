package com.cbgm.sparrow.feature.chats.domain.repository.group

import kotlinx.coroutines.flow.Flow

interface GroupTypingRepository {
    fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<Boolean>

    suspend fun setTyping(
        groupId: String,
        isTyping: Boolean
    ): Result<Unit>
}
