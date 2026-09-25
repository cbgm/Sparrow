package com.cbgm.sparrow.feature.chats.data.group.description

import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder

internal class GroupDescriptionPacketProtocol(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) {
    suspend fun create(
        groupId: String,
        epoch: Int,
        description: String?,
        changedAtEpochMilliseconds: Long,
        adminSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupDescriptionUpdatedPacket> =
        runCatching {
            val unsigned =
                GroupDescriptionUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-description"),
                    groupId = groupId,
                    epoch = epoch,
                    description = description,
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    adminSigningPublicKey = adminSigningKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeDescriptionUpdated(unsigned),
                        signingPrivateKey = adminSigningKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    suspend fun verify(packet: GroupDescriptionUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeDescriptionUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
