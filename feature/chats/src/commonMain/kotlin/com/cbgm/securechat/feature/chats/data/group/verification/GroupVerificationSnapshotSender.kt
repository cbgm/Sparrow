package com.cbgm.securechat.feature.chats.data.group.verification

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupVerificationMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity

@Suppress("LongParameterList")
internal class GroupVerificationSnapshotSender(
    private val groupVerificationDao: GroupVerificationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: GroupVerificationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox,
    private val verificationState: GroupVerificationState
) {
    suspend fun broadcast(groupId: String) {
        val recipientContactIds =
            groupVerificationDao
                .findByGroupId(groupId)
                .filter { row ->
                    row.membershipStatus == GroupVerificationPairEntity.ACTIVE_STATUS
                }.mapNotNull { row -> row.contactId }

        for (contactId in recipientContactIds) {
            if (isCurrentParticipant(groupId, contactId)) {
                sendToParticipant(groupId, contactId)
            }
        }
    }

    private suspend fun isCurrentParticipant(
        groupId: String,
        contactId: String
    ): Boolean =
        runCatching {
            verificationState.requireCurrentParticipant(groupId, contactId)
        }.isSuccess

    suspend fun sendToParticipant(
        groupId: String,
        recipientContactId: String
    ) {
        val localIdentity =
            localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair =
            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireMatchingSigningKey(localIdentity, signingKeyPair)

        val members =
            groupVerificationDao
                .findByGroupId(groupId)
                .map { row -> row.toPayload() }
        val snapshotId = IdGenerator.generate()
        val unsignedPacket =
            GroupVerificationSnapshotPacket(
                packetId = "group-verification-snapshot-$snapshotId-$recipientContactId",
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

    private fun GroupVerificationPairEntity.toPayload(): GroupVerificationMemberPayload =
        GroupVerificationMemberPayload(
            invitationId = invitationId,
            displayName = displayName,
            membershipStatus = membershipStatus,
            adminVerifiedParticipant = adminVerifiedParticipant,
            participantVerifiedAdmin = participantVerifiedAdmin
        )

    private companion object {
        val EMPTY_SIGNATURE = ByteArray(64)
    }
}
