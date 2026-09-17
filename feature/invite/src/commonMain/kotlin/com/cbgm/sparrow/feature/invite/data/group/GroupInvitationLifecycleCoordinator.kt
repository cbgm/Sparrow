package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupInvitationDirection
import com.cbgm.sparrow.feature.membership.data.model.GroupInvitationStatus
import com.cbgm.sparrow.feature.membership.data.resolveInvitationUpdatedAt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GroupInvitationLifecycleCoordinator(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipMessageDataSource: GroupMembershipMessageDataSource
) {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        groupInvitationDao
            .observeByDirection(direction.toStoredDirection())
            .map { invitations ->
                val now = SystemClock.nowEpochMilliseconds()
                invitations.mapNotNull { invitation ->
                    val current = expireIfNeeded(invitation, now)
                    val status = current.toVisibleStatus(direction, now) ?: return@mapNotNull null
                    current.toInvitation(direction, status)
                }
            }
            .distinctUntilChanged()

    fun observeInvitationResults(): Flow<List<InvitationResult>> =
        groupInvitationDao
            .observeAll()
            .map { invitations ->
                invitations.mapNotNull { invitation -> invitation.toInvitationResult() }
            }
            .distinctUntilChanged()

    suspend fun contains(invitationId: String): Boolean =
        groupInvitationDao.findByInvitationId(invitationId) != null

    suspend fun getPeerId(invitationId: String): Result<String> =
        runCatching {
            requireNotNull(groupInvitationDao.findByInvitationId(invitationId)) {
                "Group invitation was not found"
            }.contactId
        }

    suspend fun accept(invitationId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncoming(invitationId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be accepted from status ${invitation.status}"
            }

            val now = SystemClock.nowEpochMilliseconds()
            if (now > invitation.expiresAtEpochMilliseconds) {
                expire(invitation, now)
                error("Group invitation has expired")
            }

            prepareOwnerIdentity(invitation)

            val memberIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val joinRequest =
                membershipPacketProtocol
                    .createJoinRequest(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberIdentity = memberIdentity,
                        memberSigningKeyPair = memberSigningKeyPair
                    ).getOrThrow()

            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    newStatus =
                        GroupMembershipStateMachine
                            .transition(invitation.status, GroupMembershipEvent.ACCEPT)
                            .name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = now
                        )
                )
            check(updated == 1) { "Group invitation changed while it was accepted" }

            protocolOutbox.enqueue(invitation.contactId, joinRequest).getOrElse { error ->
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                    newStatus =
                        GroupMembershipStateMachine
                            .transition(
                                GroupInvitationStatus.JOIN_SENT.name,
                                GroupMembershipEvent.JOIN_SEND_FAILED
                            ).name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                )
                throw error
            }
        }

    suspend fun decline(invitationId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncoming(invitationId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be declined from status ${invitation.status}"
            }

            val hasHistory = chatDao.hasMessages(invitation.groupId)
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                membershipPacketProtocol
                    .createDecline(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            protocolOutbox.enqueue(invitation.contactId, packet).getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    newStatus =
                        GroupMembershipStateMachine
                            .transition(invitation.status, GroupMembershipEvent.DECLINE)
                            .name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = now
                        )
                )
            check(updated == 1) { "Group invitation changed while it was declined" }

            if (!hasHistory) {
                chatDao.hideGroupConversation(
                    membershipMessageDataSource.localConversationDeletedMarker(
                        conversationId = invitation.groupId,
                        createdAtEpochMilliseconds =
                            resolveInvitationUpdatedAt(
                                createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                                candidateAtEpochMilliseconds = now
                            )
                    )
                )
            }
        }

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        runCatching {
            val invitation =
                requireNotNull(groupInvitationDao.findByInvitationId(invitationId)) {
                    "Group invitation was not found"
                }
            check(invitation.direction == GroupInvitationDirection.OUTGOING.name) {
                "Only outgoing group invitations can be deleted"
            }
            check(invitation.status == GroupInvitationStatus.DECLINED.name) {
                "Only declined outgoing group invitations can be deleted"
            }
            check(groupInvitationDao.deleteByInvitationId(invitationId) == 1) {
                "Group invitation could not be deleted"
            }
        }

    private suspend fun requireIncoming(invitationId: String): GroupInvitationEntity {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        val invitation =
            requireNotNull(groupInvitationDao.findByInvitationId(invitationId)) {
                "Group invitation was not found"
            }
        check(invitation.direction == GroupInvitationDirection.INCOMING.name) {
            "Only incoming group invitations can be accepted or declined"
        }
        return invitation
    }

    private suspend fun prepareOwnerIdentity(invitation: GroupInvitationEntity) {
        val existing = contactDao.findPublicIdentityByContactId(invitation.contactId)
        val encryptionPublicKey =
            invitation.ownerEncryptionPublicKey
                ?: existing?.encryptionPublicKey
                ?: error("Group owner encryption identity was not stored")
        val signingPublicKey =
            invitation.ownerSigningPublicKey
                ?: existing?.signingPublicKey
                ?: error("Group owner signing identity was not stored")
        val sameIdentity =
            existing != null &&
                existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existing.signingPublicKey.contentEquals(signingPublicKey)

        if (!sameIdentity) {
            contactKeyExchangeDataSource.prepareRemoteIdentityForHandshake(
                contactId = invitation.contactId,
                remoteEncryptionPublicKey = encryptionPublicKey,
                remoteSigningPublicKey = signingPublicKey
            )
        } else if (existing.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name) {
            contactKeyExchangeDataSource.acceptRemoteIdentityForHandshake(
                contactId = invitation.contactId,
                expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                expectedRemoteSigningPublicKey = signingPublicKey
            )
        }

        val acceptedIdentity =
            requireNotNull(contactDao.findPublicIdentityByContactId(invitation.contactId)) {
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
        invitation: GroupInvitationEntity,
        now: Long
    ): GroupInvitationEntity {
        if (
            invitation.status != GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            now <= invitation.expiresAtEpochMilliseconds
        ) {
            return invitation
        }
        expire(invitation, now)
        return invitation.copy(
            status = GroupInvitationStatus.EXPIRED.name,
            updatedAtEpochMilliseconds = maxOf(invitation.createdAtEpochMilliseconds, now)
        )
    }

    private suspend fun expire(
        invitation: GroupInvitationEntity,
        now: Long
    ) {
        groupInvitationDao.updateStatus(
            invitationId = invitation.invitationId,
            expectedStatus = invitation.status,
            newStatus =
                GroupMembershipStateMachine
                    .transition(invitation.status, GroupMembershipEvent.EXPIRE)
                    .name,
            updatedAt =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                    candidateAtEpochMilliseconds = now
                )
        )
    }

    private suspend fun GroupInvitationEntity.toInvitation(
        direction: InvitationDirection,
        status: InvitationStatus
    ): Invitation {
        val contact = contactDao.findById(contactId)
        val contactName = contact?.contact?.displayName?.takeIf(String::isNotBlank)
        val contactPhone =
            contact?.phoneNumbers
                ?.firstOrNull { phoneNumber -> phoneNumber.id == contact.contact.preferredPhoneNumberId }
                ?.value
                ?: contact?.phoneNumbers?.firstOrNull()?.value
        val groupTitle = chatDao.findConversationById(groupId)?.title?.takeIf(String::isNotBlank)

        return Invitation(
            invitationId = invitationId,
            payloadType = InvitationPayloadType.GROUP,
            payloadId = groupId,
            peerId = contactId,
            peerDisplayName = if (direction == InvitationDirection.INCOMING) groupTitle ?: contactName else contactName,
            peerSecondaryText = if (direction == InvitationDirection.INCOMING) contactName ?: contactPhone else groupTitle,
            direction = direction,
            status = status,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
            hasUnreadUpdate = false
        )
    }

    private fun GroupInvitationEntity.toInvitationResult(): InvitationResult? {
        val direction =
            when (direction) {
                GroupInvitationDirection.INCOMING.name -> InvitationDirection.INCOMING
                GroupInvitationDirection.OUTGOING.name -> InvitationDirection.OUTGOING
                else -> return null
            }
        val response =
            when {
                status == GroupInvitationStatus.DECLINED.name -> InvitationResponse.DECLINED
                direction == InvitationDirection.INCOMING &&
                    status in INCOMING_ACCEPTED_STATUSES -> InvitationResponse.ACCEPTED
                direction == InvitationDirection.OUTGOING &&
                    status in OUTGOING_ACCEPTED_STATUSES -> InvitationResponse.ACCEPTED
                else -> return null
            }
        return InvitationResult(
            invitationId = invitationId,
            payloadType = InvitationPayloadType.GROUP,
            payloadId = groupId,
            peerId = contactId,
            direction = direction,
            response = response
        )
    }

    private fun GroupInvitationEntity.toVisibleStatus(
        direction: InvitationDirection,
        now: Long
    ): InvitationStatus? =
        when (status) {
            GroupInvitationStatus.AWAITING_ACCEPTANCE.name ->
                InvitationStatus.PENDING.takeIf { direction == InvitationDirection.INCOMING }

            GroupInvitationStatus.INVITE_SENT.name,
            GroupInvitationStatus.INVITE_RECEIVED.name,
            GroupInvitationStatus.WAITING_FOR_IDENTITY.name ->
                InvitationStatus.PENDING.takeIf { direction == InvitationDirection.OUTGOING }

            GroupInvitationStatus.DECLINED.name ->
                InvitationStatus.DECLINED.takeIf {
                    direction == InvitationDirection.OUTGOING &&
                        now - updatedAtEpochMilliseconds < DECLINED_INVITATION_RETENTION_MILLISECONDS
                }

            GroupInvitationStatus.EXPIRED.name,
            GroupInvitationStatus.FAILED.name -> null
            else -> null
        }

    private fun InvitationDirection.toStoredDirection(): String =
        when (this) {
            InvitationDirection.INCOMING -> GroupInvitationDirection.INCOMING.name
            InvitationDirection.OUTGOING -> GroupInvitationDirection.OUTGOING.name
        }

    private companion object {
        const val DECLINED_INVITATION_RETENTION_MILLISECONDS = 24L * 60L * 60L * 1_000L

        val INCOMING_ACCEPTED_STATUSES =
            setOf(
                GroupInvitationStatus.JOIN_SENT.name,
                GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
                GroupInvitationStatus.ACTIVE.name
            )
        val OUTGOING_ACCEPTED_STATUSES =
            setOf(
                GroupInvitationStatus.IDENTITY_READY.name,
                GroupInvitationStatus.WELCOME_SENT.name,
                GroupInvitationStatus.ACTIVE.name
            )
    }
}
