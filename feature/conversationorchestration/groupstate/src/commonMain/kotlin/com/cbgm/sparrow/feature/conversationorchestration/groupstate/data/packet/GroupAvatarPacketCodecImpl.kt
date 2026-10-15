package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.packet

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupAvatarMetadata
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupAvatarPacketCodec
import com.cbgm.sparrow.feature.membership.crypto.GroupCrypto
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder

internal class GroupAvatarPacketCodecImpl(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) : GroupAvatarPacketCodec {
    override suspend fun create(state: GroupAvatarState, context: GroupAdminContext): Result<GroupAvatarUpdatedPacket> =
        safeSuspendCall {
            val unsigned =
                GroupAvatarUpdatedPacket(
                    packetId = IdGenerator.generate(prefix = "group-avatar"),
                    groupId = state.groupId,
                    epoch = context.epoch,
                    avatar = state.toGroupAvatarMetadata(),
                    adminSigningPublicKey = context.signingKeyPair.publicKey.copyOf(),
                    adminSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeAvatarUpdated(unsigned),
                        signingPrivateKey = context.signingKeyPair.privateKey
                    ).getOrThrow()
            unsigned.copy(adminSignature = signature)
        }

    override suspend fun verify(packet: GroupAvatarUpdatedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeAvatarUpdated(packet),
            signature = packet.adminSignature,
            signingPublicKey = packet.adminSigningPublicKey
        )

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
