package com.cbgm.sparrow.feature.chats.data.group.title

import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.core.protocol.packet.GroupTitleUpdatedPacket

internal class GroupTitlePacketProtocol(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) {
    suspend fun create(
        groupId: String,
        epoch: Int,
        title: String,
        changedAtEpochMilliseconds: Long,
        adminSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupTitleUpdatedPacket> =
        runCatching {
            val unsigned =
                GroupTitleUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-title"),
                    groupId = groupId,
                    epoch = epoch,
                    title = title,
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    adminSigningPublicKey = adminSigningKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeTitleUpdated(unsigned),
                        signingPrivateKey = adminSigningKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    suspend fun verify(packet: GroupTitleUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeTitleUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
