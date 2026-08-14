package com.cbgm.securechat.feature.chats.domain.repository.group

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        text: String
    ): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun refreshDeliveryState(groupId: String): Result<Unit>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
