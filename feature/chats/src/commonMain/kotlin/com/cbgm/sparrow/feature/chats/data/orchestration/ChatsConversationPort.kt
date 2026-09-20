package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupCreatedIncomingProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupWelcomePersistence
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitleBroadcaster
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class ChatsConversationPort(
    private val conversationRepository: DirectConversationRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val activateAuthorizedDirectConversation: ActivateAuthorizedDirectConversationUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val groupConversationDataSource: GroupConversationDataSource,
    private val groupLocalCleanupDataSource: GroupLocalCleanupDataSource,
    private val verificationCoordinator: GroupVerificationCoordinator,
    private val incomingConversationDataSource: GroupIncomingConversationDataSource,
    private val createdIncomingProcessor: GroupCreatedIncomingProcessor,
    private val welcomePersistence: GroupWelcomePersistence,
    private val groupAvatarBroadcaster: GroupAvatarBroadcaster,
    private val groupTitleBroadcaster: GroupTitleBroadcaster,
    private val groupDescriptionBroadcaster: GroupDescriptionBroadcaster,
    private val groupPinBroadcaster: GroupPinBroadcaster
) : ConversationPort {
    private val logger = SparrowLog.withTag("ChatsConversationPort")

    override suspend fun recordIncomingGroupWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit> = runCatching {
        welcomePersistence.recordMembershipRestartIfNeeded(
            packet,
            invitationId,
            isFirstWelcome,
            persistedAt
        )
    }

    override suspend fun getCurrentGroupParticipantIds(groupId: String): Result<List<String>> =
        runCatching {
            incomingConversationDataSource.findConversationParticipants(groupId)
                .map { it.contactId }
        }

    override suspend fun installIncomingGroupWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>> = createdIncomingProcessor.process(
        packet,
        previousSigningKeysByContactId,
        contactIdsByMember,
        contactDisplayNames,
        persistedAt
    )

    override suspend fun sendCurrentGroupMetadataTo(groupId: String, peerId: String) {
        groupAvatarBroadcaster.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group avatar for $peerId" } }
        groupTitleBroadcaster.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group title for $peerId" } }
        groupDescriptionBroadcaster.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group description for $peerId" } }
        groupPinBroadcaster.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group pin for $peerId" } }
    }

    override suspend fun applyIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String
    ): Result<Unit> = runCatching {
        val wasLocallyHidden = incomingConversationDataSource.hasMessageWithTransportMode(
            conversationId = packet.groupId,
            transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
        )
        if (!wasLocallyHidden) {
            val message =
                if (packet.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                    GroupMembershipMessageFactory.localMembershipLeft(
                        conversationId = packet.groupId,
                        invitationId = packet.invitationId,
                        epoch = packet.epoch,
                        createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                    )
                } else {
                    GroupMembershipMessageFactory.localMembershipRemoved(
                        conversationId = packet.groupId,
                        invitationId = packet.invitationId,
                        epoch = packet.epoch,
                        createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                    )
                }
            incomingConversationDataSource.applyLocalGroupRemoval(message)
        }
        incomingConversationDataSource.deleteVerificationRows(packet.groupId)
    }

    override suspend fun prepareIncomingGroupDeletion(groupId: String): Result<Unit> = runCatching {
        messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId).getOrThrow()
        incomingConversationDataSource.deleteConversationParticipants(groupId)
        incomingConversationDataSource.deleteVerificationRows(groupId)
    }

    override suspend fun finishIncomingGroupDeletion(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        incomingConversationDataSource.updateConversationTimestamp(
            conversationId = groupId,
            timestamp = deletedAtEpochMilliseconds
        )
    }

    override suspend fun onLocalGroupMemberActivated(groupId: String): Result<Unit> =
        verificationCoordinator.synchronize(groupId)

    override suspend fun onRemoteGroupMemberActivated(
        groupId: String,
        contactId: String,
        role: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        incomingConversationDataSource.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = groupId,
                contactId = contactId,
                role = role,
                joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
            )
        )
    }

    override suspend fun refreshOwnedGroupVerification(groupId: String): Result<Unit> =
        verificationCoordinator.onOwnedMembershipChanged(groupId)

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

    override suspend fun showAcceptedIncomingGroupConversation(groupId: String): Result<Unit> =
        runCatching { groupConversationDataSource.showAcceptedIncoming(groupId) }

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
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.addParticipant(
                groupId = groupId,
                peerId = peerId,
                memberDisplayName = memberDisplayName,
                epoch = epoch,
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
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit> =
        runCatching {
            groupConversationDataSource.removeParticipant(
                groupId = groupId,
                peerId = peerId,
                memberDisplayName = memberDisplayName,
                epoch = epoch,
                eventId = eventId,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                memberLeft = memberLeft
            )
        }

    override suspend fun endLocalGroupMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        groupLocalCleanupDataSource.endMembership(groupId, referenceId, epoch, endedAtEpochMilliseconds)
    }

    override suspend fun deleteLocalGroupConversation(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        groupLocalCleanupDataSource.deleteConversationHistory(groupId, deletedAtEpochMilliseconds)
    }

    override suspend fun deleteGroupAttachments(groupId: String): Result<Unit> =
        messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId)
}
