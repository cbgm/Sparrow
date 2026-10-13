package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.membership.crypto.GroupCrypto
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket

internal class GroupTitlePacketCodecImpl(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) : GroupTitlePacketCodec {
    override suspend fun create(state: GroupTitleState, context: GroupAdminContext): Result<GroupTitleUpdatedPacket> =
        safeSuspendCall {
            val unsigned =
                GroupTitleUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-title"),
                    groupId = state.groupId,
                    epoch = context.epoch,
                    title = state.title,
                    changedAtEpochMilliseconds = state.changedAtEpochMilliseconds,
                    adminSigningPublicKey = context.signingKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeTitleUpdated(unsigned),
                        signingPrivateKey = context.signingKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    override suspend fun verify(packet: GroupTitleUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeTitleUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
