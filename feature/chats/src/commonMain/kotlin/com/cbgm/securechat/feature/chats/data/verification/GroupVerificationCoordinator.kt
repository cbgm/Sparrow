package com.cbgm.securechat.feature.chats.data.verification

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupVerificationMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.security.isGroupAdminRole
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationActionRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupVerificationCoordinator(
    private val chatDao: ChatDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val getContact: GetContact,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: GroupVerificationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox
) : GroupVerificationActionRepository {
    private val mutex = Mutex()

    suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            mutex.withLock {
                refreshOwnedStateLocked(groupId)
            }
        }

    override suspend fun synchronize(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            mutex.withLock {
                val securityState =
                    groupSecurityDao.findState(groupId)
                if (securityState == null) {
                    val ownsGroup =
                        groupVerificationDao
                            .findByGroupId(groupId)
                            .any { row -> row.contactId != null } ||
                            groupInvitationDao
                                .findByGroupId(groupId)
                                .any { invitation ->
                                    invitation.direction == GroupInvitationDirection.OUTGOING.name
                                }
                    if (ownsGroup) {
                        refreshOwnedStateLocked(groupId)
                    }
                    return@withLock
                }

                val ownerContactId = securityState.ownerContactId
                if (securityState.localRole.isGroupAdminRole()) {
                    refreshOwnedStateLocked(groupId)
                    broadcastSnapshotLocked(groupId)
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
                    val ownsGroup =
                        groupVerificationDao
                            .findByGroupId(groupId)
                            .any { row -> row.contactId != null } ||
                            groupInvitationDao
                                .findByGroupId(groupId)
                                .any { invitation ->
                                    invitation.direction == GroupInvitationDirection.OUTGOING.name
                                }
                    check(ownsGroup) {
                        "Only the group owner may update membership verification state"
                    }
                }

                refreshOwnedStateLocked(groupId)
                if (securityState != null) {
                    broadcastSnapshotLocked(groupId)
                }
            }
        }

    override suspend fun verify(
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

                requireCurrentParticipant(
                    groupId = packet.groupId,
                    contactId = context.contactId
                )

                val participant = loadContact(context.contactId)
                val participantIdentity =
                    participant.secureChatIdentity
                        ?: error("Participant has no SecureChat identity")
                check(participantIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                    "Group verification requires mutual contact keys"
                }
                check(
                    participantIdentity.encryptionPublicKey.contentEquals(
                        packet.participantEncryptionPublicKey
                    )
                ) {
                    "Participant encryption key changed before group verification"
                }
                check(
                    participantIdentity.signingPublicKey.contentEquals(
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
                        signingPublicKey = participantIdentity.signingPublicKey,
                        signature = packet.signature
                    ).getOrThrow()

                refreshOwnedStateLocked(packet.groupId)
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

                broadcastSnapshotLocked(packet.groupId)
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

                requireCurrentParticipant(
                    groupId = packet.groupId,
                    contactId = context.contactId
                )

                val participantIdentity =
                    loadContact(context.contactId).secureChatIdentity
                        ?: error("Participant has no SecureChat identity")
                check(
                    participantIdentity.signingPublicKey.contentEquals(
                        packet.requesterSigningPublicKey
                    )
                ) {
                    "Snapshot requester signing key changed"
                }
                detachedSignatureCrypto
                    .verify(
                        payload = payloadEncoder.encodeSnapshotRequest(packet),
                        signingPublicKey = participantIdentity.signingPublicKey,
                        signature = packet.signature
                    ).getOrThrow()

                refreshOwnedStateLocked(packet.groupId)
                sendSnapshotLocked(
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
                requireCurrentRemoteAdmin(
                    groupId = packet.groupId,
                    contactId = context.contactId
                )
                val ownerContactId = context.contactId
                val ownerIdentity =
                    loadContact(ownerContactId).secureChatIdentity
                        ?: error("Group admin has no SecureChat identity")
                check(ownerIdentity.encryptionPublicKey.contentEquals(packet.ownerEncryptionPublicKey)) {
                    "Group verification snapshot admin encryption key changed"
                }
                check(ownerIdentity.signingPublicKey.contentEquals(packet.ownerSigningPublicKey)) {
                    "Group verification snapshot admin signing key changed"
                }
                detachedSignatureCrypto
                    .verify(
                        payload = payloadEncoder.encodeSnapshot(packet),
                        signingPublicKey = ownerIdentity.signingPublicKey,
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
        refreshOwnedStateLocked(groupId)

        requireCurrentParticipant(
            groupId = groupId,
            contactId = participantContactId
        )

        val participantIdentity =
            loadContact(participantContactId).secureChatIdentity
                ?: error("Participant has no SecureChat identity")
        check(participantIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
            "The participant identity is not ready for verification"
        }

        val row =
            groupVerificationDao
                .findByGroupId(groupId)
                .firstOrNull { candidate -> candidate.contactId == participantContactId }
                ?: error("Participant verification state was not found")
        check(
            row.participantEncryptionPublicKey?.contentEquals(
                participantIdentity.encryptionPublicKey
            ) == true &&
                row.participantSigningPublicKey?.contentEquals(
                    participantIdentity.signingPublicKey
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

        broadcastSnapshotLocked(groupId)
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

        val ownerIdentity =
            loadContact(ownerContactId).secureChatIdentity
                ?: error("Group admin has no SecureChat identity")
        check(ownerIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
            "The group admin identity is not ready for verification"
        }

        val localIdentity =
            localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair =
            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)

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
                ownerEncryptionPublicKey = ownerIdentity.encryptionPublicKey.copyOf(),
                ownerSigningPublicKey = ownerIdentity.signingPublicKey.copyOf(),
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
        requireLocalKeysMatch(localIdentity, signingKeyPair)

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

    private suspend fun requireCurrentParticipant(
        groupId: String,
        contactId: String
    ) {
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        check(
            chatDao.findConversationParticipants(groupId)
                .any { participant -> participant.contactId == contactId }
        ) { "Contact is not an active group participant" }
        val identity =
            loadContact(contactId).secureChatIdentity
                ?: error("Group participant has no SecureChat identity")
        val memberKey =
            groupSecurityDao.findMemberKey(
                groupId = groupId,
                epoch = state.currentEpoch,
                contactId = contactId
            ) ?: error("Group participant is not part of the current epoch")
        check(memberKey.signingPublicKey.contentEquals(identity.signingPublicKey)) {
            "Group participant identity no longer matches the current epoch"
        }
    }

    private suspend fun requireCurrentRemoteAdmin(
        groupId: String,
        contactId: String
    ) {
        requireCurrentParticipant(groupId, contactId)
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        val memberKey =
            groupSecurityDao.findMemberKey(
                groupId = groupId,
                epoch = state.currentEpoch,
                contactId = contactId
            ) ?: error("Group admin is not part of the current epoch")
        check(memberKey.role.isGroupAdminRole()) { "Group participant is not an admin" }
    }

    private suspend fun refreshOwnedStateLocked(groupId: String) {
        val existingRows = groupVerificationDao.findByGroupId(groupId)
        val existingByContactId = existingRows.mapNotNull { row -> row.contactId?.let { it to row } }.toMap()
        val invitations = groupInvitationDao.findByGroupId(groupId)
        val invitationByContactId = invitations.associateBy { invitation -> invitation.contactId }
        val participants = chatDao.findConversationParticipants(groupId)
        val now = SystemClock.nowEpochMilliseconds()

        val activeRows =
            participants.map { participant ->
                val contact = loadContact(participant.contactId)
                val identity = contact.secureChatIdentity
                val invitation = invitationByContactId[participant.contactId]
                val previous = existingByContactId[participant.contactId]
                val sameIdentity =
                    previous?.participantEncryptionPublicKey != null &&
                        previous.participantSigningPublicKey != null &&
                        identity != null &&
                        previous.participantEncryptionPublicKey.contentEquals(identity.encryptionPublicKey) &&
                        previous.participantSigningPublicKey.contentEquals(identity.signingPublicKey)
                GroupVerificationPairEntity(
                    groupId = groupId,
                    invitationId = invitation?.invitationId ?: previous?.invitationId ?: "member-${participant.contactId}",
                    contactId = participant.contactId,
                    displayName = contact.displayName?.trim()?.takeIf(String::isNotBlank) ?: "Unknown member",
                    membershipStatus = GroupVerificationPairEntity.ACTIVE_STATUS,
                    participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                    participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                    adminVerifiedParticipant = sameIdentity && previous.adminVerifiedParticipant,
                    participantVerifiedAdmin = sameIdentity && previous.participantVerifiedAdmin,
                    updatedAtEpochMilliseconds = maxOf(invitation?.updatedAtEpochMilliseconds ?: 0L, now)
                )
            }

        val activeContactIds = participants.mapTo(mutableSetOf()) { participant -> participant.contactId }
        val pendingRows =
            invitations
                .filterNot { invitation ->
                    invitation.status.isTerminalStatus() || invitation.contactId in activeContactIds
                }.map { invitation ->
                    val contact = loadContact(invitation.contactId)
                    val identity = contact.secureChatIdentity
                    val previous = existingByContactId[invitation.contactId]
                    GroupVerificationPairEntity(
                        groupId = groupId,
                        invitationId = invitation.invitationId,
                        contactId = invitation.contactId,
                        displayName = contact.displayName?.trim()?.takeIf(String::isNotBlank) ?: "Unknown member",
                        membershipStatus = GroupVerificationPairEntity.PENDING_STATUS,
                        participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                        participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                        adminVerifiedParticipant = false,
                        participantVerifiedAdmin = false,
                        updatedAtEpochMilliseconds = maxOf(invitation.updatedAtEpochMilliseconds, previous?.updatedAtEpochMilliseconds ?: 0L)
                    )
                }

        groupVerificationDao.replaceGroup(groupId = groupId, rows = activeRows + pendingRows)
    }

    private suspend fun broadcastSnapshotLocked(groupId: String) {
        val activeRows =
            groupVerificationDao
                .findByGroupId(groupId)
                .filter { row ->
                    row.membershipStatus == GroupVerificationPairEntity.ACTIVE_STATUS &&
                        row.contactId != null
                }

        activeRows.forEach { row ->
            val contactId = checkNotNull(row.contactId)
            if (runCatching { requireCurrentParticipant(groupId, contactId) }.isSuccess) {
                sendSnapshotLocked(
                    groupId = groupId,
                    recipientContactId = contactId
                )
            }
        }
    }

    private suspend fun sendSnapshotLocked(
        groupId: String,
        recipientContactId: String
    ) {
        val localIdentity =
            localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair =
            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)

        val members =
            groupVerificationDao
                .findByGroupId(groupId)
                .map { row ->
                    GroupVerificationMemberPayload(
                        invitationId = row.invitationId,
                        displayName = row.displayName,
                        membershipStatus = row.membershipStatus,
                        adminVerifiedParticipant = row.adminVerifiedParticipant,
                        participantVerifiedAdmin = row.participantVerifiedAdmin
                    )
                }
        val snapshotId = IdGenerator.generate()
        val packetId =
            "group-verification-snapshot-$snapshotId-$recipientContactId"
        val unsignedPacket =
            GroupVerificationSnapshotPacket(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                groupId = groupId,
                snapshotId = snapshotId,
                generatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                ownerEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                ownerSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                members = members,
                signature = EMPTY_SIGNATURE
            )
        val signature =
            detachedSignatureCrypto
                .sign(
                    payload = payloadEncoder.encodeSnapshot(unsignedPacket),
                    signingPrivateKey = signingKeyPair.privateKey
                ).getOrThrow()

        protocolOutbox
            .enqueue(
                contactId = recipientContactId,
                packet = unsignedPacket.copy(signature = signature.copyOf())
            ).getOrThrow()
    }

    private suspend fun loadContact(contactId: String): Contact =
        getContact(contactId).getOrThrow()
            ?: error("Contact not found: $contactId")

    private fun requireLocalKeysMatch(
        identity: LocalPublicIdentity,
        signingKeyPair: LocalSigningKeyPair
    ) {
        check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local signing key pair does not match the public identity"
        }
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

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.EXPIRED.name ||
            this == GroupInvitationStatus.FAILED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.GROUP_DELETED.name

    private companion object {
        val EMPTY_SIGNATURE = ByteArray(64)
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
