package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

interface GroupConversationProjectionRepository {
    suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext>

    suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>

    suspend fun showAcceptedIncoming(groupId: String): Result<Unit>

    suspend fun discardPending(groupId: String, updatedAtEpochMilliseconds: Long): Result<Unit>

    suspend fun addParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit>

    suspend fun promoteParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun removeParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit>

    suspend fun recordRemoteMemberAdded(
        groupId: String,
        peerId: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun activateRemoteParticipant(
        groupId: String,
        peerId: String,
        role: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit>
}
