package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.feature.membership.crypto.GroupCrypto
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder

internal class GroupPinPacketCodecImpl(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinPacketCodec {
    override suspend fun create(pin: GroupPinState, context: GroupAdminContext): Result<GroupPinUpdatedPacket> =
        safeSuspendCall {
            val unsigned =
                GroupPinUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-pin"),
                    groupId = pin.groupId,
                    epoch = context.epoch,
                    messageId = pin.messageId,
                    messageSentAtEpochMilliseconds = pin.messageSentAtEpochMilliseconds ?: 0L,
                    messageSenderSigningPublicKey = pin.messageSenderSigningPublicKey?.copyOf() ?: byteArrayOf(),
                    messageContent = pin.toGroupMessageContent(),
                    changedAtEpochMilliseconds = pin.changedAtEpochMilliseconds,
                    adminSigningPublicKey = context.signingKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = encodedPayload(unsigned),
                        signingPrivateKey = context.signingKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    override suspend fun verify(packet: GroupPinUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = encodedPayload(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    override fun toGroupPinState(
        packet: GroupPinUpdatedPacket,
        senderContactId: String?,
        isMine: Boolean
    ): GroupPinState =
        packet.toGroupPinState(senderContactId = senderContactId, isMine = isMine)

    private fun encodedPayload(packet: GroupPinUpdatedPacket): ByteArray =
        payloadEncoder.encodePinUpdated(
            packet = packet,
            encodedMessageContent = packet.messageContent?.let(groupMessageContentCodec::encode)
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
