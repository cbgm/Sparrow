package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.datasource.GroupInvitationOwnerIdentityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource

internal class GroupInviteIncomingProcessorImpl(
    private val chatDao: ChatDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val ownerIdentityDataSource: GroupInvitationOwnerIdentityDataSource
) {
    suspend fun process(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        shouldStage: Boolean
    ): Result<Unit> =
        runCatching {
            membershipPacketProtocol.verifyInvite(packet).getOrThrow()
            if (shouldIgnore(packet)) return@runCatching
            if (!shouldStage) {
                acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }
            if (isExisting(ownerContactId, packet)) {
                acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }

            membershipAttempts.clearRetiredMembershipBeforeRejoin(packet.groupId).getOrThrow()
            val persistedAt = maxOf(packet.createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
            updateOwnerIdentity(ownerContactId, packet)
            store(ownerContactId, packet, persistedAt)
            acknowledge(ownerContactId, packet, receivedAtEpochMilliseconds)
        }

    private suspend fun updateOwnerIdentity(
        ownerContactId: String,
        packet: GroupInvitePacket
    ) {
        val identityChanged =
            ownerIdentityDataSource.stage(
                contactId = ownerContactId,
                encryptionPublicKey = packet.ownerEncryptionPublicKey,
                signingPublicKey = packet.ownerSigningPublicKey
            )
        if (!identityChanged) return

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
        val existingConversation = chatDao.findConversationById(packet.groupId)
        val hasHistory = chatDao.hasMessages(packet.groupId)
        chatDao.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt,
                unseenLocalMessageCount = existingConversation?.unseenLocalMessageCount ?: 0,
                isVisible = existingConversation?.isVisible == true && hasHistory
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
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
