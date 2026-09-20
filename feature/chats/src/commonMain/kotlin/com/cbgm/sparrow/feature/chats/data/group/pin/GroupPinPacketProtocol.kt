package com.cbgm.sparrow.feature.chats.data.group.pin

import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder

internal class GroupPinPacketProtocol(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupMessageContentCodec: GroupMessageContentCodec
) {
    suspend fun create(
        groupId: String,
        epoch: Int,
        messageId: String?,
        messageSentAtEpochMilliseconds: Long,
        messageSenderSigningPublicKey: ByteArray,
        messageContent: GroupMessageContent?,
        changedAtEpochMilliseconds: Long,
        adminSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupPinUpdatedPacket> =
        runCatching {
            val unsigned =
                GroupPinUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-pin"),
                    groupId = groupId,
                    epoch = epoch,
                    messageId = messageId,
                    messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
                    messageSenderSigningPublicKey = messageSenderSigningPublicKey.copyOf(),
                    messageContent = messageContent,
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    adminSigningPublicKey = adminSigningKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = encodedPayload(unsigned),
                        signingPrivateKey = adminSigningKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    suspend fun verify(packet: GroupPinUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = encodedPayload(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private fun encodedPayload(packet: GroupPinUpdatedPacket): ByteArray =
        payloadEncoder.encodePinUpdated(
            packet = packet,
            encodedMessageContent = packet.messageContent?.let(groupMessageContentCodec::encode)
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
