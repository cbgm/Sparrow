package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        parts: List<MessagePart>,
        replyToMessageId: String? = null,
        access: GroupMessageMembershipAccess
    ): Result<String>

    suspend fun toggleReaction(groupId: String, messageId: String, emoji: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun votePoll(
        groupId: String,
        messageId: String,
        pollId: String,
        selectedOptionIds: Set<String>,
        access: GroupMessageMembershipAccess
    ): Result<Unit>

    suspend fun closePoll(
        groupId: String,
        messageId: String,
        pollId: String,
        closedAtEpochMilliseconds: Long,
        access: GroupMessageMembershipAccess
    ): Result<Unit>

    suspend fun deleteMessage(groupId: String, messageId: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun editMessage(groupId: String, messageId: String, text: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun flushQueued(groupId: String): Result<Unit>

    suspend fun findGroupIdForMessage(messageId: String): Result<String?>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
