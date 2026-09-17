package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.InvitationDao
import com.cbgm.sparrow.data.database.entity.InvitationEntity
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipAttemptDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class GroupInvitationLifecycleCoordinator(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val invitationDao: InvitationDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val membershipMessageDataSource: GroupMembershipMessageDataSource
) {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        invitationDao
            .observeByPayloadTypeAndDirection(
                payloadType = InvitationPayloadType.GROUP.name,
                direction = direction.name
            ).map { invitations ->
                val now = SystemClock.nowEpochMilliseconds()
                invitations.mapNotNull { invitation ->
                    val current = expireIfNeeded(invitation, now)
                    val status = current.toVisibleStatus(direction, now) ?: return@mapNotNull null
                    current.toInvitation(direction, status)
                }
            }.distinctUntilChanged()

    fun observeLifecycleStatus(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        invitationDao
            .observeLatest(
                payloadType = InvitationPayloadType.GROUP.name,
                payloadId = payloadId,
                peerId = peerId,
                direction = direction.name
            ).map { invitation ->
                invitation?.status?.toLifecycleStatus()
            }.distinctUntilChanged()

    fun observeInvitationResults(): Flow<List<InvitationResult>> =
        invitationDao
            .observeByPayloadType(InvitationPayloadType.GROUP.name)
            .map { invitations ->
                invitations.mapNotNull { invitation -> invitation.toInvitationResult() }
            }.distinctUntilChanged()

    suspend fun contains(invitationId: String): Boolean =
        invitationDao.findById(invitationId)?.payloadType == InvitationPayloadType.GROUP.name

    suspend fun getPeerId(invitationId: String): Result<String> =
        runCatching { requireGroupInvitation(invitationId).peerId }

    suspend fun send(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactIds.isNotEmpty()) { "Choose at least one contact" }

            val conversation =
                chatDao.findConversationById(groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
            val title = requireNotNull(conversation.title).trim()
            check(title.isNotEmpty()) { "Group title must not be blank" }

            val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()

            contactIds.sorted().forEach { contactId ->
                check(contactDao.findById(contactId) != null) { "Contact was not found: $contactId" }
                if (resendPendingIfPossible(groupId, contactId, now)) return@forEach
                createAndQueueOutgoingInvitation(
                    groupId = groupId,
                    title = title,
                    contactId = contactId,
                    ownerIdentity = ownerIdentity,
                    ownerSigningKeyPair = ownerSigningKeyPair,
                    createdAt = now
                )
            }
            membershipAttempts.refreshOwnedMembership(groupId).getOrThrow()
        }

    suspend fun markTransportFailed(packetId: String) {
        if (!packetId.startsWith(INVITE_PACKET_ID_PREFIX)) return
        if (packetId.startsWith(INVITE_RECEIVED_PACKET_ID_PREFIX)) return
        val invitationId = packetId.removePrefix(INVITE_PACKET_ID_PREFIX)
        if (invitationId.isBlank()) return

        val invitation = invitationDao.findById(invitationId) ?: return
        if (invitation.payloadType != InvitationPayloadType.GROUP.name) return
        if (invitation.direction != InvitationDirection.OUTGOING.name) return
        if (invitation.status != INVITATION_STATUS_PENDING) return

        markOutgoingInvitationFailed(invitation, SystemClock.nowEpochMilliseconds())
        membershipAttempts.refreshOwnedMembership(invitation.payloadId)
    }

    suspend fun accept(invitationId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(invitationId)
            check(invitation.status == INVITATION_STATUS_PENDING) {
                "Group invitation cannot be accepted from status ${invitation.status}"
            }
            val membership = requireIncomingMembership(invitationId)
            check(membership.status == GroupMembershipStatus.STAGED) {
                "Group membership cannot be accepted from status ${membership.status}"
            }

            val now = SystemClock.nowEpochMilliseconds()
            if (now > invitation.expiresAtEpochMilliseconds) {
                expire(invitation, membership, now)
                error("Group invitation has expired")
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

            val updatedMembership =
                membershipAttempts
                    .markJoinRequested(
                        sourceInvitationId = invitationId,
                        updatedAtEpochMilliseconds = now
                    ).getOrThrow()

            val invitationUpdated =
                invitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = INVITATION_STATUS_PENDING,
                    newStatus = INVITATION_STATUS_ACCEPTED,
                    updatedAt = updatedMembership.updatedAtEpochMilliseconds
                )
            check(invitationUpdated == 1) { "Group invitation changed while it was accepted" }

            protocolOutbox.enqueue(membership.contactId, joinRequest).getOrElse { error ->
                membershipAttempts
                    .markJoinSendFailed(
                        sourceInvitationId = invitationId,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    ).getOrThrow()
                throw error
            }
        }

    suspend fun decline(invitationId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(invitationId)
            check(invitation.status == INVITATION_STATUS_PENDING) {
                "Group invitation cannot be declined from status ${invitation.status}"
            }
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
            val invitationUpdated =
                invitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = INVITATION_STATUS_PENDING,
                    newStatus = INVITATION_STATUS_DECLINED,
                    updatedAt = updatedAt
                )
            check(invitationUpdated == 1) { "Group invitation changed while it was declined" }
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

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        runCatching {
            val invitation = requireGroupInvitation(invitationId)
            check(invitation.direction == InvitationDirection.OUTGOING.name) {
                "Only outgoing group invitations can be deleted"
            }
            check(invitation.status == INVITATION_STATUS_DECLINED) {
                "Only declined outgoing group invitations can be deleted"
            }
            check(invitationDao.deleteById(invitationId) == 1) {
                "Group invitation could not be deleted"
            }
            membershipAttempts.deleteAttempt(invitationId).getOrThrow()
        }

    private suspend fun resendPendingIfPossible(
        groupId: String,
        contactId: String,
        now: Long
    ): Boolean {
        val invitation =
            invitationDao.findLatest(
                payloadType = InvitationPayloadType.GROUP.name,
                payloadId = groupId,
                peerId = contactId,
                direction = InvitationDirection.OUTGOING.name
            ) ?: return false
        if (invitation.status != INVITATION_STATUS_PENDING) return false
        if (now > invitation.expiresAtEpochMilliseconds) {
            markOutgoingInvitationFailed(invitation, now)
            return false
        }

        val membership = membershipAttempts.findOwnerAttempt(groupId, contactId)
        if (
            membership == null ||
            membership.sourceInvitationId != invitation.invitationId ||
            membership.status != GroupMembershipStatus.STAGED
        ) {
            markOutgoingInvitationFailed(invitation, now)
            return false
        }

        val packetId = INVITE_PACKET_ID_PREFIX + invitation.invitationId
        val queuedPacket = protocolOutbox.findByPacketId(packetId).getOrThrow()
        if (queuedPacket == null) {
            markOutgoingInvitationFailed(invitation, now)
            return false
        }
        protocolOutbox.resend(packetId).getOrThrow()
        return true
    }

    private suspend fun createAndQueueOutgoingInvitation(
        groupId: String,
        title: String,
        contactId: String,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair,
        createdAt: Long
    ) {
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

        membershipAttempts
            .stageOwnerAttempt(
                groupId = groupId,
                contactId = contactId,
                sourceInvitationId = invitationId,
                challenge = packet.challenge,
                createdAtEpochMilliseconds = createdAt
            ).getOrThrow()

        invitationDao.deleteByPayloadPeerAndDirection(
            payloadType = InvitationPayloadType.GROUP.name,
            payloadId = groupId,
            peerId = contactId,
            direction = InvitationDirection.OUTGOING.name
        )
        val invitation =
            InvitationEntity(
                invitationId = invitationId,
                payloadType = InvitationPayloadType.GROUP.name,
                payloadId = groupId,
                peerId = contactId,
                direction = InvitationDirection.OUTGOING.name,
                status = INVITATION_STATUS_PENDING,
                createdAtEpochMilliseconds = createdAt,
                expiresAtEpochMilliseconds = expiresAt,
                updatedAtEpochMilliseconds = createdAt
            )
        invitationDao.upsert(invitation)

        protocolOutbox
            .enqueue(contactId, packet)
            .onFailure { markOutgoingInvitationFailed(invitation, SystemClock.nowEpochMilliseconds()) }
    }

    private suspend fun markOutgoingInvitationFailed(
        invitation: InvitationEntity,
        updatedAt: Long
    ) {
        invitationDao.updateStatus(
            invitationId = invitation.invitationId,
            expectedStatus = INVITATION_STATUS_PENDING,
            newStatus = INVITATION_STATUS_FAILED,
            updatedAt = maxOf(invitation.createdAtEpochMilliseconds, updatedAt)
        )
        membershipAttempts.deleteAttempt(invitation.invitationId).getOrThrow()
    }

    private suspend fun requireGroupInvitation(invitationId: String): InvitationEntity {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        val invitation =
            requireNotNull(invitationDao.findById(invitationId)) {
                "Group invitation was not found"
            }
        check(invitation.payloadType == InvitationPayloadType.GROUP.name) {
            "Invitation is not a group invitation"
        }
        return invitation
    }

    private suspend fun requireIncomingInvitation(invitationId: String): InvitationEntity {
        val invitation = requireGroupInvitation(invitationId)
        check(invitation.direction == InvitationDirection.INCOMING.name) {
            "Only incoming group invitations can be accepted or declined"
        }
        return invitation
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

    private suspend fun expireIfNeeded(
        invitation: InvitationEntity,
        now: Long
    ): InvitationEntity {
        if (
            invitation.status != INVITATION_STATUS_PENDING ||
            invitation.direction != InvitationDirection.INCOMING.name ||
            now <= invitation.expiresAtEpochMilliseconds
        ) {
            return invitation
        }
        val membership = membershipAttempts.findBySourceInvitationId(invitation.invitationId)
        if (membership != null) {
            expire(invitation, membership, now)
        } else {
            invitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = INVITATION_STATUS_PENDING,
                newStatus = INVITATION_STATUS_EXPIRED,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, now)
            )
        }
        return invitation.copy(
            status = INVITATION_STATUS_EXPIRED,
            updatedAtEpochMilliseconds = maxOf(invitation.createdAtEpochMilliseconds, now)
        )
    }

    private suspend fun expire(
        invitation: InvitationEntity,
        membership: GroupMembershipAttemptDto,
        now: Long
    ) {
        val updatedAt = maxOf(membership.createdAtEpochMilliseconds, now)
        membershipAttempts.deleteAttempt(invitation.invitationId).getOrThrow()
        invitationDao.updateStatus(
            invitationId = invitation.invitationId,
            expectedStatus = INVITATION_STATUS_PENDING,
            newStatus = INVITATION_STATUS_EXPIRED,
            updatedAt = updatedAt
        )
    }

    private suspend fun InvitationEntity.toInvitation(
        direction: InvitationDirection,
        status: InvitationStatus
    ): Invitation {
        val contact = contactDao.findById(peerId)
        val contactName = contact?.contact?.displayName?.takeIf(String::isNotBlank)
        val contactPhone =
            contact?.phoneNumbers
                ?.firstOrNull { phoneNumber -> phoneNumber.id == contact.contact.preferredPhoneNumberId }
                ?.value
                ?: contact?.phoneNumbers?.firstOrNull()?.value
        val groupTitle = chatDao.findConversationById(payloadId)?.title?.takeIf(String::isNotBlank)

        return Invitation(
            invitationId = invitationId,
            payloadType = InvitationPayloadType.GROUP,
            payloadId = payloadId,
            peerId = peerId,
            peerDisplayName = if (direction == InvitationDirection.INCOMING) groupTitle ?: contactName else contactName,
            peerSecondaryText = if (direction == InvitationDirection.INCOMING) contactName ?: contactPhone else groupTitle,
            direction = direction,
            status = status,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
            hasUnreadUpdate = false
        )
    }

    private fun InvitationEntity.toInvitationResult(): InvitationResult? {
        val direction =
            InvitationDirection.entries.firstOrNull { candidate -> candidate.name == direction }
                ?: return null
        val response =
            when (status) {
                INVITATION_STATUS_ACCEPTED -> InvitationResponse.ACCEPTED
                INVITATION_STATUS_DECLINED -> InvitationResponse.DECLINED
                else -> return null
            }
        return InvitationResult(
            invitationId = invitationId,
            payloadType = InvitationPayloadType.GROUP,
            payloadId = payloadId,
            peerId = peerId,
            direction = direction,
            response = response
        )
    }

    private fun String.toLifecycleStatus(): InvitationLifecycleStatus? =
        when (this) {
            INVITATION_STATUS_PENDING -> InvitationLifecycleStatus.PENDING
            INVITATION_STATUS_ACCEPTED -> InvitationLifecycleStatus.ACCEPTED
            INVITATION_STATUS_DECLINED -> InvitationLifecycleStatus.DECLINED
            INVITATION_STATUS_EXPIRED -> InvitationLifecycleStatus.EXPIRED
            INVITATION_STATUS_FAILED -> InvitationLifecycleStatus.FAILED
            else -> null
        }

    private fun InvitationEntity.toVisibleStatus(
        direction: InvitationDirection,
        now: Long
    ): InvitationStatus? =
        when (status) {
            INVITATION_STATUS_PENDING -> InvitationStatus.PENDING
            INVITATION_STATUS_DECLINED ->
                InvitationStatus.DECLINED.takeIf {
                    direction == InvitationDirection.OUTGOING &&
                        now - updatedAtEpochMilliseconds < DECLINED_INVITATION_RETENTION_MILLISECONDS
                }
            INVITATION_STATUS_EXPIRED -> null
            INVITATION_STATUS_FAILED -> null
            INVITATION_STATUS_ACCEPTED -> null
            else -> null
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val INVITE_RECEIVED_PACKET_ID_PREFIX = "group-invite-received-"
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        const val DECLINED_INVITATION_RETENTION_MILLISECONDS = 24L * 60L * 60L * 1_000L
        const val INVITATION_STATUS_PENDING = "PENDING"
        const val INVITATION_STATUS_ACCEPTED = "ACCEPTED"
        const val INVITATION_STATUS_DECLINED = "DECLINED"
        const val INVITATION_STATUS_EXPIRED = "EXPIRED"
        const val INVITATION_STATUS_FAILED = "FAILED"
    }
}
