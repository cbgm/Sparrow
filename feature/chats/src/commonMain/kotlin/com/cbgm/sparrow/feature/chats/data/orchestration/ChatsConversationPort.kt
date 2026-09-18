package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class ChatsConversationPort(
    private val conversationRepository: DirectConversationRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val activateAuthorizedDirectConversation: ActivateAuthorizedDirectConversationUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val groupConversationDataSource: GroupConversationDataSource
) : ConversationPort {
    override suspend fun getOrCreateConversation(peerId: String): Result<String> =
        conversationRepository.getOrCreate(peerId)

    override suspend fun activateAuthorizedConversation(peerId: String): Result<Unit> =
        activateAuthorizedDirectConversation(peerId)

    override suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit> =
        directMessageRepository.discardWaitingForAuthorization(peerId)

    override suspend fun runPendingAuthorizationCleanup() {
        pendingAuthorizationMessageCoordinator.run()
    }

    override suspend fun findPeerId(conversationId: String): Result<String?> =
        conversationRepository.findContactId(conversationId)

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        conversationRepository.delete(conversationId)

    override suspend fun getGroupTitle(groupId: String): Result<String> =
        getGroupMembershipContext(groupId).map { context -> context.title }

    override suspend fun getGroupMembershipContext(groupId: String): Result<GroupMembershipContext> =
        runCatching {
            val context = groupConversationDataSource.getContext(groupId)
            GroupMembershipContext(
                title = context.title,
                createdAtEpochMilliseconds = context.createdAtEpochMilliseconds
            )
        }

    override suspend fun stageIncomingGroupConversation(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        runCatching {
            groupConversationDataSource.stageIncoming(
                groupId = groupId,
                title = title,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        }

    override suspend fun discardPendingGroupConversation(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.discardPending(groupId, updatedAtEpochMilliseconds)
        }

    override suspend fun addGroupParticipant(
        groupId: String,
        peerId: String,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.addParticipant(
                groupId = groupId,
                peerId = peerId,
                joinedAtEpochMilliseconds = joinedAtEpochMilliseconds,
                eventId = eventId
            )
        }

    override suspend fun promoteGroupParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.promoteParticipant(
                groupId = groupId,
                peerId = peerId,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        }

    override suspend fun removeGroupParticipant(
        groupId: String,
        peerId: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.removeParticipant(
                groupId = groupId,
                peerId = peerId,
                epoch = epoch,
                eventId = eventId,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                memberLeft = memberLeft
            )
        }

    override suspend fun deleteGroupAttachments(groupId: String): Result<Unit> =
        messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId)
}
