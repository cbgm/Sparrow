package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationProjectionRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class GroupConversationProjectionRepositoryImpl(
    private val conversationDataSource: GroupConversationDataSource
) : GroupConversationProjectionRepository {
    override suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext> =
        runCatching {
            val context = conversationDataSource.getContext(groupId)
            GroupMembershipContext(
                title = context.title,
                createdAtEpochMilliseconds = context.createdAtEpochMilliseconds
            )
        }

    override suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> = runCatching {
        conversationDataSource.stageIncoming(
            groupId = groupId,
            title = title,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )
    }

    override suspend fun showAcceptedIncoming(groupId: String): Result<Unit> =
        runCatching { conversationDataSource.showAcceptedIncoming(groupId) }

    override suspend fun discardPending(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        conversationDataSource.discardPending(groupId, updatedAtEpochMilliseconds)
    }

    override suspend fun addParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit> = runCatching {
        conversationDataSource.addParticipant(
            groupId = groupId,
            peerId = peerId,
            memberDisplayName = memberDisplayName,
            epoch = epoch,
            joinedAtEpochMilliseconds = joinedAtEpochMilliseconds,
            eventId = eventId
        )
    }

    override suspend fun promoteParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        conversationDataSource.promoteParticipant(groupId, peerId, updatedAtEpochMilliseconds)
    }

    override suspend fun removeParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit> = runCatching {
        conversationDataSource.removeParticipant(
            groupId = groupId,
            peerId = peerId,
            memberDisplayName = memberDisplayName,
            epoch = epoch,
            eventId = eventId,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
            memberLeft = memberLeft
        )
    }

    override suspend fun recordRemoteMemberAdded(
        groupId: String,
        peerId: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        conversationDataSource.recordRemoteMemberAdded(
            groupId = groupId,
            peerId = peerId,
            epoch = epoch,
            activationId = activationId,
            memberDisplayName = memberDisplayName,
            joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
        )
    }

    override suspend fun activateRemoteParticipant(
        groupId: String,
        peerId: String,
        role: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        conversationDataSource.activateRemoteParticipant(
            groupId = groupId,
            peerId = peerId,
            role = role,
            epoch = epoch,
            activationId = activationId,
            memberDisplayName = memberDisplayName,
            joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
        )
    }
}
