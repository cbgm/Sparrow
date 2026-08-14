package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket

class GroupVerificationPayloadEncoder {
    fun encodeReceipt(packet: GroupVerificationReceiptPacket): ByteArray =
        ByteArrays.concatenate(
            RECEIPT_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            encodeString(packet.invitationId),
            encodeString(packet.receiptId),
            ByteArrays.encodeLong(packet.verifiedAtEpochMilliseconds),
            ByteArrays.withLengthPrefix(packet.participantEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.participantSigningPublicKey),
            ByteArrays.withLengthPrefix(packet.ownerEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.ownerSigningPublicKey)
        )

    fun encodeSnapshotRequest(packet: GroupVerificationSnapshotRequestPacket): ByteArray =
        ByteArrays.concatenate(
            SNAPSHOT_REQUEST_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            encodeString(packet.invitationId),
            encodeString(packet.requestId),
            ByteArrays.encodeLong(packet.requestedAtEpochMilliseconds),
            ByteArrays.withLengthPrefix(packet.requesterSigningPublicKey)
        )

    fun encodeSnapshot(packet: GroupVerificationSnapshotPacket): ByteArray =
        ByteArrays.concatenate(
            SNAPSHOT_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            encodeString(packet.snapshotId),
            ByteArrays.encodeLong(packet.generatedAtEpochMilliseconds),
            ByteArrays.withLengthPrefix(packet.ownerEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.ownerSigningPublicKey),
            encodeMembers(packet.members)
        )

    private fun encodeMembers(members: List<GroupVerificationMemberPayload>): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeInt(members.size),
            *members
                .sortedBy(GroupVerificationMemberPayload::invitationId)
                .map { member ->
                    ByteArrays.concatenate(
                        encodeString(member.invitationId),
                        encodeString(member.displayName),
                        encodeString(member.membershipStatus),
                        byteArrayOf(if (member.adminVerifiedParticipant) TRUE_VALUE else FALSE_VALUE),
                        byteArrayOf(if (member.participantVerifiedAdmin) TRUE_VALUE else FALSE_VALUE)
                    )
                }.toTypedArray()
        )

    private fun encodeString(value: String): ByteArray = ByteArrays.withLengthPrefix(value.encodeToByteArray())

    private companion object {
        val RECEIPT_DOMAIN = "sparrow.group-verification-receipt.v1".encodeToByteArray()
        val SNAPSHOT_REQUEST_DOMAIN = "sparrow.group-verification-snapshot-request.v1".encodeToByteArray()
        val SNAPSHOT_DOMAIN = "sparrow.group-verification-snapshot.v1".encodeToByteArray()
        const val FALSE_VALUE: Byte = 0
        const val TRUE_VALUE: Byte = 1
    }
}
