package com.cbgm.sparrow.feature.chats.data.orchestration

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationProjectionRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIncomingConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupLocalConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTitleRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

/**
 * Chats adapter for cross-feature conversation orchestration.
 *
 * This boundary depends on domain repositories only. Chats data sources,
 * processors, broadcasters and runtime coordinators stay behind repositories.
 */
internal class ChatsConversationPort(
    private val directConversationRepository: DirectConversationRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val groupConversationRepository: GroupConversationRepository,
    private val groupConversationProjectionRepository: GroupConversationProjectionRepository,
    private val groupIncomingConversationRepository: GroupIncomingConversationRepository,
    private val groupLocalConversationRepository: GroupLocalConversationRepository,
    private val groupMessageRepository: GroupMessageRepository,
    private val groupVerificationActionRepository: GroupVerificationActionRepository,
    private val groupAvatarRepository: GroupAvatarRepository,
    private val groupTitleRepository: GroupTitleRepository,
    private val groupDescriptionRepository: GroupDescriptionRepository,
    private val groupPinRepository: GroupPinRepository
) : ConversationPort {
    private val logger = SparrowLog.withTag("ChatsConversationPort")

    override suspend fun createOwnedGroupConversation(title: String): Result<String> =
        groupConversationRepository.create(title)

    override suspend fun initializeOwnedGroupVerification(groupId: String): Result<Unit> =
        groupVerificationActionRepository.initializeOwnedGroup(groupId)

    override suspend fun flushPendingGroupMessages(groupId: String): Result<Unit> =
        groupMessageRepository.flushQueued(groupId)

    override suspend fun recordIncomingGroupWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit> = groupIncomingConversationRepository.recordWelcomeRestart(
        packet = packet,
        invitationId = invitationId,
        isFirstWelcome = isFirstWelcome,
        persistedAt = persistedAt
    )

    override suspend fun getCurrentGroupParticipantIds(groupId: String): Result<List<String>> =
        groupIncomingConversationRepository.getParticipantIds(groupId)

    override suspend fun installIncomingGroupWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>> = groupIncomingConversationRepository.installWelcome(
        packet = packet,
        previousSigningKeysByContactId = previousSigningKeysByContactId,
        contactIdsByMember = contactIdsByMember,
        contactDisplayNames = contactDisplayNames,
        persistedAt = persistedAt
    )

    override suspend fun sendCurrentGroupMetadataTo(groupId: String, peerId: String) {
        groupAvatarRepository.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group avatar for $peerId" } }
        groupTitleRepository.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group title for $peerId" } }
        groupDescriptionRepository.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group description for $peerId" } }
        groupPinRepository.sendCurrentTo(groupId, peerId)
            .onFailure { error -> logger.warn(error) { "Could not queue current group pin for $peerId" } }
    }

    override suspend fun refreshOwnedGroupVerification(groupId: String): Result<Unit> =
        groupVerificationActionRepository.refreshOwnedGroup(groupId)

    override suspend fun onLocalGroupMemberActivated(groupId: String): Result<Unit> =
        groupVerificationActionRepository.synchronize(groupId)

    override suspend fun recordRemoteGroupMemberAdded(
        groupId: String,
        contactId: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit> = groupConversationProjectionRepository.recordRemoteMemberAdded(
        groupId = groupId,
        peerId = contactId,
        epoch = epoch,
        activationId = activationId,
        memberDisplayName = memberDisplayName,
        joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
    )

    override suspend fun onRemoteGroupMemberActivated(
        groupId: String,
        contactId: String,
        role: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit> = groupConversationProjectionRepository.activateRemoteParticipant(
        groupId = groupId,
        peerId = contactId,
        role = role,
        epoch = epoch,
        activationId = activationId,
        memberDisplayName = memberDisplayName,
        joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
    )

    override suspend fun applyIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String
    ): Result<Unit> = groupIncomingConversationRepository.applyRemoval(packet)

    override suspend fun prepareIncomingGroupDeletion(groupId: String): Result<Unit> =
        runCatching {
            messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId).getOrThrow()
            groupIncomingConversationRepository.prepareDeletion(groupId).getOrThrow()
        }

    override suspend fun finishIncomingGroupDeletion(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = groupIncomingConversationRepository.finishDeletion(
        groupId = groupId,
        deletedAtEpochMilliseconds = deletedAtEpochMilliseconds
    )

    override suspend fun getOrCreateConversation(peerId: String): Result<String> =
        directConversationRepository.getOrCreate(peerId)

    override suspend fun findConversationId(peerId: String): Result<String?> =
        directConversationRepository.findConversationId(peerId)

    override suspend fun activateAuthorizedConversation(peerId: String): Result<Unit> =
        runCatching {
            directConversationRepository.getOrCreate(peerId).getOrThrow()
            directMessageRepository.releaseWaitingForAuthorization(peerId).getOrThrow()
        }

    override suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit> =
        directMessageRepository.discardWaitingForAuthorization(peerId)

    override suspend fun runPendingAuthorizationCleanup() {
        directMessageRepository.runPendingAuthorizationCleanup()
    }

    override suspend fun findPeerId(conversationId: String): Result<String?> =
        directConversationRepository.findContactId(conversationId)

    override suspend fun findGroupIdForMessage(messageId: String): Result<String?> =
        groupMessageRepository.findGroupIdForMessage(messageId)

    override suspend fun deleteConversation(conversationId: String): Result<Unit> =
        directConversationRepository.delete(conversationId)

    override suspend fun getGroupTitle(groupId: String): Result<String> =
        getGroupMembershipContext(groupId).map(GroupMembershipContext::title)

    override suspend fun getGroupMembershipContext(groupId: String): Result<GroupMembershipContext> =
        groupConversationProjectionRepository.getMembershipContext(groupId)

    override suspend fun stageIncomingGroupConversation(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> = groupConversationProjectionRepository.stageIncoming(
        groupId = groupId,
        title = title,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    override suspend fun showAcceptedIncomingGroupConversation(groupId: String): Result<Unit> =
        groupConversationProjectionRepository.showAcceptedIncoming(groupId)

    override suspend fun discardPendingGroupConversation(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = groupConversationProjectionRepository.discardPending(
        groupId = groupId,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    override suspend fun addGroupParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit> = groupConversationProjectionRepository.addParticipant(
        groupId = groupId,
        peerId = peerId,
        memberDisplayName = memberDisplayName,
        epoch = epoch,
        joinedAtEpochMilliseconds = joinedAtEpochMilliseconds,
        eventId = eventId
    )

    override suspend fun promoteGroupParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = groupConversationProjectionRepository.promoteParticipant(
        groupId = groupId,
        peerId = peerId,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    override suspend fun removeGroupParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit> = groupConversationProjectionRepository.removeParticipant(
        groupId = groupId,
        peerId = peerId,
        memberDisplayName = memberDisplayName,
        epoch = epoch,
        eventId = eventId,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
        memberLeft = memberLeft
    )

    override suspend fun endLocalGroupMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): Result<Unit> = groupLocalConversationRepository.endMembership(
        groupId = groupId,
        referenceId = referenceId,
        epoch = epoch,
        endedAtEpochMilliseconds = endedAtEpochMilliseconds
    )

    override suspend fun deleteLocalGroupConversation(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = groupLocalConversationRepository.deleteConversation(
        groupId = groupId,
        deletedAtEpochMilliseconds = deletedAtEpochMilliseconds
    )

    override suspend fun deleteGroupAttachments(groupId: String): Result<Unit> =
        messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId)
}
