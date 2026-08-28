package com.cbgm.sparrow.feature.chats.data.group.avatar

import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder

internal class GroupAvatarPacketProtocol(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) {
    suspend fun create(
        groupId: String,
        epoch: Int,
        avatar: GroupAvatarMetadata,
        adminSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupAvatarUpdatedPacket> =
        runCatching {
            val unsigned = GroupAvatarUpdatedPacket(
                packetId = IdGenerator.generate(prefix = "group-avatar"),
                groupId = groupId,
                epoch = epoch,
                avatar = avatar,
                adminSigningPublicKey = adminSigningKeyPair.publicKey.copyOf(),
                adminSignature = UNSIGNED_PACKET_MARKER
            )
            val signature = groupCrypto.sign(
                payload = payloadEncoder.encodeAvatarUpdated(unsigned),
                signingPrivateKey = adminSigningKeyPair.privateKey
            ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    suspend fun verify(packet: GroupAvatarUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeAvatarUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
