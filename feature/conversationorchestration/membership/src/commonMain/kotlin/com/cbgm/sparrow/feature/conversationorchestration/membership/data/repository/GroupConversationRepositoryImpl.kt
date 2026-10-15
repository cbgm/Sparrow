package com.cbgm.sparrow.feature.conversationorchestration.membership.data.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupConversationEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupMembershipContext
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantActivation
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class GroupConversationRepositoryImpl(
    private val conversationDataSource: GroupConversationDataSource,
    private val cleanupDataSource: GroupLocalCleanupDataSource
) : GroupConversationRepository {
    override suspend fun create(title: String): Result<String> =
        safeSuspendCall {
            val groupId = IdGenerator.generate(prefix = "group")
            conversationDataSource.create(
                conversation = toGroupConversationEntity(
                    groupId = groupId,
                    createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ),
                title = title
            )
            groupId
        }

    override suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext> =
        safeSuspendCall {
            conversationDataSource.getGroupConversation(groupId)
                .toGroupMembershipContext(conversationDataSource.getTitle(groupId))
        }

    override suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        safeSuspendCall {
            conversationDataSource.stageIncoming(
                groupId = groupId,
                title = title,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        }

    override suspend fun showAcceptedIncoming(groupId: String): Result<Unit> =
        safeSuspendCall { conversationDataSource.showAcceptedIncoming(groupId) }

    override suspend fun discardPending(groupId: String, updatedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall { conversationDataSource.discardPending(groupId, updatedAtEpochMilliseconds) }

    override suspend fun addParticipant(addition: GroupParticipantAddition): Result<Unit> =
        safeSuspendCall {
            conversationDataSource.addParticipant(
                groupId = addition.groupId,
                peerId = addition.contactId,
                memberDisplayName = addition.memberDisplayName,
                epoch = addition.epoch,
                joinedAtEpochMilliseconds = addition.joinedAtEpochMilliseconds,
                eventId = addition.eventId
            )
        }

    override suspend fun promoteParticipant(
        groupId: String,
        contactId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall { conversationDataSource.promoteParticipant(groupId, contactId, updatedAtEpochMilliseconds) }

    override suspend fun removeParticipant(removal: GroupParticipantRemoval): Result<Unit> =
        safeSuspendCall {
            conversationDataSource.removeParticipant(
                groupId = removal.result.groupId,
                peerId = removal.result.contactId,
                memberDisplayName = removal.memberDisplayName,
                epoch = removal.result.epoch,
                eventId = removal.result.eventId,
                updatedAtEpochMilliseconds = removal.result.updatedAtEpochMilliseconds,
                memberLeft = removal.memberLeft
            )
        }

    override suspend fun recordRemoteMemberAdded(activation: GroupParticipantActivation): Result<Unit> =
        safeSuspendCall {
            conversationDataSource.recordRemoteMemberAdded(
                groupId = activation.groupId,
                peerId = activation.contactId,
                epoch = activation.epoch,
                activationId = activation.activationId,
                memberDisplayName = activation.memberDisplayName,
                joinedAtEpochMilliseconds = activation.joinedAtEpochMilliseconds
            )
        }

    override suspend fun activateRemoteParticipant(activation: GroupParticipantActivation): Result<Unit> =
        safeSuspendCall {
            conversationDataSource.activateRemoteParticipant(
                groupId = activation.groupId,
                peerId = activation.contactId,
                role = activation.role,
                epoch = activation.epoch,
                activationId = activation.activationId,
                memberDisplayName = activation.memberDisplayName,
                joinedAtEpochMilliseconds = activation.joinedAtEpochMilliseconds
            )
        }

    override suspend fun endLocalMembership(end: GroupLocalMembershipEnd): Result<Unit> =
        safeSuspendCall {
            cleanupDataSource.endMembership(end.groupId, end.referenceId, end.epoch, end.endedAtEpochMilliseconds)
        }

    override suspend fun deleteLocalConversation(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall { cleanupDataSource.deleteConversationHistory(groupId, deletedAtEpochMilliseconds) }
}
