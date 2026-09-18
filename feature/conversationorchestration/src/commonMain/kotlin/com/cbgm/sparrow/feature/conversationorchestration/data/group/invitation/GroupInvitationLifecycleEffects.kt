package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleEffects
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipAttemptDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

@Suppress("LongParameterList")
internal class GroupInvitationLifecycleEffects(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val membershipMessageDataSource: GroupMembershipMessageDataSource
) : InvitationLifecycleEffects {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.GROUP

    override suspend fun send(
        payloadId: String,
        peerId: String
    ): Result<InvitationLifecycleRecord?> =
        runCatching {
            require(payloadId.isNotBlank()) { "Group ID must not be blank" }
            require(peerId.isNotBlank()) { "Contact ID must not be blank" }

            val conversation =
                chatDao.findConversationById(payloadId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
            val title = requireNotNull(conversation.title).trim()
            check(title.isNotEmpty()) { "Group title must not be blank" }
            check(contactDao.findById(peerId) != null) { "Contact was not found: $peerId" }

            val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val record =
                resendPendingIfPossible(payloadId, peerId, now)
                    ?: createAndQueueOutgoingInvitation(
                        groupId = payloadId,
                        title = title,
                        contactId = peerId,
                        ownerIdentity = ownerIdentity,
                        ownerSigningKeyPair = ownerSigningKeyPair,
                        createdAt = now
                    )
            membershipAttempts.refreshOwnedMembership(payloadId).getOrThrow()
            record
        }

    override suspend fun accept(invitationId: String): Result<Unit> =
        runCatching {
            val membership = requireIncomingMembership(invitationId)
            check(membership.status == GroupMembershipStatus.STAGED) {
                "Group membership cannot be accepted from status ${membership.status}"
            }

            prepareOwnerIdentity(membership)

            val memberIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val joinRequest =
                membershipPacketProtocol
                    .createJoinRequest(
                        invitationId = membership.sourceInvitationId,
                        groupId = membership.groupId,
                        challenge = membership.challenge,
                        memberIdentity = memberIdentity,
                        memberSigningKeyPair = memberSigningKeyPair
                    ).getOrThrow()

            membershipAttempts
                .markJoinRequested(
                    sourceInvitationId = invitationId,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ).getOrThrow()

            protocolOutbox.enqueue(membership.contactId, joinRequest).getOrElse { error ->
                membershipAttempts
                    .markJoinSendFailed(
                        sourceInvitationId = invitationId,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    ).getOrThrow()
                throw error
            }
        }

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        runCatching {
            require(action == null) { "Group invitations do not support decline-and-block" }
            val membership = requireIncomingMembership(invitationId)
            check(membership.status == GroupMembershipStatus.STAGED) {
                "Group membership cannot be declined from status ${membership.status}"
            }

            val hasHistory = chatDao.hasMessages(membership.groupId)
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                membershipPacketProtocol
                    .createDecline(
                        invitationId = membership.sourceInvitationId,
                        groupId = membership.groupId,
                        challenge = membership.challenge,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            protocolOutbox.enqueue(membership.contactId, packet).getOrThrow()
            val updatedAt = maxOf(membership.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
            membershipAttempts.deleteAttempt(invitationId).getOrThrow()

            if (!hasHistory) {
                chatDao.hideGroupConversation(
                    membershipMessageDataSource.localConversationDeletedMarker(
                        conversationId = membership.groupId,
                        createdAtEpochMilliseconds = updatedAt
                    )
                )
            }
        }

    override suspend fun onExpired(invitationId: String): Result<Unit> =
        clearMembershipAttempt(invitationId)

    override suspend fun onTransportFailed(invitationId: String): Result<Unit> =
        clearMembershipAttempt(invitationId)

    override suspend fun onDeleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        clearMembershipAttempt(invitationId)

    private suspend fun resendPendingIfPossible(
        groupId: String,
        contactId: String,
        now: Long
    ): InvitationLifecycleRecord? {
        val membership = membershipAttempts.findOwnerAttempt(groupId, contactId) ?: return null
        if (membership.status != GroupMembershipStatus.STAGED) return null

        val packetId = INVITE_PACKET_ID_PREFIX + membership.sourceInvitationId
        val queuedPacket = protocolOutbox.findByPacketId(packetId).getOrThrow()
        val expiresAt =
            queuedPacket?.expiresAtEpochMilliseconds
                ?: membership.createdAtEpochMilliseconds + INVITATION_VALIDITY_MILLISECONDS
        if (queuedPacket == null || now > expiresAt) {
            membershipAttempts.deleteAttempt(membership.sourceInvitationId).getOrThrow()
            return null
        }

        protocolOutbox.resend(packetId).getOrThrow()
        return membership.toOutgoingLifecycleRecord(expiresAt)
    }

    private suspend fun createAndQueueOutgoingInvitation(
        groupId: String,
        title: String,
        contactId: String,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair,
        createdAt: Long
    ): InvitationLifecycleRecord {
        val invitationId = IdGenerator.generate(prefix = "group-invitation")
        val expiresAt = createdAt + INVITATION_VALIDITY_MILLISECONDS
        val packet =
            membershipPacketProtocol
                .createInvite(
                    invitationId = invitationId,
                    groupId = groupId,
                    title = title,
                    createdAtEpochMilliseconds = createdAt,
                    expiresAtEpochMilliseconds = expiresAt,
                    ownerIdentity = ownerIdentity,
                    ownerSigningKeyPair = ownerSigningKeyPair
                ).getOrThrow()

        val membership =
            membershipAttempts
                .stageOwnerAttempt(
                    groupId = groupId,
                    contactId = contactId,
                    sourceInvitationId = invitationId,
                    challenge = packet.challenge,
                    createdAtEpochMilliseconds = createdAt
                ).getOrThrow()

        protocolOutbox.enqueue(contactId, packet).getOrElse { error ->
            membershipAttempts.deleteAttempt(invitationId).getOrThrow()
            throw error
        }

        return membership.toOutgoingLifecycleRecord(expiresAt)
    }

    private suspend fun requireIncomingMembership(invitationId: String): GroupMembershipAttemptDto {
        val membership =
            requireNotNull(membershipAttempts.findBySourceInvitationId(invitationId)) {
                "Group membership attempt was not found"
            }
        check(membership.perspective == GroupMembershipPerspective.MEMBER) {
            "Only incoming group memberships can be accepted or declined"
        }
        return membership
    }

    private suspend fun prepareOwnerIdentity(membership: GroupMembershipAttemptDto) {
        val existing = contactDao.findPublicIdentityByContactId(membership.contactId)
        val encryptionPublicKey =
            membership.ownerEncryptionPublicKey
                ?: existing?.encryptionPublicKey
                ?: error("Group owner encryption identity was not stored")
        val signingPublicKey =
            membership.ownerSigningPublicKey
                ?: existing?.signingPublicKey
                ?: error("Group owner signing identity was not stored")
        when {
            existing == null ||
                !existing.encryptionPublicKey.contentEquals(encryptionPublicKey) ||
                !existing.signingPublicKey.contentEquals(signingPublicKey) ->
                contactKeyExchangeDataSource.prepareRemoteIdentityForHandshake(
                    contactId = membership.contactId,
                    remoteEncryptionPublicKey = encryptionPublicKey,
                    remoteSigningPublicKey = signingPublicKey
                )

            existing.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name ->
                contactKeyExchangeDataSource.acceptRemoteIdentityForHandshake(
                    contactId = membership.contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                )
        }

        val acceptedIdentity =
            requireNotNull(contactDao.findPublicIdentityByContactId(membership.contactId)) {
                "Group owner identity was not stored"
            }
        check(acceptedIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey)) {
            "Group owner encryption identity changed while the invitation was accepted"
        }
        check(acceptedIdentity.signingPublicKey.contentEquals(signingPublicKey)) {
            "Group owner signing identity changed while the invitation was accepted"
        }
    }

    private suspend fun clearMembershipAttempt(invitationId: String): Result<Unit> =
        runCatching {
            val membership = membershipAttempts.findBySourceInvitationId(invitationId) ?: return@runCatching
            membershipAttempts.deleteAttempt(invitationId).getOrThrow()
            if (membership.perspective == GroupMembershipPerspective.OWNER) {
                membershipAttempts.refreshOwnedMembership(membership.groupId).getOrThrow()
            }
        }

    private fun GroupMembershipAttemptDto.toOutgoingLifecycleRecord(
        expiresAtEpochMilliseconds: Long
    ): InvitationLifecycleRecord =
        InvitationLifecycleRecord(
            invitationId = sourceInvitationId,
            payloadType = InvitationPayloadType.GROUP,
            payloadId = groupId,
            peerId = contactId,
            direction = InvitationDirection.OUTGOING,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
