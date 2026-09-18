package com.cbgm.sparrow.feature.membership.data.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.mapper.toMembershipResult
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineDisposition
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSigningProof
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class MembershipRepositoryImpl(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val membershipLock: GroupMembershipLock,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox
) : MembershipRepository {
    override fun observeResults(): Flow<List<MembershipResult>> =
        membershipStore
            .observeAll()
            .map { memberships -> memberships.map { membership -> membership.toMembershipResult() } }
            .distinctUntilChanged()

    override suspend fun startHandshake(
        groupId: String,
        title: String,
        peerId: String
    ): Result<StartedMembershipHandshake> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(title.isNotBlank()) { "Group title must not be blank" }
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }

            val now = SystemClock.nowEpochMilliseconds()
            resendPendingHandshake(groupId, peerId, now)?.let { return@runCatching it }

            val sourceId = IdGenerator.generate(prefix = "group-membership")
            val expiresAt = now + HANDSHAKE_VALIDITY_MILLISECONDS
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                membershipPacketProtocol
                    .createInvite(
                        invitationId = sourceId,
                        groupId = groupId,
                        title = title,
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        ownerIdentity = localIdentity,
                        ownerSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            stageOwnerHandshake(
                groupId = groupId,
                peerId = peerId,
                sourceId = sourceId,
                challenge = packet.challenge,
                createdAtEpochMilliseconds = now
            )

            protocolOutbox.enqueue(peerId, packet).getOrElse { error ->
                membershipStore.deleteBySourceInvitationId(sourceId)
                throw error
            }

            StartedMembershipHandshake(
                sourceId = sourceId,
                groupId = groupId,
                peerId = peerId,
                createdAtEpochMilliseconds = now,
                expiresAtEpochMilliseconds = expiresAt
            )
        }

    override suspend fun inspectIncomingOffer(
        peerId: String,
        packet: GroupInvitePacket
    ): Result<IncomingMembershipOffer> =
        runCatching {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            membershipPacketProtocol.verifyInvite(packet).getOrThrow()
            IncomingMembershipOffer(
                sourceId = packet.invitationId,
                groupId = packet.groupId,
                peerId = peerId,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                ownerEncryptionPublicKey = packet.ownerEncryptionPublicKey.copyOf(),
                ownerSigningPublicKey = packet.ownerSigningPublicKey.copyOf()
            )
        }

    override suspend fun receiveIncomingOffer(
        peerId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        shouldStage: Boolean
    ): Result<Unit> =
        runCatching {
            membershipPacketProtocol.verifyInvite(packet).getOrThrow()
            val existing = membershipStore.findBySourceInvitationId(packet.invitationId)
            if (existing != null) {
                validateExistingIncoming(existing, peerId, packet)
                acknowledgeIncomingOffer(peerId, packet, receivedAtEpochMilliseconds)
                return@runCatching
            }

            if (shouldStage) {
                stageMemberHandshake(
                    peerId = peerId,
                    packet = packet,
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
                )
            }

            acknowledgeIncomingOffer(peerId, packet, receivedAtEpochMilliseconds)
        }

    override suspend fun discardSupersededIncomingHandshakes(
        peerId: String,
        currentSourceId: String
    ): Result<Unit> =
        runCatching {
            membershipStore.deleteSupersededStagedMemberships(
                contactId = peerId,
                currentInvitationId = currentSourceId,
                perspective = GroupMembershipPerspective.MEMBER.name,
                stagedStatus = GroupMembershipStatus.STAGED.name
            )
        }

    override suspend fun getHandshake(sourceId: String): Result<MembershipHandshake?> =
        runCatching {
            membershipStore.findBySourceInvitationId(sourceId)?.toDomainHandshake()
        }

    override suspend fun acceptHandshake(sourceId: String): Result<Unit> =
        runCatching {
            val membership = requireMemberHandshake(sourceId)
            check(membership.status == GroupMembershipStatus.STAGED.name) {
                "Membership cannot be accepted from status ${membership.status}"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                membershipPacketProtocol
                    .createJoinRequest(
                        invitationId = membership.sourceInvitationId,
                        groupId = membership.groupId,
                        challenge = membership.challenge,
                        memberIdentity = localIdentity,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            val updatedAt = maxOf(membership.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
            transition(
                sourceId = membership.sourceInvitationId,
                event = GroupMembershipEvent.JOIN_REQUESTED,
                updatedAtEpochMilliseconds = updatedAt
            )

            protocolOutbox.enqueue(membership.contactId, packet).getOrElse { error ->
                transition(
                    sourceId = membership.sourceInvitationId,
                    event = GroupMembershipEvent.JOIN_SEND_FAILED,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
                throw error
            }
        }

    override suspend fun declineHandshake(sourceId: String): Result<Unit> =
        runCatching {
            val membership = requireMemberHandshake(sourceId)
            check(membership.status == GroupMembershipStatus.STAGED.name) {
                "Membership cannot be declined from status ${membership.status}"
            }

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
            membershipStore.deleteBySourceInvitationId(sourceId)
        }

    override suspend fun receiveOfferReceipt(
        peerId: String,
        packet: GroupInviteReceivedPacket
    ): Result<MembershipSigningProof?> =
        runCatching {
            val membership = membershipStore.findBySourceInvitationId(packet.invitationId)
                ?: return@runCatching null
            check(membership.perspective == GroupMembershipPerspective.OWNER.name) {
                "Membership receipt does not belong to an owner-side handshake"
            }
            validateMembershipPacket(
                membership = membership,
                peerId = peerId,
                groupId = packet.groupId,
                challenge = packet.challenge
            )
            membershipPacketProtocol.verifyInviteReceived(packet).getOrThrow()
            MembershipSigningProof(
                sourceId = membership.sourceInvitationId,
                groupId = membership.groupId,
                peerId = peerId,
                signingPublicKey = packet.memberSigningPublicKey.copyOf()
            )
        }

    override suspend fun receiveDecline(
        peerId: String,
        packet: GroupInviteDeclinedPacket
    ): Result<MembershipDeclineResult?> =
        runCatching {
            val membership = membershipStore.findBySourceInvitationId(packet.invitationId)
                ?: return@runCatching null
            validateMembershipPacket(
                membership = membership,
                peerId = peerId,
                groupId = packet.groupId,
                challenge = packet.challenge
            )
            membershipPacketProtocol.verifyDecline(packet).getOrThrow()

            val disposition =
                if (
                    membership.status == GroupMembershipStatus.WELCOME_SENT.name ||
                    membership.status == GroupMembershipStatus.ACTIVE.name
                ) {
                    MembershipDeclineDisposition.ACTIVE_MEMBER
                } else {
                    check(membership.status == GroupMembershipStatus.STAGED.name) {
                        "Membership cannot be declined from status ${membership.status}"
                    }
                    MembershipDeclineDisposition.PENDING_HANDSHAKE
                }

            MembershipDeclineResult(
                sourceId = membership.sourceInvitationId,
                groupId = membership.groupId,
                peerId = peerId,
                signingPublicKey = packet.memberSigningPublicKey.copyOf(),
                disposition = disposition
            )
        }

    override suspend fun receiveJoinRequest(
        peerId: String,
        packet: GroupJoinRequestPacket
    ): Result<MembershipJoinRequest?> =
        runCatching {
            val membership = membershipStore.findBySourceInvitationId(packet.invitationId)
                ?: return@runCatching null
            validateMembershipPacket(
                membership = membership,
                peerId = peerId,
                groupId = packet.groupId,
                challenge = packet.challenge
            )
            membershipPacketProtocol.verifyJoinRequest(packet).getOrThrow()

            val alreadyAccepted =
                membership.status == GroupMembershipStatus.WELCOME_SENT.name ||
                    membership.status == GroupMembershipStatus.ACTIVE.name
            check(
                alreadyAccepted ||
                    membership.status == GroupMembershipStatus.STAGED.name ||
                    membership.status == GroupMembershipStatus.IDENTITY_READY.name
            ) {
                "Unsupported membership status: ${membership.status}"
            }

            MembershipJoinRequest(
                sourceId = membership.sourceInvitationId,
                groupId = membership.groupId,
                peerId = peerId,
                memberEncryptionPublicKey = packet.memberEncryptionPublicKey.copyOf(),
                memberSigningPublicKey = packet.memberSigningPublicKey.copyOf(),
                alreadyAccepted = alreadyAccepted
            )
        }

    override suspend fun confirmJoinIdentity(
        sourceId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val membership =
                requireNotNull(membershipStore.findBySourceInvitationId(sourceId)) {
                    "Membership handshake was not found"
                }
            when (membership.status) {
                GroupMembershipStatus.STAGED.name ->
                    transition(
                        sourceId = sourceId,
                        event = GroupMembershipEvent.IDENTITY_CONFIRMED,
                        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                    )

                GroupMembershipStatus.IDENTITY_READY.name,
                GroupMembershipStatus.WELCOME_SENT.name,
                GroupMembershipStatus.ACTIVE.name -> Unit

                else -> error("Unsupported membership status: ${membership.status}")
            }
        }

    override suspend fun markRemoved(
        sourceId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            transition(
                sourceId = sourceId,
                event = GroupMembershipEvent.REMOVE,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        }

    override suspend fun clearHandshake(sourceId: String): Result<Unit> =
        runCatching {
            membershipStore.deleteBySourceInvitationId(sourceId)
        }

    private suspend fun resendPendingHandshake(
        groupId: String,
        peerId: String,
        now: Long
    ): StartedMembershipHandshake? {
        val membership =
            membershipStore.findByGroupContactAndPerspective(
                groupId = groupId,
                contactId = peerId,
                perspective = GroupMembershipPerspective.OWNER.name
            ) ?: return null
        if (membership.status != GroupMembershipStatus.STAGED.name) return null

        val packetId = INVITE_PACKET_ID_PREFIX + membership.sourceInvitationId
        val queuedPacket = protocolOutbox.findByPacketId(packetId).getOrThrow()
        val expiresAt =
            queuedPacket?.expiresAtEpochMilliseconds
                ?: (membership.createdAtEpochMilliseconds + HANDSHAKE_VALIDITY_MILLISECONDS)
        if (queuedPacket == null || now > expiresAt) {
            membershipStore.deleteBySourceInvitationId(membership.sourceInvitationId)
            return null
        }

        protocolOutbox.resend(packetId).getOrThrow()
        return StartedMembershipHandshake(
            sourceId = membership.sourceInvitationId,
            groupId = membership.groupId,
            peerId = membership.contactId,
            createdAtEpochMilliseconds = membership.createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAt
        )
    }

    private suspend fun stageOwnerHandshake(
        groupId: String,
        peerId: String,
        sourceId: String,
        challenge: ByteArray,
        createdAtEpochMilliseconds: Long
    ) {
        membershipLock.withLock {
            val existing =
                membershipStore.findByGroupContactAndPerspective(
                    groupId = groupId,
                    contactId = peerId,
                    perspective = GroupMembershipPerspective.OWNER.name
                )
            check(existing == null || existing.status.canBeReplacedForFreshHandshake()) {
                "Peer already has an active membership handshake"
            }

            membershipStore.replaceForGroupAndContact(
                GroupMembershipEntity(
                    membershipId = IdGenerator.generate(prefix = "group-membership"),
                    sourceInvitationId = sourceId,
                    groupId = groupId,
                    contactId = peerId,
                    perspective = GroupMembershipPerspective.OWNER.name,
                    status = GroupMembershipStatus.STAGED.name,
                    challenge = challenge.copyOf(),
                    createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                )
            )
        }
    }

    private suspend fun stageMemberHandshake(
        peerId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ) {
        membershipLock.withLock {
            membershipStore.replaceForGroupAndContact(
                GroupMembershipEntity(
                    membershipId = IdGenerator.generate(prefix = "group-membership"),
                    sourceInvitationId = packet.invitationId,
                    groupId = packet.groupId,
                    contactId = peerId,
                    perspective = GroupMembershipPerspective.MEMBER.name,
                    status = GroupMembershipStatus.STAGED.name,
                    challenge = packet.challenge.copyOf(),
                    ownerEncryptionPublicKey = packet.ownerEncryptionPublicKey.copyOf(),
                    ownerSigningPublicKey = packet.ownerSigningPublicKey.copyOf(),
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds =
                        maxOf(packet.createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
                )
            )
        }
    }

    private suspend fun acknowledgeIncomingOffer(
        peerId: String,
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
        protocolOutbox.enqueue(peerId, acknowledgement).getOrThrow()
    }

    private fun validateExistingIncoming(
        membership: GroupMembershipEntity,
        peerId: String,
        packet: GroupInvitePacket
    ) {
        check(membership.perspective == GroupMembershipPerspective.MEMBER.name) {
            "Incoming membership offer conflicts with an owner-side handshake"
        }
        validateMembershipPacket(
            membership = membership,
            peerId = peerId,
            groupId = packet.groupId,
            challenge = packet.challenge
        )
    }

    private fun validateMembershipPacket(
        membership: GroupMembershipEntity,
        peerId: String,
        groupId: String,
        challenge: ByteArray
    ) {
        check(membership.groupId == groupId) { "Membership packet uses the wrong group" }
        check(membership.contactId == peerId) { "Membership packet came from the wrong peer" }
        check(membership.challenge.contentEquals(challenge)) {
            "Membership packet challenge does not match"
        }
    }

    private suspend fun requireMemberHandshake(sourceId: String): GroupMembershipEntity {
        val membership =
            requireNotNull(membershipStore.findBySourceInvitationId(sourceId)) {
                "Membership handshake was not found"
            }
        check(membership.perspective == GroupMembershipPerspective.MEMBER.name) {
            "Only member-side handshakes can be accepted or declined"
        }
        return membership
    }

    private suspend fun transition(
        sourceId: String,
        event: GroupMembershipEvent,
        updatedAtEpochMilliseconds: Long
    ) {
        membershipLock.withLock {
            val current =
                requireNotNull(membershipStore.findBySourceInvitationId(sourceId)) {
                    "Membership handshake was not found"
                }
            val nextStatus = GroupMembershipStateMachine.transition(current.status, event)
            val updatedAt = maxOf(current.createdAtEpochMilliseconds, updatedAtEpochMilliseconds)
            val changed =
                membershipStore.updateStatus(
                    membershipId = current.membershipId,
                    expectedStatus = current.status,
                    newStatus = nextStatus.name,
                    updatedAt = updatedAt
                )
            check(changed == 1) { "Membership changed while it was updated" }
        }
    }

    private fun GroupMembershipEntity.toDomainHandshake(): MembershipHandshake =
        MembershipHandshake(
            sourceId = sourceInvitationId,
            groupId = groupId,
            peerId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
            ownerEncryptionPublicKey = ownerEncryptionPublicKey?.copyOf(),
            ownerSigningPublicKey = ownerSigningPublicKey?.copyOf()
        )

    private fun String.canBeReplacedForFreshHandshake(): Boolean =
        this == GroupMembershipStatus.STAGED.name ||
            this == GroupMembershipStatus.FAILED.name ||
            this == GroupMembershipStatus.REMOVED.name ||
            this == GroupMembershipStatus.GROUP_DELETED.name

    private companion object {
        const val INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val HANDSHAKE_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
