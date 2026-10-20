package com.cbgm.sparrow.feature.conversationorchestration.membership.data.packet

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupVerificationMemberPayload
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationSigner
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.protocol.version.ProtocolVersion

internal class GroupVerificationPacketCodecImpl(
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: GroupVerificationPayloadEncoder
) : GroupVerificationPacketCodec {
    override suspend fun createReceipt(
        groupId: String,
        invitationId: String,
        admin: GroupVerificationMemberKey,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationReceiptPacket> =
        safeSuspendCall {
            val receiptId = IdGenerator.generate()
            val unsigned =
                GroupVerificationReceiptPacket(
                    packetId = "group-verification-receipt-$receiptId",
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    invitationId = invitationId,
                    receiptId = receiptId,
                    verifiedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    participantEncryptionPublicKey = signer.identity.encryptionPublicKey.copyOf(),
                    participantSigningPublicKey = signer.identity.signingPublicKey.copyOf(),
                    ownerEncryptionPublicKey = admin.encryptionPublicKey.copyOf(),
                    ownerSigningPublicKey = admin.signingPublicKey.copyOf(),
                    signature = EMPTY_SIGNATURE
                )
            unsigned.copy(signature = sign(payloadEncoder.encodeReceipt(unsigned), signer))
        }

    override suspend fun createSnapshotRequest(
        groupId: String,
        invitationId: String,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationSnapshotRequestPacket> =
        safeSuspendCall {
            val requestId = IdGenerator.generate()
            val unsigned =
                GroupVerificationSnapshotRequestPacket(
                    packetId = "group-verification-snapshot-request-$requestId",
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    invitationId = invitationId,
                    requestId = requestId,
                    requestedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    requesterSigningPublicKey = signer.identity.signingPublicKey.copyOf(),
                    signature = EMPTY_SIGNATURE
                )
            unsigned.copy(signature = sign(payloadEncoder.encodeSnapshotRequest(unsigned), signer))
        }

    override suspend fun createSnapshot(
        groupId: String,
        recipientContactId: String,
        rows: List<GroupVerificationRow>,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationSnapshotPacket> =
        safeSuspendCall {
            val snapshotId = IdGenerator.generate()
            val unsigned =
                GroupVerificationSnapshotPacket(
                    packetId = "group-verification-snapshot-$snapshotId-$recipientContactId",
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    snapshotId = snapshotId,
                    generatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    ownerEncryptionPublicKey = signer.identity.encryptionPublicKey.copyOf(),
                    ownerSigningPublicKey = signer.identity.signingPublicKey.copyOf(),
                    members = rows.map { row -> row.toGroupVerificationMemberPayload() },
                    signature = EMPTY_SIGNATURE
                )
            unsigned.copy(signature = sign(payloadEncoder.encodeSnapshot(unsigned), signer))
        }

    override suspend fun verifyReceipt(
        packet: GroupVerificationReceiptPacket,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        detachedSignatureCrypto.verify(payloadEncoder.encodeReceipt(packet), signingPublicKey, packet.signature)

    override suspend fun verifySnapshotRequest(
        packet: GroupVerificationSnapshotRequestPacket,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        detachedSignatureCrypto.verify(payloadEncoder.encodeSnapshotRequest(packet), signingPublicKey, packet.signature)

    override suspend fun verifySnapshot(
        packet: GroupVerificationSnapshotPacket,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        detachedSignatureCrypto.verify(payloadEncoder.encodeSnapshot(packet), signingPublicKey, packet.signature)

    override fun readSnapshotRows(packet: GroupVerificationSnapshotPacket): List<GroupVerificationRow> =
        packet.members.map { member ->
            member.toGroupVerificationRow(packet.groupId, packet.generatedAtEpochMilliseconds)
        }

    private suspend fun sign(payload: ByteArray, signer: GroupVerificationSigner): ByteArray =
        detachedSignatureCrypto.sign(payload, signer.signingKeyPair.privateKey).getOrThrow().copyOf()

    private companion object {
        val EMPTY_SIGNATURE = ByteArray(64)
    }
}
