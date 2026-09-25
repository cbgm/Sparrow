package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess

interface GroupMessageRepository {
    suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null,
        access: GroupMessageMembershipAccess
    ): Result<Unit>

    suspend fun toggleReaction(groupId: String, messageId: String, emoji: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun deleteMessage(groupId: String, messageId: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun editMessage(groupId: String, messageId: String, text: String, access: GroupMessageMembershipAccess): Result<Unit>

    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markConversationRead(groupId: String): Result<Unit>
}
