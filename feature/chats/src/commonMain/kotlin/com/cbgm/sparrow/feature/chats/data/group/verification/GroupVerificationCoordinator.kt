package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("LongParameterList")
class GroupVerificationCoordinator internal constructor(
    private val groupVerificationDao: GroupVerificationDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: GroupVerificationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox,
    private val verificationState: GroupVerificationState,
    private val snapshotSender: GroupVerificationSnapshotSender
) {
    private val mutex = Mutex()

    suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            mutex.withLock {
                verificationState.refreshOwnedState(groupId)
            }
        }

    suspend fun synchronize(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            mutex.withLock {
                val securityState =
                    groupSecurityDao.findState(groupId)
                if (securityState == null) {
                    val ownsGroup = verificationState.ownsGroup(groupId)
                    if (ownsGroup) {
                        verificationState.refreshOwnedState(groupId)
                    }
                    return@withLock
                }

                val ownerContactId = securityState.ownerContactId
                if (securityState.localRole.isGroupAdminRole()) {
                    verificationState.refreshOwnedState(groupId)
                    snapshotSender.broadcast(groupId)
                } else {
                    ownerContactId?.let { adminContactId ->
                        enqueueSnapshotRequestLocked(
                            groupId = groupId,
                            ownerContactId = adminContactId
                        )
                    }
                }
            }
        }

    suspend fun onOwnedMembershipChanged(groupId: String): Result<Unit> =
        runCatching {
            mutex.withLock {
                val securityState = groupSecurityDao.findState(groupId)
                if (securityState != null) {
                    check(securityState.localRole.isGroupAdminRole()) {
                        "Only a group admin may publish membership verification state"
                    }
                } else {
                    val ownsGroup = verificationState.ownsGroup(groupId)
                    check(ownsGroup) {
                        "Only the group owner may update membership verification state"
                    }
                }

                verificationState.refreshOwnedState(groupId)
                if (securityState != null) {
                    snapshotSender.broadcast(groupId)
                }
            }
        }

    suspend fun verify(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            mutex.withLock {
                val securityState =
                    groupSecurityDao.findState(groupId)
                        ?: error("Group security state was not found")

                if (securityState.localRole.isGroupAdminRole()) {
                    verifyParticipantAsOwnerLocked(
                        groupId = groupId,
                        participantContactId = contactId
                    )
                } else {
                    check(contactId == securityState.ownerContactId) {
                        "A group participant may verify only the group admin"
                    }
                    verifyOwnerAsParticipantLocked(
                        groupId = groupId,
                        ownerContactId = contactId
                    )
                }
            }
        }

    suspend fun receiveReceipt(
        context: IncomingPacketContext,
        packet: GroupVerificationReceiptPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requireEncryptedTransport(context)
                requireFreshTimestamp(
                    timestamp = packet.verifiedAtEpochMilliseconds,
                    receivedAt = context.receivedAtEpochMilliseconds
                )
                check(packet.packetId == "group-verification-receipt-${packet.receiptId}") {
                    "Group verification receipt packet ID is invalid"
                }

                val securityState =
                    groupSecurityDao.findState(packet.groupId)
                        ?: error("Group security state was not found")
                check(securityState.localRole.isGroupAdminRole()) {
                    "Only a group admin may receive participant verification receipts"
                }

                val participantMemberKey =
                    verificationState.requireCurrentParticipant(
                        groupId = packet.groupId,
                        contactId = context.contactId
                    )
                check(
                    participantMemberKey.encryptionPublicKey.contentEquals(
                        packet.participantEncryptionPublicKey
                    )
                ) {
                    "Participant encryption key changed before group verification"
                }
                check(
                    participantMemberKey.signingPublicKey.contentEquals(
                        packet.participantSigningPublicKey
                    )
                ) {
                    "Participant signing key changed before group verification"
                }

                val ownerIdentity =
                    localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(ownerIdentity.encryptionPublicKey.contentEquals(packet.ownerEncryptionPublicKey)) {
                    "Group verification receipt targets a different admin encryption key"
                }
                check(ownerIdentity.signingPublicKey.contentEquals(packet.ownerSigningPublicKey)) {
                    "Group verification receipt targets a different admin signing key"
                }

                detachedSignatureCrypto
                    .verify(
                        payload = payloadEncoder.encodeReceipt(packet),
                        signingPublicKey = participantMemberKey.signingPublicKey,
                        signature = packet.signature
                    ).getOrThrow()

                verificationState.refreshOwnedState(packet.groupId)
                val verificationRow =
                    groupVerificationDao
                        .findByGroupId(packet.groupId)
                        .firstOrNull { row -> row.contactId == context.contactId }
                        ?: error("Participant verification state was not found")
                val updated =
                    groupVerificationDao.markParticipantVerifiedAdmin(
                        groupId = packet.groupId,
                        invitationId = verificationRow.invitationId,
                        updatedAt =
                            maxOf(
                                packet.verifiedAtEpochMilliseconds,
                                context.receivedAtEpochMilliseconds
                            )
                    )
                check(updated == 1) {
                    "Participant is no longer an active verification target"
                }

                snapshotSender.broadcast(packet.groupId)
            }
        }

    suspend fun receiveSnapshotRequest(
        context: IncomingPacketContext,
        packet: GroupVerificationSnapshotRequestPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requireEncryptedTransport(context)
                requireFreshTimestamp(
                    timestamp = packet.requestedAtEpochMilliseconds,
                    receivedAt = context.receivedAtEpochMilliseconds
                )
                check(packet.packetId == "group-verification-snapshot-request-${packet.requestId}") {
                    "Group verification snapshot request packet ID is invalid"
                }

                val securityState =
                    groupSecurityDao.findState(packet.groupId)
                        ?: error("Group security state was not found")
                check(securityState.localRole.isGroupAdminRole()) {
                    "Only a group admin may answer verification snapshot requests"
                }

                val participantMemberKey =
                    verificationState.requireCurrentParticipant(
                        groupId = packet.groupId,
                        contactId = context.contactId
                    )
                check(
                    participantMemberKey.signingPublicKey.contentEquals(
                        packet.requesterSigningPublicKey
                    )
                ) {
                    "Snapshot requester signing key changed"
                }
                detachedSignatureCrypto
                    .verify(
                        payload = payloadEncoder.encodeSnapshotRequest(packet),
                        signingPublicKey = participantMemberKey.signingPublicKey,
                        signature = packet.signature
                    ).getOrThrow()

                verificationState.refreshOwnedState(packet.groupId)
                snapshotSender.sendToParticipant(
                    groupId = packet.groupId,
                    recipientContactId = context.contactId
                )
            }
        }

    suspend fun receiveSnapshot(
        context: IncomingPacketContext,
        packet: GroupVerificationSnapshotPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requireEncryptedTransport(context)
                requireFreshTimestamp(
                    timestamp = packet.generatedAtEpochMilliseconds,
                    receivedAt = context.receivedAtEpochMilliseconds
                )
                check(
                    packet.packetId.startsWith(
                        "group-verification-snapshot-${packet.snapshotId}-"
                    )
                ) {
                    "Group verification snapshot packet ID is invalid"
                }

                val securityState =
                    groupSecurityDao.findState(packet.groupId)
                        ?: error("Group security state was not found")
                check(!securityState.localRole.isGroupAdminRole()) {
                    "A group admin must not consume another admin's verification snapshot"
                }
                val ownerMemberKey =
                    verificationState.requireCurrentRemoteAdmin(
                        groupId = packet.groupId,
                        contactId = context.contactId
                    )
                val ownerContactId = context.contactId
                check(ownerMemberKey.encryptionPublicKey.contentEquals(packet.ownerEncryptionPublicKey)) {
                    "Group verification snapshot admin encryption key changed"
                }
                check(ownerMemberKey.signingPublicKey.contentEquals(packet.ownerSigningPublicKey)) {
                    "Group verification snapshot admin signing key changed"
                }
                detachedSignatureCrypto
                    .verify(
                        payload = payloadEncoder.encodeSnapshot(packet),
                        signingPublicKey = ownerMemberKey.signingPublicKey,
                        signature = packet.signature
                    ).getOrThrow()

                val latestSnapshot =
                    groupVerificationDao.findLatestUpdatedAt(packet.groupId)
                if (
                    latestSnapshot != null &&
                    packet.generatedAtEpochMilliseconds < latestSnapshot
                ) {
                    return@withLock
                }

                groupVerificationDao.replaceGroup(
                    groupId = packet.groupId,
                    rows =
                        packet.members.map { member ->
                            GroupVerificationPairEntity(
                                groupId = packet.groupId,
                                invitationId = member.invitationId,
                                contactId = null,
                                displayName = member.displayName,
                                membershipStatus = member.membershipStatus,
                                participantEncryptionPublicKey = null,
                                participantSigningPublicKey = null,
                                adminVerifiedParticipant = member.adminVerifiedParticipant,
                                participantVerifiedAdmin = member.participantVerifiedAdmin,
                                updatedAtEpochMilliseconds =
                                    packet.generatedAtEpochMilliseconds
                            )
                        }
                )
            }
        }

    private suspend fun verifyParticipantAsOwnerLocked(
        groupId: String,
        participantContactId: String
    ) {
        verificationState.refreshOwnedState(groupId)

        val participantMemberKey =
            verificationState.requireCurrentParticipant(
                groupId = groupId,
                contactId = participantContactId
            )

        val row =
            groupVerificationDao
                .findByGroupId(groupId)
                .firstOrNull { candidate -> candidate.contactId == participantContactId }
                ?: error("Participant verification state was not found")
        check(
            row.participantEncryptionPublicKey?.contentEquals(
                participantMemberKey.encryptionPublicKey
            ) == true &&
                row.participantSigningPublicKey?.contentEquals(
                    participantMemberKey.signingPublicKey
                ) == true
        ) {
            "Participant identity changed before group verification"
        }

        val updated =
            groupVerificationDao.markAdminVerifiedParticipant(
                groupId = groupId,
                invitationId = row.invitationId,
                updatedAt = SystemClock.nowEpochMilliseconds()
            )
        check(updated == 1) {
            "Participant is no longer an active verification target"
        }

        snapshotSender.broadcast(groupId)
    }

    private suspend fun verifyOwnerAsParticipantLocked(
        groupId: String,
        ownerContactId: String
    ) {
        val invitation =
            groupInvitationDao
                .findByGroupId(groupId)
                .singleOrNull()
                ?: error("Local group invitation was not found")
        check(invitation.status == GroupInvitationStatus.ACTIVE.name) {
            "The group invitation must be active before verification"
        }

        val row =
            groupVerificationDao.findPair(
                groupId = groupId,
                invitationId = invitation.invitationId
            ) ?: error("Open the group once to synchronize its verification state")
        check(row.membershipStatus == GroupVerificationPairEntity.ACTIVE_STATUS) {
            "The group invitation must be active before verification"
        }

        val ownerMemberKey =
            verificationState.requireCurrentRemoteAdmin(
                groupId = groupId,
                contactId = ownerContactId
            )

        val localIdentity =
            localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair =
            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireMatchingSigningKey(localIdentity, signingKeyPair)

        val receiptId = IdGenerator.generate()
        val packetId = "group-verification-receipt-$receiptId"
        val verifiedAt = SystemClock.nowEpochMilliseconds()
        val unsignedPacket =
            GroupVerificationReceiptPacket(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                groupId = groupId,
                invitationId = invitation.invitationId,
                receiptId = receiptId,
                verifiedAtEpochMilliseconds = verifiedAt,
                participantEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                participantSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                ownerEncryptionPublicKey = ownerMemberKey.encryptionPublicKey.copyOf(),
                ownerSigningPublicKey = ownerMemberKey.signingPublicKey.copyOf(),
                signature = EMPTY_SIGNATURE
            )
        val signature =
            detachedSignatureCrypto
                .sign(
                    payload = payloadEncoder.encodeReceipt(unsignedPacket),
                    signingPrivateKey = signingKeyPair.privateKey
                ).getOrThrow()

        protocolOutbox
            .enqueue(
                contactId = ownerContactId,
                packet = unsignedPacket.copy(signature = signature.copyOf())
            ).getOrThrow()
    }

    private suspend fun enqueueSnapshotRequestLocked(
        groupId: String,
        ownerContactId: String
    ) {
        val invitation =
            groupInvitationDao
                .findByGroupId(groupId)
                .singleOrNull()
                ?: return
        if (invitation.status != GroupInvitationStatus.ACTIVE.name) {
            return
        }

        val localIdentity =
            localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair =
            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireMatchingSigningKey(localIdentity, signingKeyPair)

        val requestId = IdGenerator.generate()
        val packetId = "group-verification-snapshot-request-$requestId"
        val unsignedPacket =
            GroupVerificationSnapshotRequestPacket(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                groupId = groupId,
                invitationId = invitation.invitationId,
                requestId = requestId,
                requestedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                requesterSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                signature = EMPTY_SIGNATURE
            )
        val signature =
            detachedSignatureCrypto
                .sign(
                    payload = payloadEncoder.encodeSnapshotRequest(unsignedPacket),
                    signingPrivateKey = signingKeyPair.privateKey
                ).getOrThrow()

        protocolOutbox
            .enqueue(
                contactId = ownerContactId,
                packet = unsignedPacket.copy(signature = signature.copyOf())
            ).getOrThrow()
    }

    private fun requireEncryptedTransport(context: IncomingPacketContext) {
        check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
            "Group verification packets require encrypted transport"
        }
    }

    private fun requireFreshTimestamp(
        timestamp: Long,
        receivedAt: Long
    ) {
        require(timestamp <= receivedAt + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Group verification packet was created too far in the future"
        }
    }

    private companion object {
        val EMPTY_SIGNATURE = ByteArray(64)
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
