package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.StageIncomingGroupOwnerIdentityUseCase

internal class GroupInviteIncomingProcessor(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val stageIncomingOwnerIdentity: StageIncomingGroupOwnerIdentityUseCase
) {
    suspend fun process(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            membershipPacketProtocol.verifyInvite(packet).getOrThrow()
            if (shouldIgnore(packet)) return@runCatching
            if (isExisting(ownerContactId, packet)) {
                acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }

            val replacedInvitation = findReplaceable(ownerContactId, packet)
            if (
                replacedInvitation != null &&
                !replacedInvitation.status.isTerminalStatus() &&
                packet.createdAtEpochMilliseconds <= replacedInvitation.createdAtEpochMilliseconds
            ) {
                acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }

            groupSecurityManager.clearRetiredMembershipBeforeRejoin(packet.groupId).getOrThrow()
            val persistedAt =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                )
            updateOwnerIdentity(ownerContactId, packet, persistedAt)
            store(ownerContactId, packet, persistedAt, replacedInvitation != null)
            acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
        }

    private suspend fun updateOwnerIdentity(
        ownerContactId: String,
        packet: GroupInvitePacket,
        persistedAt: Long
    ) {
        val identityChanged =
            stageIncomingOwnerIdentity(
                contactId = ownerContactId,
                encryptionPublicKey = packet.ownerEncryptionPublicKey,
                signingPublicKey = packet.ownerSigningPublicKey
            ).getOrThrow()
        if (!identityChanged) return

        groupInvitationDao.failSupersededIncomingInvitations(
            contactId = ownerContactId,
            currentInvitationId = packet.invitationId,
            awaitingAcceptanceStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
            failedStatus = GroupInvitationStatus.FAILED.name,
            updatedAt = persistedAt
        )
    }

    private suspend fun acknowledge(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ) {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val acknowledgement =
            membershipPacketProtocol
                .createInviteReceived(
                    invite = packet,
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                    memberSigningKeyPair = signingKeyPair
                ).getOrThrow()
        protocolOutbox.enqueue(ownerContactId, acknowledgement).getOrThrow()
    }

    private suspend fun shouldIgnore(packet: GroupInvitePacket): Boolean {
        val localDeletionTimestamp =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = packet.groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
            ) ?: return false

        if (packet.createdAtEpochMilliseconds <= localDeletionTimestamp) return true
        chatDao.deleteConversationMessages(packet.groupId)
        return false
    }

    private suspend fun isExisting(
        ownerContactId: String,
        packet: GroupInvitePacket
    ): Boolean {
        val existing = groupInvitationDao.findByInvitationId(packet.invitationId) ?: return false
        check(
            existing.groupId == packet.groupId &&
                existing.contactId == ownerContactId &&
                existing.challenge.contentEquals(packet.challenge)
        ) {
            "Group invitation conflicts with an existing invitation"
        }
        return true
    }

    private suspend fun findReplaceable(
        ownerContactId: String,
        packet: GroupInvitePacket
    ): GroupInvitationEntity? =
        groupInvitationDao.findByGroupContactAndDirection(
            groupId = packet.groupId,
            contactId = ownerContactId,
            direction = GroupInvitationDirection.INCOMING.name
        )

    private suspend fun store(
        ownerContactId: String,
        packet: GroupInvitePacket,
        persistedAt: Long,
        replacesExisting: Boolean
    ) {
        chatDao.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            )
        )
        val invitation =
            GroupInvitationEntity(
                invitationId = packet.invitationId,
                groupId = packet.groupId,
                contactId = ownerContactId,
                direction = GroupInvitationDirection.INCOMING.name,
                status = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                challenge = packet.challenge.copyOf(),
                ownerEncryptionPublicKey = packet.ownerEncryptionPublicKey.copyOf(),
                ownerSigningPublicKey = packet.ownerSigningPublicKey.copyOf(),
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            )
        if (replacesExisting) {
            groupInvitationDao.replaceForGroupAndContact(invitation)
        } else {
            groupInvitationDao.upsert(invitation)
        }
    }

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.EXPIRED.name ||
            this == GroupInvitationStatus.FAILED.name

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
