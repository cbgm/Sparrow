package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationSigner
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket

/** Creates, signs and verifies the packets that synchronize mutual group verification. */
interface GroupVerificationPacketCodec {
    /** A member confirms that it verified [admin]. */
    suspend fun createReceipt(
        groupId: String,
        invitationId: String,
        admin: GroupVerificationMemberKey,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationReceiptPacket>

    suspend fun createSnapshotRequest(
        groupId: String,
        invitationId: String,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationSnapshotRequestPacket>

    suspend fun createSnapshot(
        groupId: String,
        recipientContactId: String,
        rows: List<GroupVerificationRow>,
        signer: GroupVerificationSigner
    ): Result<GroupVerificationSnapshotPacket>

    suspend fun verifyReceipt(packet: GroupVerificationReceiptPacket, signingPublicKey: ByteArray): Result<Unit>

    suspend fun verifySnapshotRequest(
        packet: GroupVerificationSnapshotRequestPacket,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun verifySnapshot(packet: GroupVerificationSnapshotPacket, signingPublicKey: ByteArray): Result<Unit>

    /** The admin's snapshot rows as this device stores them. */
    fun readSnapshotRows(packet: GroupVerificationSnapshotPacket): List<GroupVerificationRow>
}
