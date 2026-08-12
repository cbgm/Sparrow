package com.cbgm.securechat.feature.chats.domain.repository

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.model.GroupConversation
import com.cbgm.securechat.feature.chats.domain.model.GroupLeaveRequirement
import kotlinx.coroutines.flow.Flow

interface ChatsRepository {
    fun observeConversations(): Flow<List<Conversation>>

    fun observeConversation(conversationId: String): Flow<Conversation?>

    suspend fun getOrCreateDirectConversation(contactId: String): String

    suspend fun createGroupConversation(
        title: String,
        contactIds: Set<String>
    ): String

    suspend fun addGroupMembers(
        conversationId: String,
        contactIds: Set<String>
    ): Result<Unit>

    suspend fun removeGroupMember(
        conversationId: String,
        contactId: String
    ): Result<Unit>

    suspend fun promoteGroupMember(
        conversationId: String,
        contactId: String
    ): Result<Unit>

    suspend fun transferGroupAdminAndLeave(
        conversationId: String,
        contactId: String
    ): Result<Unit>

    fun observeGroupAdministration(conversationId: String): Flow<GroupAdministrationState>

    suspend fun getGroupLeaveRequirement(conversationId: String): Result<GroupLeaveRequirement>

    suspend fun leaveGroup(conversationId: String): Result<Unit>

    suspend fun deleteConversation(conversationId: String): Result<Unit>

    fun observeGroupConversation(conversationId: String): Flow<GroupConversation?>

    suspend fun acceptGroupInvitation(conversationId: String): Result<Unit>

    suspend fun declineGroupInvitation(conversationId: String): Result<Unit>

    suspend fun sendGroupMessage(
        conversationId: String,
        text: String
    ): Result<Unit>

    suspend fun sendMessage(
        conversationId: String,
        text: String
    )

    suspend fun retryMessage(messageId: String): Result<Unit>

    suspend fun refreshDeliveryState(conversationId: String): Result<Unit>

    suspend fun markConversationRead(conversationId: String): Result<Unit>
}
