package com.cbgm.sparrow.feature.conversationorchestration.domain.port

import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

interface ConversationPort {
    suspend fun getOrCreateConversation(peerId: String): Result<String>

    suspend fun activateAuthorizedConversation(peerId: String): Result<Unit>

    suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit>

    suspend fun runPendingAuthorizationCleanup()

    suspend fun findPeerId(conversationId: String): Result<String?>

    suspend fun deleteConversation(conversationId: String): Result<Unit>

    suspend fun getGroupTitle(groupId: String): Result<String>

    suspend fun getGroupMembershipContext(groupId: String): Result<GroupMembershipContext>

    suspend fun stageIncomingGroupConversation(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>

    suspend fun discardPendingGroupConversation(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun addGroupParticipant(
        groupId: String,
        peerId: String,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit>

    suspend fun promoteGroupParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun removeGroupParticipant(
        groupId: String,
        peerId: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit>

    suspend fun deleteGroupAttachments(groupId: String): Result<Unit>
}
