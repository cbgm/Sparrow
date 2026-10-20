package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupDescriptionPacketCodec
import com.cbgm.sparrow.feature.membership.crypto.GroupCrypto
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder

internal class GroupDescriptionPacketCodecImpl(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) : GroupDescriptionPacketCodec {
    override suspend fun create(state: GroupDescriptionState, context: GroupAdminContext): Result<GroupDescriptionUpdatedPacket> =
        safeSuspendCall {
            val unsigned =
                GroupDescriptionUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-description"),
                    groupId = state.groupId,
                    epoch = context.epoch,
                    description = state.description,
                    changedAtEpochMilliseconds = state.changedAtEpochMilliseconds,
                    adminSigningPublicKey = context.signingKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeDescriptionUpdated(unsigned),
                        signingPrivateKey = context.signingKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    override suspend fun verify(packet: GroupDescriptionUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeDescriptionUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
