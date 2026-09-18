package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.InvitationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.InvitationEntity
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.domain.usecase.StageGroupOwnerIdentityUseCase

internal class GroupInviteIncomingProcessorImpl(
    private val chatDao: ChatDao,
    private val invitationDao: InvitationDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val stageIncomingOwnerIdentity: StageGroupOwnerIdentityUseCase
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

            val latestInvitation =
                invitationDao.findLatest(
                    payloadType = INVITATION_PAYLOAD_TYPE_GROUP,
                    payloadId = packet.groupId,
                    peerId = ownerContactId,
                    direction = INVITATION_DIRECTION_INCOMING
                )
            if (
                latestInvitation != null &&
                packet.createdAtEpochMilliseconds <= latestInvitation.createdAtEpochMilliseconds
            ) {
                acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }

            membershipAttempts.clearRetiredMembershipBeforeRejoin(packet.groupId).getOrThrow()
            val persistedAt = maxOf(packet.createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
            updateOwnerIdentity(ownerContactId, packet, persistedAt)
            store(ownerContactId, packet, persistedAt)
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

        invitationDao.failSuperseded(
            payloadType = INVITATION_PAYLOAD_TYPE_GROUP,
            payloadId = packet.groupId,
            peerId = ownerContactId,
            currentInvitationId = packet.invitationId,
            direction = INVITATION_DIRECTION_INCOMING,
            pendingStatus = INVITATION_STATUS_PENDING,
            failedStatus = INVITATION_STATUS_FAILED,
            updatedAt = persistedAt
        )
        membershipAttempts
            .deleteSupersededMemberStagedAttempts(
                ownerContactId = ownerContactId,
                currentInvitationId = packet.invitationId
            ).getOrThrow()
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
                transportMode = GroupMembershipMessageDataSource.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
            ) ?: return false

        if (packet.createdAtEpochMilliseconds <= localDeletionTimestamp) return true
        chatDao.deleteConversationMessages(packet.groupId)
        return false
    }

    private suspend fun isExisting(
        ownerContactId: String,
        packet: GroupInvitePacket
    ): Boolean {
        val existing = membershipAttempts.findBySourceInvitationId(packet.invitationId) ?: return false
        check(
            existing.groupId == packet.groupId &&
                existing.contactId == ownerContactId &&
                existing.challenge.contentEquals(packet.challenge)
        ) {
            "Group invitation conflicts with an existing membership attempt"
        }
        return true
    }

    private suspend fun store(
        ownerContactId: String,
        packet: GroupInvitePacket,
        persistedAt: Long
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
        membershipAttempts
            .stageMemberAttempt(
                groupId = packet.groupId,
                ownerContactId = ownerContactId,
                sourceInvitationId = packet.invitationId,
                challenge = packet.challenge,
                ownerEncryptionPublicKey = packet.ownerEncryptionPublicKey,
                ownerSigningPublicKey = packet.ownerSigningPublicKey,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            ).getOrThrow()
        val invitation =
            InvitationEntity(
                invitationId = packet.invitationId,
                payloadType = INVITATION_PAYLOAD_TYPE_GROUP,
                payloadId = packet.groupId,
                peerId = ownerContactId,
                direction = INVITATION_DIRECTION_INCOMING,
                status = INVITATION_STATUS_PENDING,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            )

        invitationDao.deleteByPayloadPeerAndDirection(
            payloadType = INVITATION_PAYLOAD_TYPE_GROUP,
            payloadId = packet.groupId,
            peerId = ownerContactId,
            direction = INVITATION_DIRECTION_INCOMING
        )
        invitationDao.upsert(invitation)
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val INVITATION_PAYLOAD_TYPE_GROUP = "GROUP"
        const val INVITATION_DIRECTION_INCOMING = "INCOMING"
        const val INVITATION_STATUS_PENDING = "PENDING"
        const val INVITATION_STATUS_FAILED = "FAILED"
    }
}
